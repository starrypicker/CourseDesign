package com.sports.sales.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret:SportsSalesJwtSecretKey2026ForTokenSigning!}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = new byte[32];
        byte[] srcBytes = secret.getBytes(StandardCharsets.UTF_8);
        int copyLen = Math.min(srcBytes.length, 32);
        System.arraycopy(srcBytes, 0, keyBytes, 0, copyLen);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成JWT Token
     */
    public String generateToken(Long userId, String username, String role, String customerCode) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        if (customerCode != null) {
            claims.put("customerCode", customerCode);
        }

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析Token获取Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 从Token中获取角色
     */
    public String getRoleFromToken(String token) {
        return parseToken(token).get("role", String.class);
    }

    /**
     * 从Token中获取顾客代码
     */
    public String getCustomerCodeFromToken(String token) {
        return parseToken(token).get("customerCode", String.class);
    }
}
