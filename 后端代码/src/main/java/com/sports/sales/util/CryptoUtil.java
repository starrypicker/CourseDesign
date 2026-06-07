package com.sports.sales.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
public class CryptoUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    @Value("${crypto.secret-key}")
    private String secretKey;

    @Value("${crypto.iv-key}")
    private String ivKey;

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            byte[] keyBytes = getKeyBytes(secretKey, 32);
            byte[] ivBytes = getKeyBytes(ivKey, 16);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
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
            byte[] keyBytes = getKeyBytes(secretKey, 32);
            byte[] ivBytes = getKeyBytes(ivKey, 16);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("解密失败，返回原文: {}", cipherText);
            return cipherText;
        }
    }

    /**
     * 获取指定长度的密钥字节数组
     * 如果密钥不足，用0填充；如果超出，截取前 targetLen 字节
     */
    private byte[] getKeyBytes(String key, int targetLen) {
        byte[] result = new byte[targetLen];
        if (key != null) {
            byte[] srcBytes = key.getBytes(StandardCharsets.UTF_8);
            int copyLen = Math.min(srcBytes.length, targetLen);
            System.arraycopy(srcBytes, 0, result, 0, copyLen);
        }
        return result;
    }
}
