package com.example.dingtalk.controller;

import com.example.dingtalk.common.Result;
import com.example.dingtalk.common.SecurityUtils;
import com.example.dingtalk.dto.CreateGroupDTO;
import com.example.dingtalk.dto.SendMessageDTO;
import com.example.dingtalk.entity.ChatSession;
import com.example.dingtalk.service.ChatService;
import com.example.dingtalk.vo.MemberVO;
import com.example.dingtalk.vo.MessageVO;
import com.example.dingtalk.vo.SessionVO;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /* ---- 会话/消息 ---- */
    @GetMapping("/sessions")
    public Result<List<SessionVO>> sessions() {
        return Result.ok(chatService.getSessions(SecurityUtils.getUserId()));
    }

    @GetMapping("/messages")
    public Result<List<MessageVO>> messages(@RequestParam Long sessionId) {
        return Result.ok(chatService.getMessages(SecurityUtils.getUserId(), sessionId));
    }

    @PostMapping("/messages")
    public Result<MessageVO> send(@Valid @RequestBody SendMessageDTO dto) {
        return Result.ok(chatService.sendMessage(SecurityUtils.getUserId(), dto));
    }

    @PostMapping("/recall")
    public Result<?> recall(@RequestParam Long messageId) {
        chatService.recall(SecurityUtils.getUserId(), messageId);
        return Result.ok();
    }

    @PostMapping("/read")
    public Result<?> read(@RequestParam Long sessionId) {
        chatService.markRead(SecurityUtils.getUserId(), sessionId);
        return Result.ok();
    }

    @PostMapping("/single")
    public Result<Long> single(@RequestParam Long targetUserId) {
        return Result.ok(chatService.getOrCreateSingle(SecurityUtils.getUserId(), targetUserId));
    }

    /* ---- 已读回执 ---- */
    @PostMapping("/read-receipt")
    public Result<?> readReceipt(@RequestParam Long sessionId, @RequestParam Long lastMsgId) {
        chatService.markMessageRead(SecurityUtils.getUserId(), sessionId, lastMsgId);
        chatService.markRead(SecurityUtils.getUserId(), sessionId);
        return Result.ok();
    }

    @GetMapping("/message-readers")
    public Result<java.util.List<java.util.Map<String, Object>>> readers(@RequestParam Long messageId) {
        return Result.ok(chatService.getMessageReaders(messageId));
    }

    /* ---- 搜索 ---- */
    @GetMapping("/search")
    public Result<java.util.List<com.example.dingtalk.vo.MessageVO>> search(@RequestParam String keyword) {
        return Result.ok(chatService.searchMessages(SecurityUtils.getUserId(), keyword));
    }

    /* ---- 置顶 ---- */
    @PostMapping("/toggle-top")
    public Result<?> toggleTop(@RequestParam Long sessionId) {
        chatService.toggleTop(SecurityUtils.getUserId(), sessionId);
        return Result.ok();
    }

    /* ---- 免打扰 ---- */
    @PostMapping("/toggle-mute")
    public Result<?> toggleMute(@RequestParam Long sessionId) {
        chatService.toggleMute(SecurityUtils.getUserId(), sessionId);
        return Result.ok();
    }

    /* ---- 特别关注 ---- */
    @PostMapping("/toggle-star")
    public Result<?> toggleStar(@RequestParam Long sessionId) {
        chatService.toggleStar(SecurityUtils.getUserId(), sessionId);
        return Result.ok();
    }

    /* ---- AI助手 ---- */
    @PostMapping(value = "/ai-reply", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MessageVO> aiReplyStream(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getUserId();
        Long sessionId = Long.parseLong(body.get("sessionId").toString());
        String question = (String) body.get("content");
        // 返回流式 Flux
        return chatService.aiReply(userId, sessionId, question);
    }

    /* ---- 群聊管理 ---- */
    @PostMapping("/group")
    public Result<Long> createGroup(@Valid @RequestBody CreateGroupDTO dto) {
        return Result.ok(chatService.createGroup(SecurityUtils.getUserId(), dto.getName(), dto.getMemberIds()));
    }

    @GetMapping("/group/{sessionId}")
    public Result<ChatSession> groupInfo(@PathVariable Long sessionId) {
        return Result.ok(chatService.getSession(sessionId));
    }

    @GetMapping("/group/{sessionId}/members")
    public Result<List<MemberVO>> members(@PathVariable Long sessionId) {
        return Result.ok(chatService.getMembers(sessionId));
    }

    @PostMapping("/group/{sessionId}/members")
    public Result<?> addMembers(@PathVariable Long sessionId, @RequestBody Map<String, List<Long>> body) {
        chatService.addMembers(SecurityUtils.getUserId(), sessionId, body.get("userIds"));
        return Result.ok();
    }

    @DeleteMapping("/group/{sessionId}/members/{userId}")
    public Result<?> removeMember(@PathVariable Long sessionId, @PathVariable Long userId) {
        chatService.removeMember(SecurityUtils.getUserId(), sessionId, userId);
        return Result.ok();
    }

    @PostMapping("/group/{sessionId}/quit")
    public Result<?> quit(@PathVariable Long sessionId) {
        chatService.quitGroup(SecurityUtils.getUserId(), sessionId);
        return Result.ok();
    }

    @PutMapping("/group/{sessionId}/notice")
    public Result<?> notice(@PathVariable Long sessionId, @RequestBody Map<String, String> body) {
        chatService.updateNotice(SecurityUtils.getUserId(), sessionId, body.get("notice"));
        return Result.ok();
    }
}
