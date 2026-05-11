package com.priz.base.application.features.apikey.impl;

import com.priz.base.application.features.apikey.ApiKeyPermissionService;
import com.priz.base.application.features.apikey.dto.ApiKeyAuthResult;
import com.priz.base.domain.mysql_priz_base.model.ApiKeyCredentialsModel;
import com.priz.base.domain.mysql_priz_base.repository.ApiKeyCredentialsRepository;
import com.priz.base.infrastructure.security.apikey.HmacSignatureValidator;
import com.priz.base.infrastructure.security.apikey.IpWhitelistValidator;
import com.priz.base.infrastructure.security.apikey.SecretCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthServiceImplTest {

    @Mock
    private ApiKeyCredentialsRepository credentialsRepository;
    @Mock
    private ApiKeyPermissionService permissionService;
    @Mock
    private SecretCipher secretCipher;
    @Mock
    private HmacSignatureValidator hmacValidator;
    @Mock
    private IpWhitelistValidator ipValidator;

    private PasswordEncoder passwordEncoder;
    private ApiKeyAuthServiceImpl authService;

    private static final String RAW_KEY = "rawKeyValue";
    private static final String CODE = "testcode12";
    private static final String API_KEY = CODE + "." + RAW_KEY;
    private static final String CREDENTIALS_ID = "cred-id-1";
    private static final String CLIENT_IP = "10.0.0.1";

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new ApiKeyAuthServiceImpl(
                credentialsRepository, permissionService, passwordEncoder,
                secretCipher, hmacValidator, ipValidator);
    }

    // ---- Format validation ----

    @Test
    void validate_invalidFormat_noSeparator_returnsFailure() {
        ApiKeyAuthResult result = authService.validate(
                "nokeyformat", CLIENT_IP, null, null, 5000, "GET", "/", "", "");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("API_KEY_INVALID");
    }

    @Test
    void validate_invalidFormat_emptyCode_returnsFailure() {
        ApiKeyAuthResult result = authService.validate(
                ".rawKeyOnly", CLIENT_IP, null, null, 5000, "GET", "/", "", "");

        assertThat(result.isValid()).isFalse();
    }

    // ---- Key not found ----

    @Test
    void validate_codeNotFound_returnsFailure() {
        when(credentialsRepository.findByCode(CODE)).thenReturn(Optional.empty());

        ApiKeyAuthResult result = validate(null, null);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("API_KEY_INVALID");
    }

    // ---- Hash mismatch ----

    @Test
    void validate_wrongRawKey_returnsFailure() {
        ApiKeyCredentialsModel cred = buildActiveCred("wrongHash");
        when(credentialsRepository.findByCode(CODE)).thenReturn(Optional.of(cred));

        ApiKeyAuthResult result = authService.validate(
                CODE + ".wrongRawKey", CLIENT_IP, null, null, 5000, "GET", "/", "", "");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("API_KEY_INVALID");
    }

    // ---- Inactive status ----

    @Test
    void validate_inactiveKey_returnsFailure() {
        String hash = passwordEncoder.encode(RAW_KEY);
        ApiKeyCredentialsModel cred = ApiKeyCredentialsModel.builder()
                .code(CODE).keyHash(hash).status("INACTIVE").build();
        cred.setId(CREDENTIALS_ID);
        when(credentialsRepository.findByCode(CODE)).thenReturn(Optional.of(cred));

        ApiKeyAuthResult result = validate(null, null);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("API_KEY_INACTIVE");
    }

    // ---- Expired ----

    @Test
    void validate_expiredKey_returnsFailure() {
        String hash = passwordEncoder.encode(RAW_KEY);
        ApiKeyCredentialsModel cred = ApiKeyCredentialsModel.builder()
                .code(CODE).keyHash(hash).status("ACTIVE")
                .expiresAt(Instant.now().minusSeconds(3600))
                .build();
        cred.setId(CREDENTIALS_ID);
        when(credentialsRepository.findByCode(CODE)).thenReturn(Optional.of(cred));

        ApiKeyAuthResult result = validate(null, null);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("API_KEY_EXPIRED");
    }

    // ---- IP whitelist ----

    @Test
    void validate_ipNotAllowed_returnsFailure() {
        ApiKeyCredentialsModel cred = buildActiveCred(passwordEncoder.encode(RAW_KEY));
        cred.setAllowIps("192.168.1.0/24");
        when(credentialsRepository.findByCode(CODE)).thenReturn(Optional.of(cred));
        when(ipValidator.isAllowed(CLIENT_IP, "192.168.1.0/24")).thenReturn(false);

        ApiKeyAuthResult result = validate(null, null);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("IP_NOT_ALLOWED");
    }

    // ---- Happy path (no signature, no IP restriction) ----

    @Test
    void validate_validKeyNoRestrictions_returnsSuccess() {
        ApiKeyCredentialsModel cred = buildActiveCred(passwordEncoder.encode(RAW_KEY));
        when(credentialsRepository.findByCode(CODE)).thenReturn(Optional.of(cred));
        when(permissionService.getEffectivePermissions(CREDENTIALS_ID))
                .thenReturn(Set.of("read:base:file"));

        ApiKeyAuthResult result = validate(null, null);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getApiKeyId()).isEqualTo(CREDENTIALS_ID);
        assertThat(result.getPermissions()).containsExactly("read:base:file");
        assertThat(result.isSignatureVerified()).isFalse();
    }

    // ---- HMAC signature ----

    @Test
    void validate_signatureProvidedNoSecretConfigured_returnsFailure() {
        ApiKeyCredentialsModel cred = buildActiveCred(passwordEncoder.encode(RAW_KEY));
        cred.setSecretKeyEncrypted(null);
        when(credentialsRepository.findByCode(CODE)).thenReturn(Optional.of(cred));

        ApiKeyAuthResult result = validate("sig123", System.currentTimeMillis());

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("SECRET_KEY_NOT_CONFIGURED");
    }

    @Test
    void validate_signatureProvidedTimestampExpired_returnsFailure() {
        ApiKeyCredentialsModel cred = buildActiveCred(passwordEncoder.encode(RAW_KEY));
        cred.setSecretKeyEncrypted("encrypted-secret");
        when(credentialsRepository.findByCode(CODE)).thenReturn(Optional.of(cred));

        long oldTimestamp = System.currentTimeMillis() - 10_000; // 10 seconds ago
        ApiKeyAuthResult result = authService.validate(
                API_KEY, CLIENT_IP, "sig123", oldTimestamp, 5000,
                "GET", "/", "", "");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("INVALID_TIMESTAMP");
    }

    @Test
    void validate_signatureMismatch_returnsFailure() {
        ApiKeyCredentialsModel cred = buildActiveCred(passwordEncoder.encode(RAW_KEY));
        cred.setSecretKeyEncrypted("encrypted-secret");
        when(credentialsRepository.findByCode(CODE)).thenReturn(Optional.of(cred));
        when(secretCipher.decrypt("encrypted-secret")).thenReturn("plain-secret");
        when(hmacValidator.verify(any(), eq("bad-sig"), eq("plain-secret"))).thenReturn(false);

        long now = System.currentTimeMillis();
        ApiKeyAuthResult result = authService.validate(
                API_KEY, CLIENT_IP, "bad-sig", now, 5000, "GET", "/", "", "");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("INVALID_SIGNATURE");
    }

    @Test
    void validate_signatureValid_returnsSuccessWithSignatureVerified() {
        ApiKeyCredentialsModel cred = buildActiveCred(passwordEncoder.encode(RAW_KEY));
        cred.setSecretKeyEncrypted("encrypted-secret");
        when(credentialsRepository.findByCode(CODE)).thenReturn(Optional.of(cred));
        when(secretCipher.decrypt("encrypted-secret")).thenReturn("plain-secret");
        when(hmacValidator.verify(any(), eq("valid-sig"), eq("plain-secret"))).thenReturn(true);
        when(permissionService.getEffectivePermissions(CREDENTIALS_ID))
                .thenReturn(Set.of("read:base:file"));

        long now = System.currentTimeMillis();
        ApiKeyAuthResult result = authService.validate(
                API_KEY, CLIENT_IP, "valid-sig", now, 5000, "GET", "/", "", "");

        assertThat(result.isValid()).isTrue();
        assertThat(result.isSignatureVerified()).isTrue();
    }

    // ---- helpers ----

    private ApiKeyAuthResult validate(String signature, Long timestamp) {
        return authService.validate(
                API_KEY, CLIENT_IP, signature, timestamp, 5000, "GET", "/", "", "");
    }

    private ApiKeyCredentialsModel buildActiveCred(String keyHash) {
        ApiKeyCredentialsModel cred = ApiKeyCredentialsModel.builder()
                .code(CODE).keyHash(keyHash).status("ACTIVE").build();
        cred.setId(CREDENTIALS_ID);
        return cred;
    }
}
