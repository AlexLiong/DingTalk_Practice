package com.example.dingtalk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.common.Result;
import com.example.dingtalk.common.SecurityUtils;
import com.example.dingtalk.entity.ChatMessageReaction;
import com.example.dingtalk.entity.SysUser;
import com.example.dingtalk.mapper.ReactionMapper;
import com.example.dingtalk.mapper.UserMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reaction")
public class ReactionController {

    private final ReactionMapper reactionMapper;
    private final UserMapper userMapper;

    public ReactionController(ReactionMapper reactionMapper, UserMapper userMapper) {
        this.reactionMapper = reactionMapper;
        this.userMapper = userMapper;
    }

    /** 获取某条消息的所有反应 */
    @GetMapping("/{messageId}")
    public Result<List<Map<String, Object>>> get(@PathVariable Long messageId) {
        List<ChatMessageReaction> list = reactionMapper.selectList(
                new LambdaQueryWrapper<ChatMessageReaction>()
                        .eq(ChatMessageReaction::getMessageId, messageId));
        // 按 emoji 分组
        Map<String, List<ChatMessageReaction>> grouped = list.stream()
                .collect(Collectors.groupingBy(ChatMessageReaction::getEmoji));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<ChatMessageReaction>> entry : grouped.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("emoji", entry.getKey());
            item.put("count", entry.getValue().size());
            List<String> users = new ArrayList<>();
            for (ChatMessageReaction r : entry.getValue()) {
                SysUser u = userMapper.selectById(r.getUserId());
                if (u != null) users.add(u.getNickname());
            }
            item.put("users", users);
            item.put("userIds", entry.getValue().stream().map(ChatMessageReaction::getUserId).collect(Collectors.toList()));
            result.add(item);
        }
        return Result.ok(result);
    }

    /** 添加/切换反应 (同一用户同一emoji再次点击则取消) */
    @PostMapping
    public Result<?> toggle(@RequestBody Map<String, Object> body) {
        Long messageId = Long.parseLong(body.get("messageId").toString());
        String emoji = (String) body.get("emoji");
        Long userId = SecurityUtils.getUserId();

        Long exists = reactionMapper.selectCount(new LambdaQueryWrapper<ChatMessageReaction>()
                .eq(ChatMessageReaction::getMessageId, messageId)
                .eq(ChatMessageReaction::getUserId, userId)
                .eq(ChatMessageReaction::getEmoji, emoji));
        if (exists > 0) {
            reactionMapper.delete(new LambdaQueryWrapper<ChatMessageReaction>()
                    .eq(ChatMessageReaction::getMessageId, messageId)
                    .eq(ChatMessageReaction::getUserId, userId)
                    .eq(ChatMessageReaction::getEmoji, emoji));
        } else {
            ChatMessageReaction r = new ChatMessageReaction();
            r.setMessageId(messageId);
            r.setUserId(userId);
            r.setEmoji(emoji);
            r.setCreateTime(LocalDateTime.now());
            reactionMapper.insert(r);
        }
        return Result.ok();
    }
}
