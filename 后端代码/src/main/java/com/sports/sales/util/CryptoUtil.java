package com.sports.sales.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-CBC 加解密工具类
 * 格式：Base64(随机IV(16字节) + 密文)
 * 每次加密使用随机IV，安全性高于固定IV
 */
@Slf4j
@Component
public class CryptoUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;

    @Value("${crypto.secret-key}")
    private String secretKey;

    private final SecureRandom secureRandom = new SecureRandom();

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            if (secretKey.length() < 32) {
                throw new RuntimeException("加密密钥长度不足32字节，当前长度: " + secretKey.length());
            }
            SecretKeySpec keySpec = new SecretKeySpec(
                    secretKey.substring(0, 32).getBytes(StandardCharsets.UTF_8), ALGORITHM);

            // 生成随机IV
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 将IV拼接在密文前面：IV(16) + 密文
            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("加密失败", e);
            throw new RuntimeException("加密失败", e);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            if (secretKey.length() < 32) {
                throw new RuntimeException("加密密钥长度不足32字节，当前长度: " + secretKey.length());
            }
            SecretKeySpec keySpec = new SecretKeySpec(
                    secretKey.substring(0, 32).getBytes(StandardCharsets.UTF_8), ALGORITHM);

            // 尝试 Base64 解码，失败则说明是明文，直接返回
            byte[] combined;
            try {
                combined = Base64.getDecoder().decode(cipherText);
            } catch (IllegalArgumentException e) {
                // 不是合法的 Base64 编码，说明是明文数据，直接返回
                return cipherText;
            }

            if (combined.length < IV_LENGTH) {
                // 数据太短，可能是旧格式的明文数据
                return cipherText;
            }

            // 分离IV和密文
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 解密失败（密钥不匹配、padding错误等），说明是明文或旧格式数据
            log.debug("解密失败，返回原文: {}", e.getMessage());
            return cipherText;
        }
    }
}
