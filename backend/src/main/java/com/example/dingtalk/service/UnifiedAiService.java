package com.example.dingtalk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.common.BizException;
import com.example.dingtalk.entity.AiFileChunk;
import com.example.dingtalk.entity.SysSchedule;
import com.example.dingtalk.mapper.AiFileChunkMapper;
import com.example.dingtalk.mapper.ScheduleMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 统一 AI 服务：RAG 问答 + Function Calling 日程创建
 *
 * 一个入口融合两种能力：
 * 1. RAG 问答 —— 先检索文档片段，再让 AI 结合上下文回答
 * 2. 日程创建 —— 通过 Function Calling 自动识别意图并调用 createSchedule 工具
 *    （可选的 RAG 上下文会一并提供给 AI，让其在回答时也参考知识库）
 */
@Service
public class UnifiedAiService {

    private static final Logger log = LoggerFactory.getLogger(UnifiedAiService.class);
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${app.ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${spring.ai.openai.api-key:}")
    private String openAiApiKey;

    @Value("${app.ai.rag.top-k:4}")
    private int topK;

    @Value("${app.ai.rag.min-score:0.18}")
    private double minScore;

    @Value("${app.ai.rag.max-context-chars:6000}")
    private int maxContextChars;

    private final AiFileChunkMapper aiFileChunkMapper;
    private final AiDocumentIngestService aiDocumentIngestService;
    private final ScheduleMapper scheduleMapper;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final ChatClient chatClient;

    public UnifiedAiService(AiFileChunkMapper aiFileChunkMapper,
                            AiDocumentIngestService aiDocumentIngestService,
                            ScheduleMapper scheduleMapper,
                            ObjectProvider<EmbeddingModel> embeddingModelProvider,
                            ChatClient chatClient) {
        this.aiFileChunkMapper = aiFileChunkMapper;
        this.aiDocumentIngestService = aiDocumentIngestService;
        this.scheduleMapper = scheduleMapper;
        this.embeddingModelProvider = embeddingModelProvider;
        this.chatClient = chatClient;
    }

    /**
     * 统一 AI 流式回答入口
     * 自动识别用户意图：RAG 问答 or Function Calling 日程创建
     *
     * @param userId   当前用户 ID
     * @param question 用户输入
     * @return AI 流式回复
     */
    public Flux<String> answer(Long userId, String question,String fileName) {
        String normalizedQuestion = question == null ? "" : question.trim();
        if (normalizedQuestion.isEmpty()) {
            return Flux.just("请先输入问题。");
        }
        if (!isAiReady()) {
            return Flux.just("AI 未配置，请设置 SPRING_AI_OPENAI_API_KEY 后重试。");
        }

        aiDocumentIngestService.reindexMissingFiles(userId, 20);

        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (embeddingModel == null) {
            return Flux.just("AI 客户端未初始化，请检查配置。");
        }

        try {
            // 1. RAG 检索：将问题向量化，搜索最相关的文档片段
            float[] queryEmbedding = embeddingModel.embed(normalizedQuestion);
            LambdaQueryWrapper<AiFileChunk> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AiFileChunk::getUserId, userId);
            if(fileName!=null &&!fileName.isBlank()){
                wrapper.like(AiFileChunk::getFileName,fileName);
            }
            List<AiFileChunk> targetChunks = aiFileChunkMapper.selectList(wrapper);
            List<ScoredChunk> relevantChunks = rankChunks(targetChunks, queryEmbedding);

            // 2. 构建系统提示（注入当前时间，让 AI 能推算相对时间）
            String systemPrompt = buildSystemPrompt(relevantChunks, normalizedQuestion);

            // 3. 构建日程创建工具（传入当前 userId 避免 SecurityContext 丢失）
            ToolCallbackProvider toolProvider = MethodToolCallbackProvider.builder()
                    .toolObjects(new ScheduleTool(scheduleMapper, userId))
                    .build();
            ToolCallback[] scheduleTools = toolProvider.getToolCallbacks();

            // 4. 调用 AI（同时携带文档上下文 + Function Calling 工具）
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(normalizedQuestion)
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, userId))
                    .toolCallbacks(java.util.Arrays.asList(scheduleTools))
                    .stream()
                    .content()
                    .doOnError(e -> log.error("AI 回答异常 userId={}", userId, e));

        } catch (Exception e) {
            log.error("AI answer failed for user {}", userId, e);
            return Flux.just("AI 服务调用失败，请检查模型地址、密钥和网络后重试。\\[DONE]");
        }
    }

    /**
     * 构建系统提示词：包含文档上下文 + 日程工具的说明
     */
    private String buildSystemPrompt(List<ScoredChunk> relevantChunks, String question) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                你是企业协作平台的AI助手。你可以：
                
                1. 回答用户问题 —— 基于下方提供的文档片段来回答，如果文档中没有相关内容，直接说明没有找到。回答简洁口语化，不要使用 Markdown 格式。
                
                2. 创建日程提醒 —— 当用户说"提醒我"、"创建日程"、"安排会议"、"约会"等关键词时，请调用 createSchedule 工具来创建日程。如果用户没有提供具体时间，询问用户具体时间。
                
                注意：优先判断是否是日程相关请求。如果是，调用 createSchedule 工具；否则基于文档片段回答。
                当前系统时间：""").append(LocalDateTime.now().format(DATE_TIME_FMT)).append("""
                请以此时间为基准推算用户提到的相对时间（如"明天下午5点"）。时间格式为：yyyy-MM-dd HH:mm:ss
                """);

        if (!relevantChunks.isEmpty()) {
            sb.append("\n\n相关文档片段：\n");
            int index = 1;
            int currentLength = 0;
            for (ScoredChunk chunk : relevantChunks) {
                String block = chunk.chunk().getFileName() + "[" + index++ + "]:\n"
                        + chunk.chunk().getChunkText() + "\n";
                if (currentLength + block.length() > maxContextChars) {
                    break;
                }
                sb.append(block).append('\n');
                currentLength += block.length();
            }
        }

        return sb.toString();
    }

    // ================== RAG 工具方法 ==================

    private boolean isAiReady() {
        return aiEnabled
                && openAiApiKey != null
                && !openAiApiKey.isBlank()
                && !"disabled".equalsIgnoreCase(openAiApiKey.trim());
    }

    private List<ScoredChunk> rankChunks(List<AiFileChunk> chunks, float[] queryEmbedding) {
        List<ScoredChunk> scored = new ArrayList<>();
        for (AiFileChunk chunk : chunks) {
            float[] vector = parseEmbedding(chunk.getEmbeddingJson());
            if (vector.length == 0) {
                continue;
            }
            double score = cosineSimilarity(queryEmbedding, vector);
            if (score >= minScore) {
                scored.add(new ScoredChunk(chunk, score));
            }
        }
        scored.sort(Comparator.comparingDouble(ScoredChunk::score).reversed());
        if (scored.size() > topK) {
            return new ArrayList<>(scored.subList(0, topK));
        }
        return scored;
    }

    private float[] parseEmbedding(String json) {
        if (json == null || json.isBlank()) {
            return new float[0];
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
        } catch (IOException e) {
            log.warn("Parse embedding failed", e);
            return new float[0];
        }
    }

    private double cosineSimilarity(float[] left, float[] right) {
        int len = Math.min(left.length, right.length);
        if (len == 0) {
            return -1D;
        }
        double dot = 0D, leftNorm = 0D, rightNorm = 0D;
        for (int i = 0; i < len; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0D || rightNorm == 0D) {
            return -1D;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private record ScoredChunk(AiFileChunk chunk, double score) {}

    // ================== 日程创建工具 ==================

    public static class ScheduleTool {

        private final ScheduleMapper scheduleMapper;
        private final Long userId;

        public ScheduleTool(ScheduleMapper scheduleMapper, Long userId) {
            this.scheduleMapper = scheduleMapper;
            this.userId = userId;
        }

        @org.springframework.ai.tool.annotation.Tool(
            name = "createSchedule",
            description = "创建一个新的日程提醒。当用户说'提醒我'、'创建日程'、'安排会议'等关键词时使用。" +
                         "需要从用户输入中提取：标题、开始时间、结束时间、内容等信息。" +
                         "如果用户没说具体时间，询问用户具体时间。" +
                         "时间格式为：yyyy-MM-dd HH:mm:ss"
        )
        public String createSchedule(
                @org.springframework.ai.tool.annotation.ToolParam(
                    description = "日程标题，简短描述日程内容，如'开会'"
                ) String title,

                @org.springframework.ai.tool.annotation.ToolParam(
                    description = "日程开始时间，格式：yyyy-MM-dd HH:mm:ss"
                ) String startTime,

                @org.springframework.ai.tool.annotation.ToolParam(
                    description = "日程结束时间，格式：yyyy-MM-dd HH:mm:ss，如果不提供则默认比开始时间多1小时"
                ) String endTime,

                @org.springframework.ai.tool.annotation.ToolParam(
                    description = "日程详细内容描述"
                ) String content,

                @org.springframework.ai.tool.annotation.ToolParam(
                    description = "是否为全天日程，0表示非全天，1表示全天"
                ) Integer allDay,

                @org.springframework.ai.tool.annotation.ToolParam(
                    description = "日程颜色，如#4A90E2，可选"
                ) String color
        ) {
            try {
                if (title == null || title.isEmpty()) {
                    title = "新日程";
                }
                if (startTime == null || startTime.isEmpty()) {
                    return "❌ 创建日程失败：请提供开始时间，格式为 yyyy-MM-dd HH:mm:ss";
                }

                LocalDateTime start = parseDateTime(startTime);
                LocalDateTime end = endTime != null && !endTime.isEmpty()
                        ? parseDateTime(endTime)
                        : start.plusHours(1);

                SysSchedule schedule = new SysSchedule();
                schedule.setTitle(title);
                schedule.setContent(content != null ? content : "");
                schedule.setUserId(userId);
                schedule.setStartTime(start);
                schedule.setEndTime(end);
                schedule.setAllDay(allDay != null ? allDay : 0);
                schedule.setColor(color != null ? color : "#4A90E2");
                schedule.setCreateTime(LocalDateTime.now());

                scheduleMapper.insert(schedule);

                return "✅ 已成功创建日程：「" + title + "」\n" +
                       "时间：" + start.format(DATE_TIME_FMT) + " - " + end.format(DATE_TIME_FMT);

            } catch (Exception e) {
                log.error("创建日程失败", e);
                return "❌ 创建日程失败：" + e.getMessage();
            }
        }

        private LocalDateTime parseDateTime(String timeStr) {
            if (timeStr == null || timeStr.isEmpty()) {
                throw new BizException("时间不能为空");
            }
            try {
                return LocalDateTime.parse(timeStr, DATE_TIME_FMT);
            } catch (DateTimeParseException e) {
                try {
                    return LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                } catch (DateTimeParseException e2) {
                    throw new BizException("无法解析时间格式：" + timeStr + "，请使用 yyyy-MM-dd HH:mm:ss 格式");
                }
            }
        }
    }
}
