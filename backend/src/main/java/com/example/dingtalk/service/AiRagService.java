package com.example.dingtalk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.dto.ExtraDTO;
import com.example.dingtalk.entity.AiFileChunk;
import com.example.dingtalk.mapper.AiFileChunkMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AiRagService {

    private static final Logger log = LoggerFactory.getLogger(AiRagService.class);
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
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final ChatClient chatClient;

    public AiRagService(AiFileChunkMapper aiFileChunkMapper,
                        AiDocumentIngestService aiDocumentIngestService,
                        ObjectProvider<EmbeddingModel> embeddingModelProvider, MessageChatMemoryAdvisor messageChatMemoryAdvisor, ChatClient chatClient) {
        this.aiFileChunkMapper = aiFileChunkMapper;
        this.aiDocumentIngestService = aiDocumentIngestService;
        this.embeddingModelProvider = embeddingModelProvider;
        this.chatClient = chatClient;
    }

    public Flux<String> answer(Long userId, String question, ExtraDTO extraDTO) {
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
            // 检索相关文档
            float[] queryEmbedding = embeddingModel.embed(normalizedQuestion);
            LambdaQueryWrapper<AiFileChunk> wrapper = new LambdaQueryWrapper<>();
            // 只查询当前用户拥有的文档
            wrapper.eq(AiFileChunk::getUserId, userId);

            //假设用户引用文档
            if(extraDTO!=null && extraDTO.getQuoteContent()!=null){
                wrapper.like(AiFileChunk::getFileName,extraDTO.getQuoteContent());
            }

            //查询指定文档
            List<AiFileChunk> targetChunks = aiFileChunkMapper.selectList(wrapper);
            List<ScoredChunk> relevantChunks = rankChunks(targetChunks, queryEmbedding);

            // 构建提示词
            String prompt;
            if (!relevantChunks.isEmpty()) {
                // 有相关文档，使用 RAG
                prompt = buildPrompt(relevantChunks, normalizedQuestion);
            } else {
                // 没有相关文档，但不阻塞，让大模型直接回答
                prompt = buildSimplePrompt(normalizedQuestion);
            }


            // 流式调用大模型
            return chatClient.prompt(prompt)
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, userId))
                    .stream()
                    .content()
                    .doOnError(e -> log.error("AI RAG stream failed for user {}", userId, e));
        } catch (Exception e) {
            log.error("AI RAG answer failed for user {}", userId, e);
            return Flux.just("AI 服务调用失败，请检查模型地址、密钥和网络后重试。");
        }
    }

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

    private String buildPrompt(List<ScoredChunk> chunks, String question) {
        StringBuilder contextBuilder = new StringBuilder();
        int currentLength = 0;
        int index = 1;
        for (ScoredChunk chunk : chunks) {
            String block =  chunk.chunk().getFileName()+"[" + index++ + "]:" + "\n"
                    + chunk.chunk().getChunkText() + "\n";
            if (currentLength + block.length() > maxContextChars) {
                break;
            }
            contextBuilder.append(block).append('\n');
            currentLength += block.length();
        }
        return """
                你是企业协作平台里的问答助手，请使用中文，不加带特殊语法（如md，katex），模仿人类办公对话的语气，根据参考文档内容回答用户的问题，并提供有价值的见解。。
                如果文档中没有直接答案，就明确回复"没有找到直接依据"，不要编造。
                如能回答，优先给出结论，再补充来自哪些文档片段。

                文档片段:
                %s
                用户问题:
                %s
                """.formatted(contextBuilder.toString().trim(), question);
    }

    private String buildSimplePrompt(String question) {
        return """
                你是企业协作平台里的通用助手，请使用中文，不加带特殊语法（如md，katex）的，模仿人类办公对话的语气回答用户的问题，并提供有价值的见解。
                用户问题:
                %s
                """.formatted(question);
    }

    private float[] parseEmbedding(String json) {
        if (json == null || json.isBlank()) {
            return new float[0];
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {
            });
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
        double dot = 0D;
        double leftNorm = 0D;
        double rightNorm = 0D;
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

    private record ScoredChunk(AiFileChunk chunk, double score) {
    }
}
