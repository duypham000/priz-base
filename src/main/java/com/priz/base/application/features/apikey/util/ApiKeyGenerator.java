package com.priz.base.application.features.apikey.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class ApiKeyGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    /** Tạo public code (prefix) để tra cứu nhanh, 12 ký tự Base64URL. */
    public String generateCode() {
        byte[] bytes = new byte[9];
        SECURE_RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }

    /** Tạo raw key 32 bytes. Trả về Base64URL string gửi cho client (chỉ lần đầu). */
    public String generateRawKey() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }

    /** Tạo HMAC secret 64 bytes. Lưu sau khi encrypt. */
    public String generateSecret() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }
}
