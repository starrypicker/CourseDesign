package com.sports.sales.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话管理器：维护用户与会话的映射关系
 */
@Component
public class WebSocketSessionManager {

    /** userCode -> WebSocketSession */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(String userCode, WebSocketSession session) {
        // 如果已有旧会话，先关闭
        WebSocketSession old = sessions.get(userCode);
        if (old != null && old.isOpen()) {
            try { old.close(); } catch (Exception ignored) {}
        }
        sessions.put(userCode, session);
    }

    public void remove(String userCode) {
        sessions.remove(userCode);
    }

    public WebSocketSession getSession(String userCode) {
        return sessions.get(userCode);
    }

    /**
     * 获取所有管理员的会话（以 'admin_' 为前缀的 key）
     */
    public Map<String, WebSocketSession> getAdminSessions() {
        Map<String, WebSocketSession> result = new ConcurrentHashMap<>();
        sessions.forEach((key, session) -> {
            if (key.startsWith("admin_") && session.isOpen()) {
                result.put(key, session);
            }
        });
        return result;
    }

    public int getSessionCount() {
        return sessions.size();
    }
}
