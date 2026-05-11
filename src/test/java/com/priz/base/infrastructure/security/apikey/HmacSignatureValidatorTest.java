package com.priz.base.infrastructure.security.apikey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HmacSignatureValidatorTest {

    private HmacSignatureValidator validator;

    @BeforeEach
    void setUp() {
        validator = new HmacSignatureValidator();
    }

    @Test
    void verify_shouldReturnTrue_whenSignatureMatches() {
        String secretKey = "my-test-secret-key";
        String payload = "GET|/api/data|key=value||1715000000000";

        String signature = validator.sign(payload, secretKey);
        boolean result = validator.verify(payload, signature, secretKey);

        assertThat(result).isTrue();
    }

    @Test
    void verify_shouldReturnFalse_whenPayloadTampered() {
        String secretKey = "my-test-secret-key";
        String payload = "GET|/api/data|key=value||1715000000000";
        String tamperedPayload = "POST|/api/data|key=value||1715000000000";

        String signature = validator.sign(payload, secretKey);
        boolean result = validator.verify(tamperedPayload, signature, secretKey);

        assertThat(result).isFalse();
    }

    @Test
    void verify_shouldReturnFalse_whenWrongSecretKey() {
        String secretKey = "my-test-secret-key";
        String wrongKey = "wrong-secret-key!!!";
        String payload = "GET|/api/data|||1715000000000";

        String signature = validator.sign(payload, secretKey);
        boolean result = validator.verify(payload, signature, wrongKey);

        assertThat(result).isFalse();
    }

    @Test
    void verify_shouldReturnFalse_whenSignatureCorrupted() {
        String secretKey = "my-test-secret-key";
        String payload = "GET|/api/data|||1715000000000";

        boolean result = validator.verify(payload, "not-a-valid-base64!!!!", secretKey);

        assertThat(result).isFalse();
    }

    @Test
    void verify_shouldReturnFalse_whenSignatureEmpty() {
        String secretKey = "my-test-secret-key";
        String payload = "GET|/api/data|||1715000000000";

        String signature = validator.sign(payload, secretKey);
        // Truncate to wrong length
        String truncated = signature.substring(0, signature.length() / 2);

        boolean result = validator.verify(payload, truncated, secretKey);

        assertThat(result).isFalse();
    }

    @Test
    void sign_shouldProduceDifferentSignatures_forDifferentPayloads() {
        String secretKey = "my-test-secret-key";

        String sig1 = validator.sign("payload-one", secretKey);
        String sig2 = validator.sign("payload-two", secretKey);

        assertThat(sig1).isNotEqualTo(sig2);
    }

    @Test
    void sign_shouldProduceSameSignature_forSameInput() {
        String secretKey = "my-test-secret-key";
        String payload = "GET|/api/data||{\"key\":\"val\"}|1715000000000";

        String sig1 = validator.sign(payload, secretKey);
        String sig2 = validator.sign(payload, secretKey);

        assertThat(sig1).isEqualTo(sig2);
    }
}
