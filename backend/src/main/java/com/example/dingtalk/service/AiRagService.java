package com.example.dingtalk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.entity.AiFileChunk;
import com.example.dingtalk.mapper.AiFileChunkMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
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
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    public AiRagService(AiFileChunkMapper aiFileChunkMapper,
                        AiDocumentIngestService aiDocumentIngestService,
                        ObjectProvider<EmbeddingModel> embeddingModelProvider,
                        ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
        this.aiFileChunkMapper = aiFileChunkMapper;
        this.aiDocumentIngestService = aiDocumentIngestService;
        this.embeddingModelProvider = embeddingModelProvider;
        this.chatClientBuilderProvider = chatClientBuilderProvider;
    }

    public Flux<String> answer(Long userId, String question) {
        String normalizedQuestion = question == null ? "" : question.trim();
        if (normalizedQuestion.isEmpty()) {
            return Flux.just("请先输入问题。");
        }
        if (!isAiReady()) {
            return Flux.just("AI 未配置，请设置 SPRING_AI_OPENAI_API_KEY 后重试。");
        }

        aiDocumentIngestService.reindexMissingFiles(userId, 20);

        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        ChatClient.Builder chatClientBuilder = chatClientBuilderProvider.getIfAvailable();
        if (embeddingModel == null || chatClientBuilder == null) {
            return Flux.just("AI 客户端未初始化，请检查 Spring AI 配置。");
        }

        try {
            // 检索相关文档
            float[] queryEmbedding = embeddingModel.embed(normalizedQuestion);
            List<AiFileChunk> allChunks = aiFileChunkMapper.selectList(new LambdaQueryWrapper<AiFileChunk>()
                    .eq(AiFileChunk::getUserId, userId));
            List<ScoredChunk> relevantChunks = rankChunks(allChunks, queryEmbedding);

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
            ChatClient chatClient = chatClientBuilder.build();
            return chatClient.prompt(prompt)
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
            String block = "[" + index++ + "] " + chunk.chunk().getFileName() + "\n"
                    + chunk.chunk().getChunkText() + "\n";
            if (currentLength + block.length() > maxContextChars) {
                break;
            }
            contextBuilder.append(block).append('\n');
            currentLength += block.length();
        }
        return """
                你是企业协作平台里的问答助手，请严格基于提供的文档片段回答，并使用中文。
                如果文档中没有直接答案，就明确回复"在当前文档中没有找到直接依据"，不要编造。
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
