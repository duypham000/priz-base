package com.priz.base.application.features.apikey.impl;

import com.priz.base.application.features.apikey.ApiKeyAuthService;
import com.priz.base.application.features.apikey.ApiKeyPermissionService;
import com.priz.base.application.features.apikey.dto.ApiKeyAuthResult;
import com.priz.base.domain.mysql_priz_base.model.ApiKeyCredentialsModel;
import com.priz.base.domain.mysql_priz_base.repository.ApiKeyCredentialsRepository;
import com.priz.base.infrastructure.security.apikey.HmacSignatureValidator;
import com.priz.base.infrastructure.security.apikey.IpWhitelistValidator;
import com.priz.base.infrastructure.security.apikey.SecretCipher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyAuthServiceImpl implements ApiKeyAuthService {

    private final ApiKeyCredentialsRepository credentialsRepository;
    private final ApiKeyPermissionService permissionService;
    private final PasswordEncoder passwordEncoder;
    private final SecretCipher secretCipher;
    private final HmacSignatureValidator hmacValidator;
    private final IpWhitelistValidator ipValidator;

    @Override
    public ApiKeyAuthResult validate(
            String rawApiKey, String clientIp,
            String signature, Long timestamp, long recvWindow,
            String method, String path, String queryStr, String body) {

        // 1. Parse format: {code}.{rawKey}
        int dot = rawApiKey.indexOf('.');
        if (dot <= 0 || dot == rawApiKey.length() - 1) {
            return ApiKeyAuthResult.failure("API_KEY_INVALID", "Invalid API key format");
        }
        String code = rawApiKey.substring(0, dot);
        String rawKey = rawApiKey.substring(dot + 1);

        // 2. Tìm theo code
        Optional<ApiKeyCredentialsModel> opt = credentialsRepository.findByCode(code);
        if (opt.isEmpty()) {
            return ApiKeyAuthResult.failure("API_KEY_INVALID", "API key not found");
        }
        ApiKeyCredentialsModel cred = opt.get();

        // 3. Verify raw key hash
        if (!passwordEncoder.matches(rawKey, cred.getKeyHash())) {
            return ApiKeyAuthResult.failure("API_KEY_INVALID", "API key invalid");
        }

        // 4. Check status
        if (!"ACTIVE".equals(cred.getStatus())) {
            return ApiKeyAuthResult.failure("API_KEY_INACTIVE", "API key is inactive");
        }

        // 5. Check expiration
        if (cred.getExpiresAt() != null && Instant.now().isAfter(cred.getExpiresAt())) {
            return ApiKeyAuthResult.failure("API_KEY_EXPIRED", "API key has expired");
        }

        // 6. Check IP whitelist
        if (cred.getAllowIps() != null && !cred.getAllowIps().isBlank()) {
            if (!ipValidator.isAllowed(clientIp, cred.getAllowIps())) {
                log.warn("API key {}: IP {} not in whitelist", code, clientIp);
                return ApiKeyAuthResult.failure("IP_NOT_ALLOWED", "Client IP not allowed");
            }
        }

        // 7. HMAC signature check (chỉ khi client gửi signature)
        boolean signatureVerified = false;
        if (signature != null && !signature.isBlank()) {
            if (cred.getSecretKeyEncrypted() == null) {
                return ApiKeyAuthResult.failure("SECRET_KEY_NOT_CONFIGURED",
                        "Signature provided but secret key not configured for this API key");
            }
            if (timestamp == null) {
                return ApiKeyAuthResult.failure("INVALID_TIMESTAMP", "Timestamp is required for signature");
            }
            long diff = Math.abs(System.currentTimeMillis() - timestamp);
            if (diff > recvWindow) {
                return ApiKeyAuthResult.failure("INVALID_TIMESTAMP",
                        "Timestamp out of recv_window (" + recvWindow + "ms)");
            }
            String secretKey = secretCipher.decrypt(cred.getSecretKeyEncrypted());
            String payload = method + "|" + path + "|" + (queryStr != null ? queryStr : "")
                    + "|" + (body != null ? body : "") + "|" + timestamp;
            if (!hmacValidator.verify(payload, signature, secretKey)) {
                return ApiKeyAuthResult.failure("INVALID_SIGNATURE", "HMAC signature mismatch");
            }
            signatureVerified = true;
        }

        // 8. Load permissions
        Set<String> permissions = permissionService.getEffectivePermissions(cred.getId());

        return ApiKeyAuthResult.builder()
                .valid(true)
                .apiKeyId(cred.getId())
                .code(code)
                .name(cred.getName())
                .permissions(permissions)
                .signatureVerified(signatureVerified)
                .build();
    }
}
