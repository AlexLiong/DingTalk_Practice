package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_mailbox_mail")
public class SysMailboxMail {
    private Long id;
    private Long userId;
    private Long senderId;
    private String direction;
    private String sender;
    private String senderRole;
    private String subject;
    private String preview;
    private String content;
    private LocalDateTime sendTime;
    private Integer unread;
    private Integer starred;
    private Integer archived;
    private String tagName;
    private String priority;
    private String recipientsJson;
    private String ccJson;
    private String attachmentsJson;
    private String draft;
    private String useCasesJson;
    private String historyJson;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String time;
    @TableField(exist = false)
    private String tag;
    @TableField(exist = false)
    private List<String> recipients;
    @TableField(exist = false)
    private List<String> cc;
    @TableField(exist = false)
    private List<AttachmentItem> attachments;
    @TableField(exist = false)
    private List<UseCaseItem> useCases;
    @TableField(exist = false)
    private List<HistoryItem> history;

    @Data
    @NoArgsConstructor
    public static class AttachmentItem {
        private Long fileId;
        private String name;
        private String size;
        private String content;
        private String url;
        private String type;

        public AttachmentItem(String name, String size, String content) {
            this.name = name;
            this.size = size;
            this.content = content;
        }

        public AttachmentItem(Long fileId, String name, String size, String content, String url, String type) {
            this.fileId = fileId;
            this.name = name;
            this.size = size;
            this.content = content;
            this.url = url;
            this.type = type;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UseCaseItem {
        private String title;
        private String description;
        private String tag;
        private String template;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItem {
        private String time;
        private String text;
    }
}
