package com.example.dingtalk.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** 在线状态管理 (按 WS session 维护, 并同步留痕到数据库) */
@Service
public class OnlineService {

    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionUsers = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final UserPreferenceService userPreferenceService;

    public OnlineService(SimpMessagingTemplate messagingTemplate, UserPreferenceService userPreferenceService) {
        this.messagingTemplate = messagingTemplate;
        this.userPreferenceService = userPreferenceService;
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
            broadcast(userId, true);
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
                    broadcast(finalResolvedUserId, false);
                }
            }
            return;
        }

        if (resolvedUserId != null && userSessions.remove(resolvedUserId) != null) {
            Long finalResolvedUserId = resolvedUserId;
            sessionUsers.entrySet().removeIf(entry -> finalResolvedUserId.equals(entry.getValue()));
            userPreferenceService.markOffline(resolvedUserId);
            broadcast(resolvedUserId, false);
        }
    }

    public boolean isOnline(Long userId) {
        return userSessions.containsKey(userId);
    }

    public Set<Long> getOnlineUserIds() {
        return Set.copyOf(new HashSet<>(userSessions.keySet()));
    }

    private void broadcast(Long userId, boolean online) {
        Map<String, Object> payload = Map.of("userId", userId, "online", online);
        messagingTemplate.convertAndSend("/topic/online-status", payload);
    }
}
