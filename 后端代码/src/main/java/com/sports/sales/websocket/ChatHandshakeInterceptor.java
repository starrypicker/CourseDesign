package com.sports.sales.websocket;

import com.sports.sales.util.JwtUtil;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器：验证 JWT Token，将用户信息存入 attributes
 */
@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    public ChatHandshakeInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            if (token == null || !jwtUtil.validateToken(token)) {
                return false;
            }
            String username = jwtUtil.getUsernameFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);
            String customerCode = jwtUtil.getCustomerCodeFromToken(token);

            attributes.put("username", username);
            attributes.put("role", role);
            attributes.put("customerCode", customerCode);

            // session key: admin -> "admin_username", customer -> customerCode
            if ("admin".equals(role)) {
                attributes.put("sessionKey", "admin_" + username);
            } else {
                attributes.put("sessionKey", customerCode != null ? customerCode : username);
            }
            return true;
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
    }
}
