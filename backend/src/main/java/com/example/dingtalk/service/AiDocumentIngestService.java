package com.example.dingtalk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.dingtalk.entity.AiFileChunk;
import com.example.dingtalk.entity.SysFile;
import com.example.dingtalk.mapper.AiFileChunkMapper;
import com.example.dingtalk.mapper.FileMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AiDocumentIngestService {

    private static final Logger log = LoggerFactory.getLogger(AiDocumentIngestService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "txt", "md", "markdown", "csv", "json", "xml", "html", "htm",
            "java", "js", "ts", "css", "scss", "sql", "yml", "yaml", "properties", "log",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
    );

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${app.ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${spring.ai.openai.api-key:}")
    private String openAiApiKey;

    @Value("${app.ai.ingest.max-file-size-bytes:20971520}")
    private long maxFileSizeBytes;

    @Value("${app.ai.ingest.max-text-length:120000}")
    private int maxTextLength;

    @Value("${app.ai.ingest.chunk-size:500}")
    private int chunkSize;

    @Value("${app.ai.ingest.min-chunk-size-chars:120}")
    private int minChunkSizeChars;

    @Value("${app.ai.ingest.min-chunk-length-to-embed:40}")
    private int minChunkLengthToEmbed;

    @Value("${app.ai.ingest.max-num-chunks:80}")
    private int maxNumChunks;

    private final AiFileChunkMapper aiFileChunkMapper;
    private final FileMapper fileMapper;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final Parser parser = new AutoDetectParser();

    public AiDocumentIngestService(AiFileChunkMapper aiFileChunkMapper,
                                   FileMapper fileMapper,
                                   ObjectProvider<EmbeddingModel> embeddingModelProvider) {
        this.aiFileChunkMapper = aiFileChunkMapper;
        this.fileMapper = fileMapper;
        this.embeddingModelProvider = embeddingModelProvider;
    }

    @Transactional
    public void indexUploadedFile(SysFile file) {
        if (!canIngest(file)) {
            return;
        }
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (embeddingModel == null) {
            return;
        }
        Path storedPath = resolveStoredPath(file.getUrl());
        if (storedPath == null || !Files.isRegularFile(storedPath)) {
            return;
        }

        deleteByFileId(file.getId());
        String text = extractText(storedPath);
        if (text == null || text.isBlank()) {
            return;
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("fileId", file.getId());
        metadata.put("fileName", file.getName());
        metadata.put("fileUrl", file.getUrl());

        Document source = new Document(text, metadata);
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .withMinChunkSizeChars(minChunkSizeChars)
                .withMinChunkLengthToEmbed(minChunkLengthToEmbed)
                .withMaxNumChunks(maxNumChunks)
                .withKeepSeparator(true)
                .build();

        List<Document> chunks = splitter.split(source);
        LocalDateTime now = LocalDateTime.now();
        int chunkIndex = 0;
        for (Document chunk : chunks) {
            String chunkText = normalizeText(chunk.getText());
            if (chunkText.length() < minChunkLengthToEmbed) {
                continue;
            }
            float[] embedding = embeddingModel.embed(chunk);
            if (embedding == null || embedding.length == 0) {
                continue;
            }
            AiFileChunk entity = new AiFileChunk();
            entity.setFileId(file.getId());
            entity.setUserId(file.getUploaderId());
            entity.setFileName(file.getName());
            entity.setFileUrl(file.getUrl());
            entity.setChunkIndex(chunkIndex++);
            entity.setChunkText(chunkText);
            entity.setEmbeddingJson(toJson(embedding));
            entity.setMetadataJson(toJson(chunk.getMetadata()));
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            aiFileChunkMapper.insert(entity);
        }
    }

    public void reindexMissingFiles(Long userId, int limit) {
        if (!isAiReady()) {
            return;
        }
        int size = Math.max(1, Math.min(limit, 50));
        List<SysFile> files = fileMapper.selectList(new LambdaQueryWrapper<SysFile>()
                .eq(SysFile::getUploaderId, userId)
                .orderByDesc(SysFile::getUpdateTime)
                .orderByDesc(SysFile::getCreateTime)
                .last("limit " + size));
        for (SysFile file : files) {
            Long count = aiFileChunkMapper.selectCount(new LambdaQueryWrapper<AiFileChunk>()
                    .eq(AiFileChunk::getFileId, file.getId()));
            if (count == 0) {
                try {
                    indexUploadedFile(file);
                } catch (Exception e) {
                    log.warn("Backfill AI chunks failed, fileId={}, name={}", file.getId(), file.getName(), e);
                }
            }
        }
    }

    @Transactional
    public void deleteByFileId(Long fileId) {
        aiFileChunkMapper.delete(new LambdaQueryWrapper<AiFileChunk>()
                .eq(AiFileChunk::getFileId, fileId));
    }

    @Transactional
    public void syncFileMetadata(SysFile file) {
        aiFileChunkMapper.update(null, new LambdaUpdateWrapper<AiFileChunk>()
                .eq(AiFileChunk::getFileId, file.getId())
                .set(AiFileChunk::getFileName, file.getName())
                .set(AiFileChunk::getFileUrl, file.getUrl())
                .set(AiFileChunk::getUpdateTime, LocalDateTime.now()));
    }

    public boolean hasIndexedContent(Long userId) {
        return aiFileChunkMapper.selectCount(new LambdaQueryWrapper<AiFileChunk>()
                .eq(AiFileChunk::getUserId, userId)) > 0;
    }

    private boolean canIngest(SysFile file) {
        return file != null
                && file.getId() != null
                && file.getUploaderId() != null
                && isAiReady()
                && isSupported(file)
                && file.getSize() != null
                && file.getSize() > 0
                && file.getSize() <= maxFileSizeBytes;
    }

    private boolean isAiReady() {
        return aiEnabled
                && openAiApiKey != null
                && !openAiApiKey.isBlank()
                && !"disabled".equalsIgnoreCase(openAiApiKey.trim());
    }

    private boolean isSupported(SysFile file) {
        String ext = extensionOf(file.getName());
        return SUPPORTED_EXTENSIONS.contains(ext);
    }

    private Path resolveStoredPath(String url) {
        if (url == null || !url.startsWith("/uploads/") || url.startsWith("/uploads/demo/")) {
            return null;
        }
        String relativePath = url.substring("/uploads/".length());
        return Path.of(uploadDir, relativePath);
    }

    private String extractText(Path storedPath) {
        BodyContentHandler handler = new BodyContentHandler(maxTextLength);
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        context.set(Parser.class, parser);
        try (TikaInputStream inputStream = TikaInputStream.get(storedPath, metadata)) {
            parser.parse(inputStream, handler, metadata, context);
            return normalizeText(handler.toString());
        } catch (Exception e) {
            log.warn("Extract text failed: {}", storedPath, e);
            return "";
        }
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace('\u0000', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.length() <= maxTextLength) {
            return normalized;
        }
        return normalized.substring(0, maxTextLength);
    }

    private String extensionOf(String name) {
        if (name == null) {
            return "";
        }
        int index = name.lastIndexOf('.');
        if (index < 0 || index == name.length() - 1) {
            return "";
        }
        return name.substring(index + 1).toLowerCase();
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("AI chunk serialization failed", e);
        }
    }
}
