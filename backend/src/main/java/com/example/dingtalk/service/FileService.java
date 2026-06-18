package com.example.dingtalk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.common.BizException;
import com.example.dingtalk.dto.UpdateFileDTO;
import com.example.dingtalk.entity.SysFile;
import com.example.dingtalk.mapper.FileMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter HISTORY_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final Set<String> ALLOWED_STATUS = new LinkedHashSet<>(List.of(
            "已共享", "待评审", "待回收", "已锁版", "可下载", "可用"
    ));

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final FileMapper fileMapper;
    private final AiDocumentIngestService aiDocumentIngestService;

    public FileService(FileMapper fileMapper, AiDocumentIngestService aiDocumentIngestService) {
        this.fileMapper = fileMapper;
        this.aiDocumentIngestService = aiDocumentIngestService;
    }

    public SysFile upload(MultipartFile file, Long uploaderId) {
        if (file == null || file.isEmpty()) throw new BizException("文件为空");
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        // 按日期分目录
        String datePath = LocalDate.now().toString();
        File dir = new File(uploadDir, datePath);
        if (!dir.exists() && !dir.mkdirs()) throw new BizException("创建上传目录失败");

        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        File dest = new File(dir, fileName);
        try {
            file.transferTo(dest.getAbsoluteFile());
        } catch (IOException e) {
            throw new BizException("文件保存失败: " + e.getMessage());
        }

        String url = "/uploads/" + datePath + "/" + fileName;
        SysFile sf = new SysFile();
        sf.setName(original);
        sf.setUrl(url);
        sf.setSize(file.getSize());
        sf.setType(resolveStoredType(original, file.getContentType()));
        sf.setUploaderId(uploaderId);
        LocalDateTime now = LocalDateTime.now();
        sf.setCreateTime(now);
        sf.setUpdateTime(now);
        sf.setShareCount(0);
        sf.setHistoryJson("[]");
        fillDefaults(sf);
        appendHistory(sf, "已上传到文档中心", now);
        fileMapper.insert(sf);
        try {
            aiDocumentIngestService.indexUploadedFile(sf);
        } catch (Exception e) {
            log.warn("AI ingest failed, fileId={}, name={}", sf.getId(), sf.getName(), e);
        }
        hydrateHistory(sf);
        return sf;
    }

    public List<SysFile> list(Long uploaderId, Integer limit) {
        int size = limit == null ? 8 : Math.max(1, Math.min(limit, 20));
        List<SysFile> files = fileMapper.selectList(
                new LambdaQueryWrapper<SysFile>()
                        .eq(SysFile::getUploaderId, uploaderId)
                        .orderByDesc(SysFile::getUpdateTime)
                        .orderByDesc(SysFile::getCreateTime)
                        .last("limit " + size));
        for (SysFile file : files) {
            normalizeAndPersistIfNeeded(file);
        }
        return files;
    }

    public SysFile update(Long fileId, Long uploaderId, UpdateFileDTO dto) {
        SysFile file = requireOwned(fileId, uploaderId);
        fillDefaults(file);
        boolean changed = false;
        LocalDateTime now = LocalDateTime.now();

        if (dto.getName() != null) {
            String nextName = requireText(dto.getName(), "文件名不能为空");
            if (!nextName.equals(file.getName())) {
                file.setName(nextName);
                file.setType(resolveStoredType(nextName, file.getType()));
                appendHistory(file, "已重命名为 " + nextName, now);
                changed = true;
            }
        }
        if (dto.getScene() != null) {
            String nextScene = normalizeScene(dto.getScene(), file);
            if (!nextScene.equals(file.getScene())) {
                file.setScene(nextScene);
                appendHistory(file, "协作场景改为 " + nextScene, now);
                changed = true;
            }
        }
        if (dto.getDescription() != null) {
            String nextDescription = normalizeDescription(dto.getDescription(), file);
            if (!nextDescription.equals(file.getDescription())) {
                file.setDescription(nextDescription);
                appendHistory(file, "已更新文件说明", now);
                changed = true;
            }
        }
        if (dto.getStatus() != null) {
            String nextStatus = normalizeStatus(dto.getStatus(), file);
            if (!nextStatus.equals(file.getStatus())) {
                file.setStatus(nextStatus);
                appendStatusHistory(file, nextStatus, now);
                if ("已共享".equals(nextStatus)) {
                    file.setShareCount((file.getShareCount() == null ? 0 : file.getShareCount()) + 1);
                }
                changed = true;
            }
        }

        changed = fillDefaults(file) || changed;
        if (changed) {
            file.setUpdateTime(now);
            fileMapper.updateById(file);
            try {
                aiDocumentIngestService.syncFileMetadata(file);
            } catch (Exception e) {
                log.warn("AI chunk metadata sync failed, fileId={}", file.getId(), e);
            }
        }
        hydrateHistory(file);
        return file;
    }

    public SysFile share(Long fileId, Long uploaderId, String targetName) {
        String historyText = isBlank(targetName) ? "已分享给协作成员" : "已分享至 " + targetName;
        return applyAction(fileId, uploaderId, "已共享", historyText, true);
    }

    public SysFile lock(Long fileId, Long uploaderId) {
        return applyAction(fileId, uploaderId, "已锁版", "已锁定最终版本", false);
    }

    public SysFile recycle(Long fileId, Long uploaderId) {
        return applyAction(fileId, uploaderId, "待回收", "已标记待回收", false);
    }

    public void delete(Long fileId, Long uploaderId) {
        SysFile file = requireOwned(fileId, uploaderId);
        aiDocumentIngestService.deleteByFileId(file.getId());
        fileMapper.deleteById(file.getId());
        deletePhysicalFile(file.getUrl());
    }

    private SysFile requireOwned(Long fileId, Long uploaderId) {
        SysFile file = fileMapper.selectById(fileId);
        if (file == null || !uploaderId.equals(file.getUploaderId())) {
            throw new BizException("文件不存在");
        }
        return file;
    }

    private void normalizeAndPersistIfNeeded(SysFile file) {
        if (fillDefaults(file)) {
            fileMapper.updateById(file);
        }
        hydrateHistory(file);
    }

    private boolean fillDefaults(SysFile file) {
        boolean changed = false;
        if (isBlank(file.getScene())) {
            file.setScene(defaultScene(file));
            changed = true;
        }
        if (isBlank(file.getDescription())) {
            file.setDescription(defaultDescription(file));
            changed = true;
        }
        if (isBlank(file.getStatus())) {
            file.setStatus(defaultStatus(file));
            changed = true;
        } else {
            String normalizedStatus = normalizeStatus(file.getStatus(), file);
            if (!normalizedStatus.equals(file.getStatus())) {
                file.setStatus(normalizedStatus);
                changed = true;
            }
        }
        if (file.getUpdateTime() == null) {
            file.setUpdateTime(file.getCreateTime() != null ? file.getCreateTime() : LocalDateTime.now());
            changed = true;
        }
        if (isBlank(file.getType())) {
            file.setType(resolveStoredType(file.getName(), file.getType()));
            changed = true;
        }
        if (file.getShareCount() == null) {
            file.setShareCount(defaultShareCount(file));
            changed = true;
        }
        if (file.getHistoryJson() == null || file.getHistoryJson().isBlank()) {
            file.setHistoryJson(buildDefaultHistoryJson(file));
            changed = true;
        }
        if (normalizeHistoryOrder(file)) {
            changed = true;
        }
        return changed;
    }

    private SysFile applyAction(Long fileId, Long uploaderId, String status, String historyText, boolean increaseShareCount) {
        SysFile file = requireOwned(fileId, uploaderId);
        fillDefaults(file);
        LocalDateTime now = LocalDateTime.now();
        file.setStatus(status);
        file.setUpdateTime(now);
        if (increaseShareCount) {
            file.setShareCount((file.getShareCount() == null ? 0 : file.getShareCount()) + 1);
        }
        appendHistory(file, historyText, now);
        fileMapper.updateById(file);
        hydrateHistory(file);
        return file;
    }

    private String defaultScene(SysFile file) {
        return switch (detectTheme(file.getName())) {
            case "pdf" -> "周报同步";
            case "word" -> "排期评审";
            case "excel" -> "数据回传";
            case "ppt" -> "客户复盘";
            case "zip" -> "资源交付";
            default -> "通用协作";
        };
    }

    private String defaultDescription(SysFile file) {
        String name = isBlank(file.getName()) ? "协作文档" : file.getName();
        return switch (detectTheme(file.getName())) {
            case "pdf" -> name + " 适合会前同步、归档和正式版本分发。";
            case "word" -> name + " 适合做方案、排期或纪要的多人评审。";
            case "excel" -> name + " 适合数据回传、补数和进度分析。";
            case "ppt" -> name + " 适合客户复盘、方案演示和会前锁版。";
            case "zip" -> name + " 适合批量交付素材、压缩包和成套资源。";
            default -> name + " 支持上传、下载和链接分发，可直接用于协作。";
        };
    }

    private String defaultStatus(SysFile file) {
        return switch (detectTheme(file.getName())) {
            case "pdf" -> "已共享";
            case "word" -> "待评审";
            case "excel" -> "待回收";
            case "ppt" -> "已锁版";
            case "zip" -> "可下载";
            default -> "可用";
        };
    }

    private String normalizeScene(String value, SysFile file) {
        String text = trimToNull(value);
        return text == null ? defaultScene(file) : text;
    }

    private String normalizeDescription(String value, SysFile file) {
        String text = trimToNull(value);
        return text == null ? defaultDescription(file) : text;
    }

    private String normalizeStatus(String value, SysFile file) {
        String text = trimToNull(value);
        if (text == null) {
            return defaultStatus(file);
        }
        if (!ALLOWED_STATUS.contains(text)) {
            throw new BizException("不支持的文件状态");
        }
        return text;
    }

    private String resolveStoredType(String name, String originalType) {
        String ext = extensionOf(name);
        if (!ext.isEmpty()) {
            return ext;
        }
        return originalType == null ? "" : originalType;
    }

    private String detectTheme(String name) {
        String ext = extensionOf(name);
        if ("pdf".equals(ext)) return "pdf";
        if ("doc".equals(ext) || "docx".equals(ext)) return "word";
        if ("xls".equals(ext) || "xlsx".equals(ext) || "csv".equals(ext)) return "excel";
        if ("ppt".equals(ext) || "pptx".equals(ext)) return "ppt";
        if ("zip".equals(ext) || "rar".equals(ext) || "7z".equals(ext) || "tar".equals(ext) || "gz".equals(ext)) return "zip";
        return "file";
    }

    private String extensionOf(String name) {
        if (name == null) {
            return "";
        }
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) {
            return "";
        }
        return name.substring(idx + 1).toLowerCase();
    }

    private String requireText(String text, String errorMessage) {
        String trimmed = trimToNull(text);
        if (trimmed == null) {
            throw new BizException(errorMessage);
        }
        return trimmed;
    }

    private String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private void appendStatusHistory(SysFile file, String status, LocalDateTime time) {
        switch (status) {
            case "已共享" -> appendHistory(file, "已设为共享文档", time);
            case "待评审" -> appendHistory(file, "已提交评审", time);
            case "待回收" -> appendHistory(file, "已标记待回收", time);
            case "已锁版" -> appendHistory(file, "已锁定最终版本", time);
            case "可下载" -> appendHistory(file, "已开放下载", time);
            default -> appendHistory(file, "已更新文件状态", time);
        }
    }

    private void appendHistory(SysFile file, String text, LocalDateTime time) {
        List<SysFile.HistoryItem> history = readHistory(file.getHistoryJson());
        history.add(0, new SysFile.HistoryItem(formatHistoryTime(time), text));
        file.setHistoryJson(toJson(history));
    }

    private void hydrateHistory(SysFile file) {
        file.setHistory(readHistory(file.getHistoryJson()));
    }

    private boolean normalizeHistoryOrder(SysFile file) {
        List<SysFile.HistoryItem> history = readHistory(file.getHistoryJson());
        if (history.size() <= 1) {
            return false;
        }
        SysFile.HistoryItem first = history.get(0);
        if (first == null || !"已上传到文档中心".equals(first.getText())) {
            return false;
        }
        history.remove(0);
        history.add(first);
        file.setHistoryJson(toJson(history));
        return true;
    }

    private List<SysFile.HistoryItem> readHistory(String historyJson) {
        if (historyJson == null || historyJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(historyJson, new TypeReference<List<SysFile.HistoryItem>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BizException("文件记录序列化失败");
        }
    }

    private String formatHistoryTime(LocalDateTime time) {
        return HISTORY_FORMATTER.format(time);
    }

    private int defaultShareCount(SysFile file) {
        return switch (file.getStatus()) {
            case "已共享" -> 3;
            case "可下载" -> 1;
            default -> 0;
        };
    }

    private String buildDefaultHistoryJson(SysFile file) {
        List<SysFile.HistoryItem> history = new ArrayList<>();
        LocalDateTime createTime = file.getCreateTime() != null ? file.getCreateTime() : LocalDateTime.now();
        if (!"可用".equals(file.getStatus())) {
            history.add(new SysFile.HistoryItem(formatHistoryTime(file.getUpdateTime() != null ? file.getUpdateTime() : createTime), defaultStatusText(file.getStatus())));
        }
        history.add(new SysFile.HistoryItem(formatHistoryTime(createTime), "已上传到文档中心"));
        return toJson(history);
    }

    private String defaultStatusText(String status) {
        return switch (status) {
            case "已共享" -> "已设为共享文档";
            case "待评审" -> "已提交评审";
            case "待回收" -> "已标记待回收";
            case "已锁版" -> "已锁定最终版本";
            case "可下载" -> "已开放下载";
            default -> "已更新文件状态";
        };
    }

    private void deletePhysicalFile(String url) {
        if (url == null || !url.startsWith("/uploads/") || url.startsWith("/uploads/demo/")) {
            return;
        }
        String relativePath = url.substring("/uploads/".length());
        File physicalFile = new File(uploadDir, relativePath);
        if (physicalFile.exists() && !physicalFile.delete()) {
            physicalFile.deleteOnExit();
        }
    }
}
