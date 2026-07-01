package com.example.dingtalk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.common.BizException;
import com.example.dingtalk.entity.AiFileChunk;
import com.example.dingtalk.entity.SysSchedule;
import com.example.dingtalk.mapper.AiFileChunkMapper;
import com.example.dingtalk.mapper.ScheduleMapper;
import com.example.dingtalk.dto.SendDingDTO;
import com.example.dingtalk.dto.SendMailDTO;
import com.example.dingtalk.dto.SendMessageDTO;
import com.example.dingtalk.entity.ChatSession;
import com.example.dingtalk.entity.ChatSessionMember;
import com.example.dingtalk.entity.SysNotice;
import com.example.dingtalk.entity.SysTodo;
import com.example.dingtalk.entity.SysUser;
import com.example.dingtalk.mapper.ChatSessionMapper;
import com.example.dingtalk.mapper.ChatSessionMemberMapper;
import com.example.dingtalk.mapper.UserMapper;
import com.example.dingtalk.service.*;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.springframework.context.annotation.Lazy;
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
    private final ChatService chatService;
    private final UserMapper userMapper;
    private final ChatSessionMapper sessionMapper;
    private final ChatSessionMemberMapper sessionMemberMapper;
    private final MailboxService mailboxService;
    private final NoticeService noticeService;
    private final PermissionService permissionService;
    private final TodoService todoService;
    private final DingService dingService;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final ChatClient chatClient;

    public UnifiedAiService(AiFileChunkMapper aiFileChunkMapper,
                            AiDocumentIngestService aiDocumentIngestService,
                            ScheduleMapper scheduleMapper,
                            ObjectProvider<EmbeddingModel> embeddingModelProvider,
                            @Lazy ChatService chatService,
                            UserMapper userMapper,
                            ChatSessionMapper sessionMapper,
                            ChatSessionMemberMapper sessionMemberMapper,
                            MailboxService mailboxService,
                            NoticeService noticeService,
                            PermissionService permissionService,
                            TodoService todoService,
                            DingService dingService,
                            ChatClient chatClient) {
        this.aiFileChunkMapper = aiFileChunkMapper;
        this.aiDocumentIngestService = aiDocumentIngestService;
        this.scheduleMapper = scheduleMapper;
        this.embeddingModelProvider = embeddingModelProvider;
        this.chatClient = chatClient;
        this.userMapper = userMapper;
        this.sessionMapper = sessionMapper;
        this.sessionMemberMapper = sessionMemberMapper;
        this.mailboxService = mailboxService;
        this.noticeService = noticeService;
        this.permissionService = permissionService;
        this.todoService = todoService;
        this.dingService = dingService;
        this.chatService = chatService;
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

            // 3. 构建创建工具（传入当前 userId 避免 SecurityContext 丢失）
            ToolCallbackProvider toolProvider = MethodToolCallbackProvider.builder()
                    .toolObjects(
                            new ScheduleTool(scheduleMapper, userId),
                            new SendMessageTool(chatService, userMapper, sessionMapper,
                                    sessionMemberMapper, userId),
                            new SendMailTool(mailboxService, userMapper, userId),
                            new PublishNoticeTool(noticeService, permissionService, userId),
                            new CreateTodoTool(todoService, userMapper, userId),
                            new SendDingTool(dingService, userMapper, userId),
                            new SearchUserTool(userMapper, userId)
                    )
                    .build();
            ToolCallback[] allTools = toolProvider.getToolCallbacks();

            // 4. 调用 AI（同时携带文档上下文 + Function Calling 工具）
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(normalizedQuestion)
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, userId))
                    .toolCallbacks(java.util.Arrays.asList(allTools))
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
                你是企业协作平台的AI智能助手。你拥有以下能力：

        **基础能力**
        1. 回答用户问题 — 除了与用户基本的聊天，你还可以基于用户引用的文档片段回答。简洁口语化，不要用 Markdown。
        2. 查询用户信息 — 不确定接收人时，先调用 searchUser 确认。

        **协作工具（根据用户意图自动调用）**
        3. 创建日程 — 触发词："提醒我"、"创建日程"、"安排会议"
           → 调用 createSchedule
        4. 发送消息 — 触发词："帮我给XX发消息"、"替我说"、"帮我在群里通知"
           → 调用 sendMessage（优先用 searchUser 确认接收人）
        5. 发送邮件 — 触发词："帮我发邮件"、"给XX发封邮件"
           → 调用 sendMail
        6. 发送DING — 触发词："催一下XX"、"给XX发DING"、"DING一下"
           → 调用 sendDing
        7. 创建待办 — 触发词："帮我记个待办"、"添加待办"、"提醒XX做"
           → 调用 createTodo
        8. 发布公告 — 触发词："发布公告"、"发通知给全员"
           → 调用 publishNotice（需管理员权限）

        **重要规则**
        - 需要接收人的操作，名字不明确时先询问或调用 searchUser。
        - 管理员操作如用户无权，清晰告知并建议联系管理员。
        - 信息不完整时友好询问补充。
        - 所有时间以当前系统时间为基准推算。

        当前系统时间：""").append(LocalDateTime.now().format(DATE_TIME_FMT));

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


    // ================== 发送消息工具 ==================

    public static class SendMessageTool {

        private final ChatService chatService;
        private final UserMapper userMapper;
        private final ChatSessionMapper sessionMapper;
        private final ChatSessionMemberMapper sessionMemberMapper;
        private final Long userId;

        public SendMessageTool(ChatService chatService, UserMapper userMapper,
                               ChatSessionMapper sessionMapper,
                               ChatSessionMemberMapper sessionMemberMapper,
                               Long userId) {
            this.chatService = chatService;
            this.userMapper = userMapper;
            this.sessionMapper = sessionMapper;
            this.sessionMemberMapper = sessionMemberMapper;
            this.userId = userId;
        }

        @org.springframework.ai.tool.annotation.Tool(
                name = "sendMessage",
                description = "帮用户向指定联系人发送消息。用户说'帮我给XX发消息'、" +
                        "'替我给XX说'、'帮我在群里通知'等关键词时使用。" +
                        "需要提取：接收人姓名或群名、消息内容。" +
                        "如果用户没有明确接收人或消息内容，询问用户补充。"
        )
        public String sendMessage(
                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "接收人姓名（昵称）或群聊名称，如'洪辰新'、'产品讨论组'"
                ) String targetName,

                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "要发送的消息文本内容"
                ) String content
        ) {
            try {
                // ===== 第1层：参数非空校验 =====
                if (targetName == null || targetName.trim().isEmpty()) {
                    return "❌ 发送消息失败：请指定接收人姓名或群聊名称。\n"
                            + "例如：'帮我在产品讨论组里发一条消息' 或 '帮我给洪辰新发消息'";
                }
                if (content == null || content.trim().isEmpty()) {
                    return "❌ 发送消息失败：消息内容不能为空。\n请提供要发送的消息内容。";
                }

                String trimmedName = targetName.trim();
                String trimmedContent = content.trim();

                // ===== 第2层：查找接收人/群聊 =====
                // 先匹配单聊用户（按昵称）
                List<SysUser> matchedUsers = userMapper.selectList(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getStatus, 1)
                                .like(SysUser::getNickname, trimmedName)
                );

                // 再查群聊（按名称）
                List<ChatSession> matchedGroups = sessionMapper.selectList(
                        new LambdaQueryWrapper<ChatSession>()
                                .eq(ChatSession::getType, 2)
                                .like(ChatSession::getName, trimmedName)
                );

                // ===== 第3层：匹配判断 =====
                if (!matchedUsers.isEmpty()) {
                    SysUser targetUser = matchedUsers.stream()
                            .filter(u -> u.getNickname().equals(trimmedName))
                            .findFirst().orElse(matchedUsers.get(0));

                    if (targetUser.getId().equals(userId)) {
                        return "❌ 不能给自己发消息。请指定其他接收人。";
                    }

                    Long sessionId = chatService.getOrCreateSingle(userId, targetUser.getId());

                    SendMessageDTO dto = new SendMessageDTO();
                    dto.setSessionId(sessionId);
                    dto.setContent(trimmedContent);
                    dto.setContentType(1);
                    chatService.sendMessage(userId, dto);

                    return "✅ 已成功向 " + targetUser.getNickname() + " 发送消息。\n"
                            + "内容：" + (trimmedContent.length() > 50
                            ? trimmedContent.substring(0, 50) + "..." : trimmedContent);

                } else if (!matchedGroups.isEmpty()) {
                    ChatSession group = matchedGroups.stream()
                            .filter(g -> g.getName().equals(trimmedName))
                            .findFirst().orElse(matchedGroups.get(0));

                    Long memberCount = sessionMemberMapper.selectCount(
                            new LambdaQueryWrapper<ChatSessionMember>()
                                    .eq(ChatSessionMember::getSessionId, group.getId())
                                    .eq(ChatSessionMember::getUserId, userId)
                    );
                    if (memberCount == 0) {
                        return "❌ 发送消息失败：你不是「" + group.getName()
                                + "」的成员，无法在此群发送消息。";
                    }

                    SendMessageDTO dto = new SendMessageDTO();
                    dto.setSessionId(group.getId());
                    dto.setContent(trimmedContent);
                    dto.setContentType(1);
                    chatService.sendMessage(userId, dto);

                    return "✅ 已在群聊「" + group.getName() + "」中发送消息。\n"
                            + "内容：" + (trimmedContent.length() > 50
                            ? trimmedContent.substring(0, 50) + "..." : trimmedContent);

                } else {
                    return "❌ 未找到名为「" + trimmedName + "」的联系人或群聊。\n"
                            + "请检查姓名或群名是否正确，你也可以在通讯录中搜索联系人。";
                }

            } catch (BizException e) {
                return "❌ 发送消息失败：" + e.getMessage();
            } catch (Exception e) {
                log.error("AI 发送消息失败 userId={}", userId, e);
                return "❌ 发送消息失败，系统异常：" + e.getMessage();
            }
        }
    }

    // ================== 发送邮件工具 ==================

    public static class SendMailTool {

        private final MailboxService mailboxService;
        private final UserMapper userMapper;
        private final Long userId;

        public SendMailTool(MailboxService mailboxService, UserMapper userMapper, Long userId) {
            this.mailboxService = mailboxService;
            this.userMapper = userMapper;
            this.userId = userId;
        }

        @org.springframework.ai.tool.annotation.Tool(
                name = "sendMail",
                description = "帮用户发送邮件。当用户说'帮我发邮件'、'给XX发封邮件'、" +
                        "'发邮件通知XX'等关键词时使用。需要：收件人姓名、邮件主题、正文。" +
                        "如果用户没有提供主题或正文，询问用户补充。"
        )
        public String sendMail(
                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "收件人姓名（昵称），多个收件人用逗号分隔，如'洪辰新,陈豫琪'"
                ) String recipientNames,

                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "邮件主题/标题"
                ) String subject,

                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "邮件正文内容"
                ) String content,

                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "抄送人姓名，可选，多个用逗号分隔"
                ) String ccNames,

                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "邮件优先级：high/normal/low，默认 normal"
                ) String priority
        ) {
            try {
                if (recipientNames == null || recipientNames.trim().isEmpty()) {
                    return "❌ 发送邮件失败：请指定收件人。\n例如：'帮我给洪辰新发封邮件，主题是...'";
                }
                if (subject == null || subject.trim().isEmpty()) {
                    return "❌ 发送邮件失败：请输入邮件主题。";
                }
                if (content == null || content.trim().isEmpty()) {
                    return "❌ 发送邮件失败：请输入邮件正文。";
                }

                String[] nameArr = recipientNames.split("[,，、]");
                List<Long> recipientIds = new ArrayList<>();
                List<String> foundNames = new ArrayList<>();
                List<String> notFoundNames = new ArrayList<>();

                for (String name : nameArr) {
                    String trimmed = name.trim();
                    if (trimmed.isEmpty()) continue;
                    SysUser user = userMapper.selectOne(
                            new LambdaQueryWrapper<SysUser>()
                                    .eq(SysUser::getStatus, 1)
                                    .eq(SysUser::getNickname, trimmed)
                    );
                    if (user != null && !user.getId().equals(userId)) {
                        recipientIds.add(user.getId());
                        foundNames.add(trimmed);
                    } else if (user != null && user.getId().equals(userId)) {
                        return "❌ 发送邮件失败：不能给自己发邮件。请指定其他收件人。";
                    } else {
                        notFoundNames.add(trimmed);
                    }
                }

                if (recipientIds.isEmpty()) {
                    return "❌ 发送邮件失败：未找到任何有效收件人。\n"
                            + (notFoundNames.isEmpty() ? ""
                            : "未找到：" + String.join("、", notFoundNames)
                              + "，请检查姓名是否正确。");
                }

                List<Long> ccIds = new ArrayList<>();
                if (ccNames != null && !ccNames.trim().isEmpty()) {
                    for (String name : ccNames.split("[,，、]")) {
                        String trimmed = name.trim();
                        if (trimmed.isEmpty()) continue;
                        SysUser user = userMapper.selectOne(
                                new LambdaQueryWrapper<SysUser>()
                                        .eq(SysUser::getStatus, 1)
                                        .eq(SysUser::getNickname, trimmed)
                        );
                        if (user != null && !user.getId().equals(userId)
                                && !recipientIds.contains(user.getId())) {
                            ccIds.add(user.getId());
                        }
                    }
                }

                SendMailDTO dto = new SendMailDTO();
                dto.setRecipientIds(recipientIds);
                dto.setCcIds(ccIds);
                dto.setSubject(subject.trim());
                dto.setContent(content.trim());
                dto.setPriority(priority != null ? priority.trim() : "normal");
                mailboxService.send(userId, dto);

                StringBuilder result = new StringBuilder();
                result.append("✅ 邮件已成功发送！\n");
                result.append("收件人：").append(String.join("、", foundNames));
                if (!notFoundNames.isEmpty()) {
                    result.append("\n⚠️ 未找到以下收件人：")
                            .append(String.join("、", notFoundNames));
                }
                result.append("\n主题：").append(subject.trim());
                return result.toString();

            } catch (BizException e) {
                return "❌ 发送邮件失败：" + e.getMessage();
            } catch (Exception e) {
                log.error("AI 发送邮件失败 userId={}", userId, e);
                return "❌ 发送邮件失败，系统异常：" + e.getMessage();
            }
        }
    }

    // ================== 发布公告工具（管理员权限） ==================

    public static class PublishNoticeTool {

        private final NoticeService noticeService;
        private final PermissionService permissionService;
        private final Long userId;

        public PublishNoticeTool(NoticeService noticeService,
                                 PermissionService permissionService, Long userId) {
            this.noticeService = noticeService;
            this.permissionService = permissionService;
            this.userId = userId;
        }

        @org.springframework.ai.tool.annotation.Tool(
                name = "publishNotice",
                description = "发布系统公告或通知（需管理员权限）。" +
                        "当用户说'发布公告'、'发通知给全员'等关键词时使用。" +
                        "需要提取：公告标题、内容、优先级。如果用户不是管理员，告知无权限。"
        )
        public String publishNotice(
                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "公告标题"
                ) String title,

                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "公告正文内容"
                ) String content,

                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "优先级：1=紧急, 2=普通，默认2"
                ) Integer priority,

                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "类型：1=通知, 2=公告，默认1"
                ) Integer type
        ) {
            try {
                // 权限校验
                Set<String> perms = permissionService.getPerms(userId);
                if (!perms.contains("notice:add")) {
                    return "❌ 发布公告失败：你没有发布系统公告的权限。\n"
                            + "只有管理员可以发布公告。如需发布，请联系管理员。";
                }

                if (title == null || title.trim().isEmpty()) {
                    return "❌ 发布公告失败：请输入公告标题。";
                }
                if (content == null || content.trim().isEmpty()) {
                    return "❌ 发布公告失败：请输入公告内容。";
                }

                SysNotice notice = new SysNotice();
                notice.setTitle(title.trim());
                notice.setContent(content.trim());
                notice.setType(type != null ? type : 1);
                notice.setPriority(priority != null ? priority : 2);
                notice.setPublisherId(userId);
                notice.setStatus(1);
                notice.setCreateTime(LocalDateTime.now());
                noticeService.save(notice);

                String typeLabel = (type != null && type == 2) ? "公告" : "通知";
                String priorityLabel = (priority != null && priority == 1) ? "【紧急】" : "";
                return "✅ " + priorityLabel + typeLabel + "发布成功！\n"
                        + "标题：「" + title.trim() + "」\n"
                        + "已向全员推送，你可以在公告管理中查看阅读情况。";

            } catch (Exception e) {
                log.error("AI 发布公告失败 userId={}", userId, e);
                return "❌ 发布公告失败：" + e.getMessage();
            }
        }
    }

    // ================== 创建待办工具 ==================

    public static class CreateTodoTool {

        private final TodoService todoService;
        private final UserMapper userMapper;
        private final Long userId;

        public CreateTodoTool(TodoService todoService, UserMapper userMapper, Long userId) {
            this.todoService = todoService;
            this.userMapper = userMapper;
            this.userId = userId;
        }

        @org.springframework.ai.tool.annotation.Tool(
                name = "createTodo",
                description = "创建一个新的待办事项。当用户说'帮我记个待办'、'添加待办'、" +
                        "'提醒XX做XX'等关键词时使用。需要：标题、内容、截止时间、指派人、优先级。"
        )
        public String createTodo(
                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "待办任务标题，如'完成Q3总结报告'"
                ) String title,

                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "待办详细描述，可选"
                ) String content,

                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "截止时间，格式：yyyy-MM-dd HH:mm:ss，可选"
                ) String dueTimeStr,

                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "指派给谁（昵称），默认为自己"
                ) String assigneeName,

                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "优先级：1=高, 2=中, 3=低，默认2"
                ) Integer priority
        ) {
            try {
                if (title == null || title.trim().isEmpty()) {
                    return "❌ 创建待办失败：请输入待办标题。\n例如：'帮我记个待办：周五前完成Q3报告'";
                }

                Long assigneeId = userId;
                String resolvedAssigneeName = null;
                if (assigneeName != null && !assigneeName.trim().isEmpty()) {
                    SysUser assignee = userMapper.selectOne(
                            new LambdaQueryWrapper<SysUser>()
                                    .eq(SysUser::getStatus, 1)
                                    .eq(SysUser::getNickname, assigneeName.trim())
                    );
                    if (assignee == null) {
                        return "❌ 创建待办失败：未找到用户「" + assigneeName.trim() + "」。";
                    }
                    assigneeId = assignee.getId();
                    resolvedAssigneeName = assignee.getNickname();
                } else {
                    SysUser self = userMapper.selectById(userId);
                    resolvedAssigneeName = self != null ? self.getNickname() : "自己";
                }

                LocalDateTime dueTime = null;
                if (dueTimeStr != null && !dueTimeStr.trim().isEmpty()) {
                    try {
                        dueTime = LocalDateTime.parse(dueTimeStr.trim(), DATE_TIME_FMT);
                    } catch (DateTimeParseException e) {
                        try {
                            dueTime = LocalDateTime.parse(dueTimeStr.trim(),
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                        } catch (DateTimeParseException e2) {
                            return "❌ 创建待办失败：截止时间格式不正确。\n"
                                    + "请使用格式：yyyy-MM-dd HH:mm:ss";
                        }
                    }
                    if (dueTime != null && dueTime.isBefore(LocalDateTime.now())) {
                        return "❌ 创建待办失败：截止时间不能早于当前时间。";
                    }
                }

                SysTodo todo = new SysTodo();
                todo.setTitle(title.trim());
                todo.setContent(content != null ? content.trim() : "");
                todo.setCreatorId(userId);
                todo.setAssigneeId(assigneeId);
                todo.setDueTime(dueTime);
                todo.setPriority(priority != null ? priority : 2);
                todo.setStatus(0);
                todoService.save(todo, userId);

                StringBuilder result = new StringBuilder();
                result.append("✅ 待办事项已创建！\n");
                result.append("标题：「").append(title.trim()).append("」\n");
                result.append("负责人：").append(resolvedAssigneeName);
                if (dueTime != null) {
                    result.append("\n截止时间：").append(dueTime.format(DATE_TIME_FMT));
                }
                String pLabel = switch (priority != null ? priority : 2) {
                    case 1 -> "高"; case 3 -> "低"; default -> "中";
                };
                result.append("\n优先级：").append(pLabel);
                return result.toString();

            } catch (BizException e) {
                return "❌ 创建待办失败：" + e.getMessage();
            } catch (Exception e) {
                log.error("AI 创建待办失败 userId={}", userId, e);
                return "❌ 创建待办失败，系统异常：" + e.getMessage();
            }
        }
    }

    // ================== 发送 DING 工具 ==================

    public static class SendDingTool {

        private final DingService dingService;
        private final UserMapper userMapper;
        private final Long userId;

        public SendDingTool(DingService dingService, UserMapper userMapper, Long userId) {
            this.dingService = dingService;
            this.userMapper = userMapper;
            this.userId = userId;
        }

        @org.springframework.ai.tool.annotation.Tool(
                name = "sendDing",
                description = "向指定用户发送DING提醒。当用户说'给XX发个DING'、" +
                        "'催一下XX'、'DING一下XX'等关键词时使用。" +
                        "需要：接收人姓名、标题、截止时间(yyyy-MM-dd HH:mm:ss)、优先级(high/normal/low)。"
        )
        public String sendDing(
                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "接收人姓名（昵称），如'陈豫琪'"
                ) String targetName,

                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "DING提醒标题，如'请尽快提交月度报告'"
                ) String title,

                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "DING提醒详细说明，可选"
                ) String content,

                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "截止时间，格式：yyyy-MM-dd HH:mm:ss，必填"
                ) String deadlineStr,

                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "优先级：high/normal/low，默认 normal"
                ) String priority
        ) {
            try {
                if (targetName == null || targetName.trim().isEmpty()) {
                    return "❌ 发送DING失败：请指定接收人。\n例如：'帮我催一下陈豫琪提交周报'";
                }
                if (title == null || title.trim().isEmpty()) {
                    return "❌ 发送DING失败：请输入提醒标题。";
                }
                if (deadlineStr == null || deadlineStr.trim().isEmpty()) {
                    return "❌ 发送DING失败：请指定截止时间。\n"
                            + "格式：yyyy-MM-dd HH:mm:ss，如 '2026-07-05 18:00:00'";
                }

                SysUser target = userMapper.selectOne(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getStatus, 1)
                                .eq(SysUser::getNickname, targetName.trim())
                );
                if (target == null) {
                    return "❌ 发送DING失败：未找到用户「" + targetName.trim() + "」。";
                }
                if (target.getId().equals(userId)) {
                    return "❌ 发送DING失败：不能给自己发DING。";
                }

                LocalDateTime deadline;
                try {
                    deadline = LocalDateTime.parse(deadlineStr.trim(), DATE_TIME_FMT);
                } catch (DateTimeParseException e) {
                    try {
                        deadline = LocalDateTime.parse(deadlineStr.trim(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    } catch (DateTimeParseException e2) {
                        return "❌ 发送DING失败：截止时间格式不正确。\n"
                                + "请使用格式：yyyy-MM-dd HH:mm:ss";
                    }
                }
                if (deadline.isBefore(LocalDateTime.now())) {
                    return "❌ 发送DING失败：截止时间不能早于当前时间。";
                }

                SendDingDTO dto = new SendDingDTO();
                dto.setTargetUserId(target.getId());
                dto.setTitle(title.trim());
                dto.setContent(content != null ? content.trim() : "");
                dto.setDeadlineTime(deadline);
                dto.setPriority(priority != null ? priority.trim() : "normal");
                dingService.send(userId, dto);

                return "✅ DING 提醒已发送！\n"
                        + "接收人：" + target.getNickname() + "\n"
                        + "标题：「" + title.trim() + "」\n"
                        + "截止时间：" + deadline.format(DATE_TIME_FMT);

            } catch (BizException e) {
                return "❌ 发送DING失败：" + e.getMessage();
            } catch (Exception e) {
                log.error("AI 发送DING失败 userId={}", userId, e);
                return "❌ 发送DING失败，系统异常：" + e.getMessage();
            }
        }
    }

    // ================== 用户查询工具（辅助） ==================

    public static class SearchUserTool {

        private final UserMapper userMapper;
        private final Long userId;

        public SearchUserTool(UserMapper userMapper, Long userId) {
            this.userMapper = userMapper;
            this.userId = userId;
        }

        @org.springframework.ai.tool.annotation.Tool(
                name = "searchUser",
                description = "在通讯录中搜索用户。根据姓名（昵称）模糊匹配，" +
                        "返回匹配到的用户列表（姓名、职位、部门）。" +
                        "用于确认接收人是否存在，再调用 sendMessage/sendMail/sendDing 等工具。"
        )
        public String searchUser(
                @org.springframework.ai.tool.annotation.ToolParam(
                        description = "要搜索的用户姓名或昵称关键字，如'陈'"
                ) String keyword
        ) {
            try {
                if (keyword == null || keyword.trim().isEmpty()) {
                    return "请提供搜索关键字。";
                }

                List<SysUser> users = userMapper.selectList(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getStatus, 1)
                                .like(SysUser::getNickname, keyword.trim())
                                .last("limit 10")
                );

                users = users.stream()
                        .filter(u -> !u.getId().equals(userId))
                        .collect(Collectors.toList());

                if (users.isEmpty()) {
                    return "未找到匹配「" + keyword.trim() + "」的用户。";
                }

                StringBuilder sb = new StringBuilder();
                sb.append("找到 ").append(users.size()).append(" 个匹配用户：\n");
                for (int i = 0; i < users.size(); i++) {
                    SysUser u = users.get(i);
                    sb.append(i + 1).append(". ").append(u.getNickname());
                    if (u.getJobTitle() != null && !u.getJobTitle().isEmpty()) {
                        sb.append("（").append(u.getJobTitle()).append("）");
                    }
                    if (u.getDeptName() != null && !u.getDeptName().isEmpty()) {
                        sb.append(" - ").append(u.getDeptName());
                    }
                    sb.append(" [ID:").append(u.getId()).append("]");
                    if (i < users.size() - 1) sb.append("\n");
                }
                return sb.toString();

            } catch (Exception e) {
                log.error("搜索用户失败", e);
                return "搜索用户时出错：" + e.getMessage();
            }
        }
    }
}
