package com.example.dingtalk.service;

import com.example.dingtalk.entity.SysUser;
import com.example.dingtalk.mapper.UserMapper;
import com.example.dingtalk.vo.UserStatusVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** 在线状态管理 (按 WS session 维护, chatStatus 从数据库读取) */
@Service
@Slf4j
public class OnlineService {

    /** 用户手动设置状态的内存缓存（key: userId, value: chatStatus 1-4），避免每次查库 */
    private final Map<Long, Integer> chatStatusCache = new ConcurrentHashMap<>();
    /** 用户最近活跃时间缓存（key: userId, value: LocalDateTime） */
    private final Map<Long, LocalDateTime> lastActiveCache = new ConcurrentHashMap<>();

    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionUsers = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final UserPreferenceService userPreferenceService;
    private final UserMapper userMapper;

    public OnlineService(@Lazy SimpMessagingTemplate messagingTemplate,
                         UserPreferenceService userPreferenceService,
                         @Lazy UserMapper userMapper) {
        this.messagingTemplate = messagingTemplate;
        this.userPreferenceService = userPreferenceService;
        this.userMapper = userMapper;
    }

    /** 缓存用户手动设置状态，避免每次查库 */
    public void cacheChatStatus(Long userId, Integer chatStatus) {
        if (userId == null) return;
        if (chatStatus == null || chatStatus < 1 || chatStatus > 4) {
            chatStatusCache.put(userId, 1);
        } else {
            chatStatusCache.put(userId, chatStatus);
        }
    }

    /** 缓存用户最近活跃时间 */
    public void cacheLastActive(Long userId, LocalDateTime time) {
        if (userId != null && time != null) {
            lastActiveCache.put(userId, time);
        }
    }

    /**
     * 获取用户手动状态（chatStatus 1-4）。
     * 优先从内存缓存读取，缓存为空则查数据库，数据库为空则默认 1(在线)。
     */
    public Integer getChatStatus(Long userId) {
        if (userId == null) return 1;
        Integer cached = chatStatusCache.get(userId);
        if (cached != null) return cached;
        try {
            SysUser u = userMapper.selectById(userId);
            Integer dbStatus = (u != null) ? u.getChatStatus() : null;
            if (dbStatus == null || dbStatus < 1 || dbStatus > 4) dbStatus = 1;
            chatStatusCache.put(userId, dbStatus);
            return dbStatus;
        } catch (Exception e) {
            return 1;
        }
    }

    public void online(Long userId, String sessionId) {
        if (userId == null || sessionId == null || sessionId.isBlank()) return;
        sessionUsers.put(sessionId, userId);
        boolean becameOnline = userSessions.compute(userId, (key, sessions) -> {
            Set<String> next = sessions == null ? ConcurrentHashMap.newKeySet() : sessions;
            next.add(sessionId);
            return next;
        }).size() == 1;
        if (becameOnline) {
            userPreferenceService.markOnline(userId);
            LocalDateTime now = LocalDateTime.now();
            lastActiveCache.put(userId, now);
            broadcastStatus(userId, true, null);
        }
    }

    public void offline(Long userId, String sessionId) {
        Long resolvedUserId = userId;
        if (sessionId != null && !sessionId.isBlank()) {
            Long mappedUserId = sessionUsers.remove(sessionId);
            if (resolvedUserId == null) {
                resolvedUserId = mappedUserId;
            }
            if (resolvedUserId != null) {
                AtomicBoolean becameOffline = new AtomicBoolean(false);
                Long finalResolvedUserId = resolvedUserId;
                userSessions.compute(resolvedUserId, (key, sessions) -> {
                    if (sessions == null) return null;
                    sessions.remove(sessionId);
                    if (sessions.isEmpty()) {
                        becameOffline.set(true);
                        return null;
                    }
                    return sessions;
                });
                if (becameOffline.get()) {
                    userPreferenceService.markOffline(finalResolvedUserId);
                    LocalDateTime now = LocalDateTime.now();
                    lastActiveCache.put(finalResolvedUserId, now);
                    broadcastStatus(finalResolvedUserId, false, null);
                }
            }
            return;
        }

        if (resolvedUserId != null && userSessions.remove(resolvedUserId) != null) {
            Long finalResolvedUserId = resolvedUserId;
            sessionUsers.entrySet().removeIf(entry -> finalResolvedUserId.equals(entry.getValue()));
            userPreferenceService.markOffline(resolvedUserId);
            LocalDateTime now = LocalDateTime.now();
            lastActiveCache.put(finalResolvedUserId, now);
            broadcastStatus(finalResolvedUserId, false, null);
        }
    }

    /** 用户手动修改状态时，主动广播给所有在线用户 */
    public void broadcastManualStatusChange(Long userId, Integer chatStatus) {
        if (userId == null) return;
        cacheChatStatus(userId, chatStatus);
        boolean online = isOnline(userId);
        LocalDateTime now = LocalDateTime.now();
        lastActiveCache.put(userId, now);
        log.info("[WS] status change userId=" + userId + " chatStatus=" + chatStatus + " online=" + online + " broadcasting to /topic/online-status");
        broadcastStatus(userId, online, chatStatus);
    }

    public boolean isOnline(Long userId) {
        return userSessions.containsKey(userId);
    }

    public Set<Long> getOnlineUserIds() {
        return Set.copyOf(new HashSet<>(userSessions.keySet()));
    }

    /** 获取指定用户的完整状态（chatStatus 优先缓存，缺失则从数据库读取） */
    public UserStatusVO getUserStatus(Long userId) {
        if (userId == null) return null;
        boolean online = isOnline(userId);
        Integer chatStatus = getChatStatus(userId);
        return new UserStatusVO(userId, chatStatus, online, lastActiveCache.get(userId));
    }

    /**
     * 广播用户状态变更给所有在线用户（前端订阅 /topic/online-status）
     *
     * @param userId        用户ID
     * @param online        是否有活跃 WebSocket 连接
     * @param chatStatus    用户手动设置状态 (1-4)，传 null 则取数据库值
     */
    public void broadcastStatus(Long userId, boolean online, Integer chatStatus) {
        if (userId == null) return;
        Integer status = chatStatus;
        if (status == null) {
            status = getChatStatus(userId);
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("online", online);
        payload.put("chatStatus", status);
        LocalDateTime lastActive = lastActiveCache.get(userId);
        if (lastActive != null) {
            payload.put("lastActiveAt", lastActive);
        }
        try {
            messagingTemplate.convertAndSend("/topic/online-status", payload);
            log.info("[WS] broadcastStatus sent userId=" + userId + " online=" + online + " chatStatus=" + status);
        } catch (Exception e) {
            log.error("[WS] broadcastStatus failed userId=" + userId, e);
        }
    }

    /**
     * 强制用户下线（账号被停用/删除时调用）。
     * 1. 向该用户所有 WebSocket 连接推送「你已被管理员停用，请重新登录」消息
     * 2. 从在线状态缓存中移除该用户，并广播下线通知给其他在线用户
     */
    public void kickUser(Long userId, String reason) {
        if (userId == null) return;
        Set<String> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            log.info("[WS kick] userId=" + userId + " not online, skip websocket push");
            return;
        }
        log.info("[WS kick] userId=" + userId + " sessions=" + sessions.size() + " reason=" + reason);
        // 复制一份快照再遍历，避免并发修改异常
        Set<String> sessionSnapshot = new HashSet<>(sessions);
        Map<String, Object> kickPayload = new HashMap<>();
        kickPayload.put("type", "kick");
        kickPayload.put("userId", userId);
        kickPayload.put("reason", reason == null ? "账号已被管理员停用" : reason);
        for (String sessionId : sessionSnapshot) {
            try {
                // 向该用户的具体 session 推送踢人消息
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(userId), "/queue/kick", kickPayload);
                log.info("[WS kick] pushed kick to sessionId=" + sessionId + " userId=" + userId);
            } catch (Exception e) {
                log.error("[WS kick] push failed userId=" + userId + " sessionId=" + sessionId, e);
            }
            // 立即清理 session 映射，避免被误认为在线
            offline(userId, sessionId);
        }
        log.info("[WS kick] userId=" + userId + " forced offline completed");
    }
}
