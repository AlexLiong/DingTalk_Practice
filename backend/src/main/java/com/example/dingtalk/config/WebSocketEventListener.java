package com.example.dingtalk.config;

import com.example.dingtalk.service.OnlineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/** 监听 WebSocket 连接/断开, 维护在线状态 */
@Slf4j
@Component
public class WebSocketEventListener {

    private final OnlineService onlineService;

    public WebSocketEventListener(OnlineService onlineService) {
        this.onlineService = onlineService;
    }

    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (accessor.getUser() != null) {
            try {
                Long uid = Long.parseLong(accessor.getUser().getName());
                onlineService.online(uid, sessionId);
                log.debug("WS online: {}", uid);
            } catch (Exception ignored) { }
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Long uid = null;
        if (accessor.getUser() != null) {
            try {
                uid = Long.parseLong(accessor.getUser().getName());
            } catch (Exception ignored) { }
        }
        try {
            onlineService.offline(uid, sessionId);
            if (uid != null) {
                log.debug("WS offline: {}", uid);
            }
        } catch (Exception ignored) {
        }
    }
}
