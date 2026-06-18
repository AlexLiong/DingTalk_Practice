package com.example.dingtalk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.common.BizException;
import com.example.dingtalk.dto.SendMessageDTO;
import com.example.dingtalk.entity.*;
import com.example.dingtalk.mapper.*;
import com.example.dingtalk.mapper.MessageReadMapper;
import com.example.dingtalk.vo.MemberVO;
import com.example.dingtalk.vo.MessageVO;
import com.example.dingtalk.vo.SessionVO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatSessionMapper sessionMapper;
    private final ChatSessionMemberMapper memberMapper;
    private final ChatMessageMapper messageMapper;
    private final UserMapper userMapper;
    private final MessageReadMapper messageReadMapper;
    private final ReactionMapper reactionMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final AiRagService aiRagService;

    public ChatService(ChatSessionMapper sessionMapper, ChatSessionMemberMapper memberMapper,
                       ChatMessageMapper messageMapper, UserMapper userMapper,
                       MessageReadMapper messageReadMapper,
                       ReactionMapper reactionMapper,
                       SimpMessagingTemplate messagingTemplate,
                       AiRagService aiRagService) {
        this.sessionMapper = sessionMapper;
        this.memberMapper = memberMapper;
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.messageReadMapper = messageReadMapper;
        this.reactionMapper = reactionMapper;
        this.messagingTemplate = messagingTemplate;
        this.aiRagService = aiRagService;
    }

    /* ===================== 会话 / 消息 ===================== */

    public List<SessionVO> getSessions(Long userId) {
        List<ChatSessionMember> myMembers = memberMapper.selectList(
                new LambdaQueryWrapper<ChatSessionMember>().eq(ChatSessionMember::getUserId, userId));
        List<SessionVO> list = new ArrayList<>();
        for (ChatSessionMember m : myMembers) {
            ChatSession session = sessionMapper.selectById(m.getSessionId());
            if (session == null) continue;
            SessionVO vo = new SessionVO();
            vo.setId(session.getId());
            vo.setType(session.getType());
            vo.setLastMsg(session.getLastMsg());
            vo.setLastTime(session.getLastTime());
            vo.setUnread(m.getUnread());
            vo.setIsTop(m.getIsTop());
            vo.setIsMute(m.getIsMute());
            vo.setIsStar(m.getIsStar());
            if (session.getType() == 2) {
                vo.setName(session.getName());
                vo.setAvatar(session.getAvatar());
            } else {
                ChatSessionMember other = memberMapper.selectOne(
                        new LambdaQueryWrapper<ChatSessionMember>()
                                .eq(ChatSessionMember::getSessionId, session.getId())
                                .ne(ChatSessionMember::getUserId, userId).last("limit 1"));
                if (other != null) {
                    SysUser target = userMapper.selectById(other.getUserId());
                    if (target != null) {
                        vo.setName(target.getNickname());
                        vo.setAvatar(target.getAvatar());
                        vo.setTargetUserId(target.getId());
                    }
                }
            }
            list.add(vo);
        }
        list.sort(Comparator
                .comparing((SessionVO s) -> s.getIsTop() != null && s.getIsTop() == 1 ? 0 : 1)
                .thenComparing(SessionVO::getLastTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    public List<MessageVO> getMessages(Long userId, Long sessionId) {
        checkMember(sessionId, userId);
        List<ChatMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreateTime).last("limit 100"));
        return messages.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Transactional
    public MessageVO sendMessage(Long userId, SendMessageDTO dto) {
        checkMember(dto.getSessionId(), userId);
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(dto.getSessionId());
        msg.setSenderId(userId);
        msg.setContentType(dto.getContentType() == null ? 1 : dto.getContentType());
        msg.setContent(dto.getContent());
        msg.setExtra(dto.getExtra());
        msg.setAtUserIds(dto.getAtUserIds());
        msg.setStatus(1);
        msg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);

        ChatSession session = sessionMapper.selectById(dto.getSessionId());
        session.setLastMsg(preview(msg));
        session.setLastTime(msg.getCreateTime());
        sessionMapper.updateById(session);

        List<ChatSessionMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<ChatSessionMember>()
                        .eq(ChatSessionMember::getSessionId, dto.getSessionId()));
        MessageVO vo = toVO(msg);
        for (ChatSessionMember m : members) {
            if (!m.getUserId().equals(userId)) {
                m.setUnread((m.getUnread() == null ? 0 : m.getUnread()) + 1);
                memberMapper.updateById(m);
            }
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(m.getUserId()), "/queue/message", vo);
            pushSessionUnread(m.getUserId());
        }
        return vo;
    }

    /** 撤回消息 (本人 或 群主) */
    @Transactional
    public void recall(Long userId, Long messageId) {
        ChatMessage msg = messageMapper.selectById(messageId);
        if (msg == null) throw new BizException("消息不存在");
        boolean isOwner = false;
        ChatSession session = sessionMapper.selectById(msg.getSessionId());
        if (session != null && session.getType() == 2) {
            ChatSessionMember me = memberMapper.selectOne(new LambdaQueryWrapper<ChatSessionMember>()
                    .eq(ChatSessionMember::getSessionId, msg.getSessionId())
                    .eq(ChatSessionMember::getUserId, userId));
            isOwner = me != null && me.getMemberRole() != null && me.getMemberRole() == 1;
        }
        if (!msg.getSenderId().equals(userId) && !isOwner) {
            throw new BizException("只能撤回自己的消息");
        }
        msg.setStatus(0);
        messageMapper.updateById(msg);
        // 通知所有成员
        MessageVO vo = toVO(msg);
        List<ChatSessionMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<ChatSessionMember>()
                        .eq(ChatSessionMember::getSessionId, msg.getSessionId()));
        for (ChatSessionMember m : members) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(m.getUserId()), "/queue/message", vo);
        }
    }

    public void markRead(Long userId, Long sessionId) {
        ChatSessionMember m = memberMapper.selectOne(
                new LambdaQueryWrapper<ChatSessionMember>()
                        .eq(ChatSessionMember::getSessionId, sessionId)
                        .eq(ChatSessionMember::getUserId, userId));
        if (m != null && m.getUnread() != null && m.getUnread() > 0) {
            m.setUnread(0);
            memberMapper.updateById(m);
            pushSessionUnread(userId);
        }
    }

    /** 已读回执: 记录用户读了某条消息, 并通过WS通知发送者 */
    public void markMessageRead(Long userId, Long sessionId, Long lastMsgId) {
        // 批量标记到 lastMsgId 为止的消息
        List<ChatMessage> unreadMsgs = messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .le(ChatMessage::getId, lastMsgId)
                        .ne(ChatMessage::getSenderId, userId));
        for (ChatMessage msg : unreadMsgs) {
            Long exists = messageReadMapper.selectCount(new LambdaQueryWrapper<ChatMessageRead>()
                    .eq(ChatMessageRead::getMessageId, msg.getId())
                    .eq(ChatMessageRead::getUserId, userId));
            if (exists == 0) {
                ChatMessageRead r = new ChatMessageRead();
                r.setMessageId(msg.getId());
                r.setSessionId(sessionId);
                r.setUserId(userId);
                r.setReadTime(LocalDateTime.now());
                messageReadMapper.insert(r);
            }
        }
        // 通知该会话所有成员刷新已读状态
        Map<String, Object> receipt = new HashMap<>();
        receipt.put("type", "read_receipt");
        receipt.put("sessionId", sessionId);
        receipt.put("readerId", userId);
        receipt.put("lastMsgId", lastMsgId);
        SysUser reader = userMapper.selectById(userId);
        receipt.put("readerName", reader != null ? reader.getNickname() : "");
        List<ChatSessionMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<ChatSessionMember>()
                        .eq(ChatSessionMember::getSessionId, sessionId));
        for (ChatSessionMember m : members) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(m.getUserId()), "/queue/receipt", receipt);
        }
    }

    /** 查询某条消息的已读用户 */
    public List<Map<String, Object>> getMessageReaders(Long messageId) {
        List<ChatMessageRead> reads = messageReadMapper.selectList(
                new LambdaQueryWrapper<ChatMessageRead>()
                        .eq(ChatMessageRead::getMessageId, messageId)
                        .orderByAsc(ChatMessageRead::getReadTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessageRead r : reads) {
            SysUser u = userMapper.selectById(r.getUserId());
            Map<String, Object> m = new HashMap<>();
            m.put("userId", r.getUserId());
            m.put("nickname", u != null ? u.getNickname() : "");
            m.put("readTime", r.getReadTime());
            result.add(m);
        }
        return result;
    }

    /** 全局搜索: 搜消息 */
    public List<MessageVO> searchMessages(Long userId, String keyword) {
        // 取我参与的所有会话
        List<ChatSessionMember> mine = memberMapper.selectList(
                new LambdaQueryWrapper<ChatSessionMember>().eq(ChatSessionMember::getUserId, userId));
        if (mine.isEmpty()) return new ArrayList<>();
        List<Long> sessionIds = mine.stream().map(ChatSessionMember::getSessionId).collect(Collectors.toList());
        List<ChatMessage> msgs = messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .in(ChatMessage::getSessionId, sessionIds)
                        .like(ChatMessage::getContent, keyword)
                        .eq(ChatMessage::getStatus, 1)
                        .orderByDesc(ChatMessage::getCreateTime)
                        .last("limit 50"));
        return msgs.stream().map(this::toVO).collect(Collectors.toList());
    }

    /** 置顶/取消置顶 */
    public void toggleTop(Long userId, Long sessionId) {
        ChatSessionMember m = memberMapper.selectOne(
                new LambdaQueryWrapper<ChatSessionMember>()
                        .eq(ChatSessionMember::getSessionId, sessionId)
                        .eq(ChatSessionMember::getUserId, userId));
        if (m != null) {
            m.setIsTop(m.getIsTop() == null || m.getIsTop() == 0 ? 1 : 0);
            memberMapper.updateById(m);
        }
    }

    /** 切换免打扰 */
    public void toggleMute(Long userId, Long sessionId) {
        ChatSessionMember m = memberMapper.selectOne(
                new LambdaQueryWrapper<ChatSessionMember>()
                        .eq(ChatSessionMember::getSessionId, sessionId)
                        .eq(ChatSessionMember::getUserId, userId));
        if (m != null) {
            m.setIsMute(m.getIsMute() == null || m.getIsMute() == 0 ? 1 : 0);
            memberMapper.updateById(m);
        }
    }

    /** 切换特别关注 */
    public void toggleStar(Long userId, Long sessionId) {
        ChatSessionMember m = memberMapper.selectOne(
                new LambdaQueryWrapper<ChatSessionMember>()
                        .eq(ChatSessionMember::getSessionId, sessionId)
                        .eq(ChatSessionMember::getUserId, userId));
        if (m != null) {
            m.setIsStar(m.getIsStar() == null || m.getIsStar() == 0 ? 1 : 0);
            memberMapper.updateById(m);
        }
    }

    /** AI 助手自动回复 */
    @Transactional
    public MessageVO aiReply(Long userId, Long sessionId, String question) {
        ChatSession session = requireAiSession(sessionId, userId);

        // 先保存用户的问题消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setSenderId(userId);
        userMsg.setContentType(1);
        userMsg.setContent(question);
        userMsg.setStatus(1);
        userMsg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(userMsg);

        // 生成AI回复
        String reply = aiRagService.answer(userId, question);
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(sessionId);
        aiMsg.setSenderId(0L);   // 0=系统/AI
        aiMsg.setContentType(1);
        aiMsg.setContent(reply);
        aiMsg.setStatus(1);
        aiMsg.setCreateTime(LocalDateTime.now().plusSeconds(1));
        messageMapper.insert(aiMsg);

        // 更新会话最后消息
        session.setLastMsg(reply.length() > 50 ? reply.substring(0, 50) : reply);
        session.setLastTime(aiMsg.getCreateTime());
        sessionMapper.updateById(session);

        // 通过WS推送AI回复
        MessageVO vo = new MessageVO();
        vo.setId(aiMsg.getId());
        vo.setSessionId(sessionId);
        vo.setSenderId(0L);
        vo.setSenderName("AI 助手");
        vo.setContentType(1);
        vo.setContent(reply);
        vo.setStatus(1);
        vo.setCreateTime(aiMsg.getCreateTime());
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/message", vo);

        return vo;
    }

    /* ===================== 单聊 ===================== */

    @Transactional
    public Long getOrCreateSingle(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) throw new BizException("不能和自己聊天");
        List<ChatSessionMember> mine = memberMapper.selectList(
                new LambdaQueryWrapper<ChatSessionMember>().eq(ChatSessionMember::getUserId, userId));
        for (ChatSessionMember m : mine) {
            ChatSession s = sessionMapper.selectById(m.getSessionId());
            if (s != null && s.getType() == 1) {
                Long cnt = memberMapper.selectCount(new LambdaQueryWrapper<ChatSessionMember>()
                        .eq(ChatSessionMember::getSessionId, s.getId())
                        .eq(ChatSessionMember::getUserId, targetUserId));
                if (cnt > 0) return s.getId();
            }
        }
        ChatSession session = new ChatSession();
        session.setType(1);
        session.setCreateTime(LocalDateTime.now());
        session.setLastTime(LocalDateTime.now());
        sessionMapper.insert(session);
        addMember(session.getId(), userId, 2);
        addMember(session.getId(), targetUserId, 2);
        return session.getId();
    }

    /* ===================== 群聊管理 ===================== */

    @Transactional
    public Long createGroup(Long ownerId, String name, List<Long> memberIds) {
        Set<Long> ids = new LinkedHashSet<>(memberIds);
        ids.add(ownerId);
        SysUser owner = userMapper.selectById(ownerId);
        String groupName = (name == null || name.isBlank())
                ? buildDefaultName(ids) : name;
        ChatSession session = new ChatSession();
        session.setType(2);
        session.setName(groupName);
        session.setOwnerId(ownerId);
        session.setCreateTime(LocalDateTime.now());
        session.setLastTime(LocalDateTime.now());
        sessionMapper.insert(session);
        for (Long uid : ids) {
            addMember(session.getId(), uid, uid.equals(ownerId) ? 1 : 2);
        }
        systemMessage(session.getId(), (owner != null ? owner.getNickname() : "有人") + " 创建了群聊");
        return session.getId();
    }

    @Transactional
    public void addMembers(Long operatorId, Long sessionId, List<Long> userIds) {
        ChatSession session = mustGroup(sessionId);
        SysUser op = userMapper.selectById(operatorId);
        List<String> added = new ArrayList<>();
        for (Long uid : userIds) {
            Long cnt = memberMapper.selectCount(new LambdaQueryWrapper<ChatSessionMember>()
                    .eq(ChatSessionMember::getSessionId, sessionId)
                    .eq(ChatSessionMember::getUserId, uid));
            if (cnt == 0) {
                addMember(sessionId, uid, 2);
                SysUser u = userMapper.selectById(uid);
                if (u != null) added.add(u.getNickname());
            }
        }
        if (!added.isEmpty()) {
            systemMessage(sessionId, (op != null ? op.getNickname() : "管理员")
                    + " 邀请 " + String.join("、", added) + " 加入群聊");
        }
    }

    @Transactional
    public void removeMember(Long operatorId, Long sessionId, Long userId) {
        ChatSession session = mustGroup(sessionId);
        if (!session.getOwnerId().equals(operatorId)) throw new BizException("只有群主可以移除成员");
        if (userId.equals(session.getOwnerId())) throw new BizException("不能移除群主");
        memberMapper.delete(new LambdaQueryWrapper<ChatSessionMember>()
                .eq(ChatSessionMember::getSessionId, sessionId)
                .eq(ChatSessionMember::getUserId, userId));
        SysUser u = userMapper.selectById(userId);
        systemMessage(sessionId, (u != null ? u.getNickname() : "成员") + " 被移出群聊");
    }

    @Transactional
    public void quitGroup(Long userId, Long sessionId) {
        ChatSession session = mustGroup(sessionId);
        if (session.getOwnerId().equals(userId)) throw new BizException("群主不能退群, 请先解散或转让");
        memberMapper.delete(new LambdaQueryWrapper<ChatSessionMember>()
                .eq(ChatSessionMember::getSessionId, sessionId)
                .eq(ChatSessionMember::getUserId, userId));
        SysUser u = userMapper.selectById(userId);
        systemMessage(sessionId, (u != null ? u.getNickname() : "成员") + " 退出了群聊");
    }

    @Transactional
    public void updateNotice(Long operatorId, Long sessionId, String notice) {
        ChatSession session = mustGroup(sessionId);
        if (!session.getOwnerId().equals(operatorId)) throw new BizException("只有群主可以修改群公告");
        session.setNotice(notice);
        sessionMapper.updateById(session);
        systemMessage(sessionId, "群公告已更新: " + notice);
    }

    public List<MemberVO> getMembers(Long sessionId) {
        List<ChatSessionMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<ChatSessionMember>()
                        .eq(ChatSessionMember::getSessionId, sessionId)
                        .orderByAsc(ChatSessionMember::getMemberRole));
        List<MemberVO> list = new ArrayList<>();
        for (ChatSessionMember m : members) {
            SysUser u = userMapper.selectById(m.getUserId());
            if (u == null) continue;
            MemberVO vo = new MemberVO();
            vo.setUserId(u.getId());
            vo.setNickname(u.getNickname());
            vo.setAvatar(u.getAvatar());
            vo.setJobTitle(u.getJobTitle());
            vo.setDeptName(u.getDeptName());
            vo.setMemberRole(m.getMemberRole());
            list.add(vo);
        }
        return list;
    }

    public ChatSession getSession(Long sessionId) {
        return sessionMapper.selectById(sessionId);
    }

    /* ===================== 私有工具 ===================== */

    private void systemMessage(Long sessionId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setSenderId(0L);
        msg.setContentType(4);
        msg.setContent(content);
        msg.setStatus(1);
        msg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);
        ChatSession session = sessionMapper.selectById(sessionId);
        session.setLastMsg(content);
        session.setLastTime(msg.getCreateTime());
        sessionMapper.updateById(session);
        MessageVO vo = toVO(msg);
        List<ChatSessionMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<ChatSessionMember>().eq(ChatSessionMember::getSessionId, sessionId));
        for (ChatSessionMember m : members) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(m.getUserId()), "/queue/message", vo);
        }
    }

    private String buildDefaultName(Set<Long> ids) {
        List<String> names = new ArrayList<>();
        for (Long id : ids) {
            SysUser u = userMapper.selectById(id);
            if (u != null) names.add(u.getNickname());
            if (names.size() >= 3) break;
        }
        return String.join("、", names) + " 等" + ids.size() + "人";
    }

    private ChatSession mustGroup(Long sessionId) {
        ChatSession s = sessionMapper.selectById(sessionId);
        if (s == null || s.getType() != 2) throw new BizException("群聊不存在");
        return s;
    }

    private void addMember(Long sessionId, Long userId, int role) {
        ChatSessionMember m = new ChatSessionMember();
        m.setSessionId(sessionId);
        m.setUserId(userId);
        m.setMemberRole(role);
        m.setUnread(0);
        m.setJoinTime(LocalDateTime.now());
        memberMapper.insert(m);
    }

    private MessageVO toVO(ChatMessage msg) {
        MessageVO vo = new MessageVO();
        vo.setId(msg.getId());
        vo.setSessionId(msg.getSessionId());
        vo.setSenderId(msg.getSenderId());
        vo.setContentType(msg.getContentType());
        vo.setContent(msg.getContent());
        vo.setExtra(msg.getExtra());
        vo.setAtUserIds(msg.getAtUserIds());
        vo.setStatus(msg.getStatus());
        vo.setCreateTime(msg.getCreateTime());
        if (msg.getSenderId() != null && msg.getSenderId() != 0L) {
            SysUser sender = userMapper.selectById(msg.getSenderId());
            if (sender != null) {
                vo.setSenderName(sender.getNickname());
                vo.setSenderAvatar(sender.getAvatar());
            }
        }
        // 已读人数 (从 chat_message_read 查)
        long rc = messageReadMapper.selectCount(new LambdaQueryWrapper<ChatMessageRead>()
                .eq(ChatMessageRead::getMessageId, msg.getId()));
        vo.setReadCount((int) rc);
        // 加载消息反应
        List<ChatMessageReaction> reactions = reactionMapper.selectList(
                new LambdaQueryWrapper<ChatMessageReaction>()
                        .eq(ChatMessageReaction::getMessageId, msg.getId()));
        if (!reactions.isEmpty()) {
            Map<String, List<ChatMessageReaction>> grouped = reactions.stream()
                    .collect(java.util.stream.Collectors.groupingBy(ChatMessageReaction::getEmoji));
            List<Map<String, Object>> reactionList = new ArrayList<>();
            for (Map.Entry<String, List<ChatMessageReaction>> entry : grouped.entrySet()) {
                Map<String, Object> item = new HashMap<>();
                item.put("emoji", entry.getKey());
                item.put("count", entry.getValue().size());
                item.put("userIds", entry.getValue().stream().map(ChatMessageReaction::getUserId).collect(java.util.stream.Collectors.toList()));
                reactionList.add(item);
            }
            vo.setReactions(reactionList);
        }
        return vo;
    }

    private void checkMember(Long sessionId, Long userId) {
        Long count = memberMapper.selectCount(new LambdaQueryWrapper<ChatSessionMember>()
                .eq(ChatSessionMember::getSessionId, sessionId)
                .eq(ChatSessionMember::getUserId, userId));
        if (count == 0) throw new BizException("无权访问该会话");
    }

    private ChatSession requireAiSession(Long sessionId, Long userId) {
        checkMember(sessionId, userId);
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException("会话不存在");
        }
        boolean isAiSession = session.getType() != null
                && session.getType() == 2
                && "AI 助手".equals(session.getName());
        if (!isAiSession) {
            throw new BizException("该会话不支持 AI 助手");
        }
        return session;
    }

    private String preview(ChatMessage msg) {
        if (msg.getContentType() == 2) return "[图片]";
        if (msg.getContentType() == 3) return "[文件]";
        if (msg.getContentType() == 5) return "[视频]";
        if (msg.getContentType() == 6) return "[链接] " + (msg.getContent() == null ? "" : msg.getContent());
        String content = msg.getContent();
        if (content == null) return "";
        return content.length() > 50 ? content.substring(0, 50) : content;
    }

    private void pushSessionUnread(Long userId) {
        if (userId == null) {
            return;
        }
        int unreadTotal = memberMapper.selectList(
                new LambdaQueryWrapper<ChatSessionMember>()
                        .eq(ChatSessionMember::getUserId, userId)
        ).stream().mapToInt(member -> member.getUnread() == null ? 0 : member.getUnread()).sum();
        Map<String, Object> payload = new HashMap<>();
        payload.put("messageUnread", unreadTotal);
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/session-unread", payload);
    }
}
