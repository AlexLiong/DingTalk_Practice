package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_file_chunk")
public class AiFileChunk {
    private Long id;
    private Long fileId;
    private Long userId;
    private String fileName;
    private String fileUrl;
    private Integer chunkIndex;
    private String chunkText;
    private String embeddingJson;
    private String metadataJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
