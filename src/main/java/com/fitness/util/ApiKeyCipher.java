package com.fitness.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * API Key 加解密工具
 * <p>
 * 使用 AES/GCM 加密后存入数据库，避免密钥明文落库。
 * 密钥由 jwt.secret 派生，保证同一部署环境下可解密。
 */
@Component
public class ApiKeyCipher {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BIT = 128;

    private final SecretKeySpec keySpec;

    public ApiKeyCipher(@Value("${jwt.secret:fitness-secret-key}") String secret) {
        // 取 SHA-256 摘要的前 16 字节作为 AES-128 密钥
        byte[] keyBytes = new byte[16];
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            System.arraycopy(digest, 0, keyBytes, 0, 16);
        } catch (Exception e) {
            // 极端情况下的兜底：直接截取
            byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(raw, 0, keyBytes, 0, Math.min(raw.length, 16));
        }
        this.keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * 加密
     *
     * @return Base64(IV + 密文)，失败返回 null
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解密
     *
     * @return 明文；失败返回 null
     */
    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encrypted);
            if (combined.length <= IV_LENGTH) {
                return null;
            }

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);

            byte[] cipherText = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 生成掩码展示（sk-abc***xyz），用于前端回显
     */
    public String mask(String plainKey) {
        if (plainKey == null || plainKey.isEmpty()) {
            return "";
        }
        if (plainKey.length() <= 8) {
            return "****";
        }
        return plainKey.substring(0, 4) + "****" + plainKey.substring(plainKey.length() - 4);
    }
}