package com.sports.sales.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sports.sales.entity.ChatMessage;
import com.sports.sales.service.ChatMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

/**
 * 客服聊天 WebSocket 处理器
 * 消息格式（JSON）: {"receiverCode": "xxx", "content": "消息内容"}
 * 顾客只能发给管理员，管理员可以发给任意顾客
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final WebSocketSessionManager sessionManager;
    private final ChatMessageService chatMessageService;

    public ChatWebSocketHandler(WebSocketSessionManager sessionManager, ChatMessageService chatMessageService) {
        this.sessionManager = sessionManager;
        this.chatMessageService = chatMessageService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionKey = (String) session.getAttributes().get("sessionKey");
        String username = (String) session.getAttributes().get("username");
        String role = (String) session.getAttributes().get("role");
        sessionManager.register(sessionKey, session);
        log.info("WebSocket 连接建立: key={}, user={}, role={}", sessionKey, username, role);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionKey = (String) session.getAttributes().get("sessionKey");
        String username = (String) session.getAttributes().get("username");
        String role = (String) session.getAttributes().get("role");
        String customerCode = (String) session.getAttributes().get("customerCode");

        // 解析消息
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
        String content = (String) payload.get("content");
        String receiverCode = (String) payload.get("receiverCode");

        if (content == null || content.trim().isEmpty()) return;

        ChatMessage chatMsg = new ChatMessage();
        chatMsg.setSenderCode("admin".equals(role) ? "admin" : (customerCode != null ? customerCode : username));
        chatMsg.setSenderName(username);
        chatMsg.setSenderRole(role);

        if ("admin".equals(role)) {
            // 管理员发给顾客
            chatMsg.setReceiverCode(receiverCode);
        } else {
            // 顾客发给所有管理员
            chatMsg.setReceiverCode("admin");
        }
        chatMsg.setContent(content);
        chatMsg.setCreateTime(java.time.LocalDateTime.now());

        // 保存到数据库
        chatMessageService.save(chatMsg);

        // 构造转发消息
        String forwardPayload = objectMapper.writeValueAsString(Map.of(
                "senderCode", chatMsg.getSenderCode(),
                "senderName", chatMsg.getSenderName(),
                "senderRole", chatMsg.getSenderRole(),
                "content", chatMsg.getContent(),
                "createTime", chatMsg.getCreateTime().toString()
        ));

        // 先回显给发送者自己
        try {
            session.sendMessage(new TextMessage(forwardPayload));
        } catch (Exception ignored) {}

        if ("admin".equals(role)) {
            // 管理员发给顾客：查找该顾客的会话
            if (receiverCode != null) {
                WebSocketSession targetSession = sessionManager.getSession(receiverCode);
                if (targetSession != null && targetSession.isOpen()) {
                    targetSession.sendMessage(new TextMessage(forwardPayload));
                }
            }
            // 同时广播给其他管理员
            Map<String, WebSocketSession> adminSessions = sessionManager.getAdminSessions();
            for (Map.Entry<String, WebSocketSession> entry : adminSessions.entrySet()) {
                if (entry.getValue().isOpen() && !entry.getKey().equals(sessionKey)) {
                    try {
                        entry.getValue().sendMessage(new TextMessage(forwardPayload));
                    } catch (Exception ignored) {}
                }
            }
        } else {
            // 顾客发给管理员：广播给所有管理员
            Map<String, WebSocketSession> adminSessions = sessionManager.getAdminSessions();
            for (Map.Entry<String, WebSocketSession> entry : adminSessions.entrySet()) {
                if (entry.getValue().isOpen()) {
                    try {
                        entry.getValue().sendMessage(new TextMessage(forwardPayload));
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionKey = (String) session.getAttributes().get("sessionKey");
        sessionManager.remove(sessionKey);
        log.info("WebSocket 连接关闭: key={}", sessionKey);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket 传输错误: {}", exception.getMessage());
    }
}
