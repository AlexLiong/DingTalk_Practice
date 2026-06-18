package com.example.dingtalk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.common.BizException;
import com.example.dingtalk.dto.RememberAddUsageDTO;
import com.example.dingtalk.dto.SaveQuickLinkDTO;
import com.example.dingtalk.entity.ChatAddUsage;
import com.example.dingtalk.entity.ChatSavedLink;
import com.example.dingtalk.mapper.AddUsageMapper;
import com.example.dingtalk.mapper.SavedLinkMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddPanelService {

    private final SavedLinkMapper savedLinkMapper;
    private final AddUsageMapper addUsageMapper;

    public AddPanelService(SavedLinkMapper savedLinkMapper, AddUsageMapper addUsageMapper) {
        this.savedLinkMapper = savedLinkMapper;
        this.addUsageMapper = addUsageMapper;
    }

    public List<ChatSavedLink> listLinks(Long userId) {
        return savedLinkMapper.selectList(
                new LambdaQueryWrapper<ChatSavedLink>()
                        .eq(ChatSavedLink::getUserId, userId)
                        .orderByDesc(ChatSavedLink::getUpdateTime)
                        .orderByDesc(ChatSavedLink::getCreateTime)
        );
    }

    @Transactional
    public ChatSavedLink saveLink(Long userId, SaveQuickLinkDTO dto) {
        String url = requireText(dto.getUrl(), "请输入链接");
        String title = requireText(dto.getTitle(), "请输入链接标题");
        LocalDateTime now = LocalDateTime.now();

        ChatSavedLink existing = savedLinkMapper.selectOne(
                new LambdaQueryWrapper<ChatSavedLink>()
                        .eq(ChatSavedLink::getUserId, userId)
                        .eq(ChatSavedLink::getUrl, url)
                        .last("limit 1")
        );
        if (existing != null) {
            existing.setTitle(title);
            existing.setUpdateTime(now);
            savedLinkMapper.updateById(existing);
            return existing;
        }

        ChatSavedLink link = new ChatSavedLink();
        link.setUserId(userId);
        link.setTitle(title);
        link.setUrl(url);
        link.setCreateTime(now);
        link.setUpdateTime(now);
        savedLinkMapper.insert(link);
        return link;
    }

    @Transactional
    public void deleteLink(Long userId, Long id) {
        ChatSavedLink link = savedLinkMapper.selectById(id);
        if (link == null || !userId.equals(link.getUserId())) {
            throw new BizException("链接不存在");
        }
        savedLinkMapper.deleteById(id);
        addUsageMapper.delete(
                new LambdaQueryWrapper<ChatAddUsage>()
                        .eq(ChatAddUsage::getUserId, userId)
                        .eq(ChatAddUsage::getItemKey, "link:" + id)
        );
    }

    public List<String> listRecentUsageKeys(Long userId) {
        return addUsageMapper.selectList(
                new LambdaQueryWrapper<ChatAddUsage>()
                        .eq(ChatAddUsage::getUserId, userId)
                        .orderByDesc(ChatAddUsage::getUseTime)
                        .last("limit 20")
        ).stream().map(ChatAddUsage::getItemKey).collect(Collectors.toList());
    }

    @Transactional
    public void rememberUsage(Long userId, RememberAddUsageDTO dto) {
        String itemKey = requireText(dto.getItemKey(), "缺少最近使用项");
        LocalDateTime now = LocalDateTime.now();
        ChatAddUsage usage = addUsageMapper.selectOne(
                new LambdaQueryWrapper<ChatAddUsage>()
                        .eq(ChatAddUsage::getUserId, userId)
                        .eq(ChatAddUsage::getItemKey, itemKey)
                        .last("limit 1")
        );
        if (usage == null) {
            usage = new ChatAddUsage();
            usage.setUserId(userId);
            usage.setItemKey(itemKey);
            usage.setUseTime(now);
            addUsageMapper.insert(usage);
            return;
        }
        usage.setUseTime(now);
        addUsageMapper.updateById(usage);
    }

    public void clearRecentUsage(Long userId) {
        addUsageMapper.delete(new LambdaQueryWrapper<ChatAddUsage>().eq(ChatAddUsage::getUserId, userId));
    }

    private String requireText(String value, String message) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            throw new BizException(message);
        }
        return text;
    }
}
