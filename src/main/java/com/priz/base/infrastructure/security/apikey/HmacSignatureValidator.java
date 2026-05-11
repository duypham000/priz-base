package com.priz.base.infrastructure.security.apikey;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class HmacSignatureValidator {

    private static final String ALGORITHM = "HmacSHA256";

    /**
     * Tính HMAC-SHA256 của payload rồi so sánh với signature gửi lên.
     * Dùng constant-time comparison để chống timing attack.
     */
    public boolean verify(String payload, String signature, String secretKey) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secretKey.getBytes(), ALGORITHM));
            byte[] expected = mac.doFinal(payload.getBytes());
            byte[] actual = Base64.getDecoder().decode(signature);
            return constantTimeEquals(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    public String sign(String payload, String secretKey) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secretKey.getBytes(), ALGORITHM));
            return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC signing failed", e);
        }
    }

    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
