package com.priz.base.application.features.apikey.impl;

import com.priz.base.application.features.apikey.ApiKeyService;
import com.priz.base.application.features.apikey.dto.CreateApiKeyRequest;
import com.priz.base.application.features.apikey.dto.CreateApiKeyResponse;
import com.priz.base.application.features.apikey.util.ApiKeyGenerator;
import com.priz.base.domain.mysql_priz_base.model.ApiKeyCredentialsModel;
import com.priz.base.domain.mysql_priz_base.repository.ApiKeyCredentialsRepository;
import com.priz.base.infrastructure.security.apikey.SecretCipher;
import com.priz.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyCredentialsRepository credentialsRepository;
    private final ApiKeyGenerator apiKeyGenerator;
    private final PasswordEncoder passwordEncoder;
    private final SecretCipher secretCipher;

    @Override
    @Transactional
    public CreateApiKeyResponse create(CreateApiKeyRequest request) {
        String code = generateUniqueCode();
        String rawKey = apiKeyGenerator.generateRawKey();
        String keyHash = passwordEncoder.encode(rawKey);

        String plainSecret = null;
        String encryptedSecret = null;
        if (request.isGenerateSecret()) {
            plainSecret = apiKeyGenerator.generateSecret();
            encryptedSecret = secretCipher.encrypt(plainSecret);
        }

        ApiKeyCredentialsModel model = ApiKeyCredentialsModel.builder()
                .code(code)
                .keyHash(keyHash)
                .secretKeyEncrypted(encryptedSecret)
                .name(request.getName())
                .description(request.getDescription())
                .allowIps(request.getAllowIps())
                .expiresAt(request.getExpiresAtMs() != null
                        ? Instant.ofEpochMilli(request.getExpiresAtMs()) : null)
                .build();

        ApiKeyCredentialsModel saved = credentialsRepository.save(model);
        log.info("Created API key id={} code={}", saved.getId(), code);

        return CreateApiKeyResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .apiKey(code + "." + rawKey)
                .secretKey(plainSecret)
                .build();
    }

    @Override
    @Transactional
    public CreateApiKeyResponse regenerate(String id) {
        ApiKeyCredentialsModel model = getById(id);
        String rawKey = apiKeyGenerator.generateRawKey();
        model.setKeyHash(passwordEncoder.encode(rawKey));

        String plainSecret = null;
        if (model.getSecretKeyEncrypted() != null) {
            plainSecret = apiKeyGenerator.generateSecret();
            model.setSecretKeyEncrypted(secretCipher.encrypt(plainSecret));
        }

        credentialsRepository.save(model);
        log.info("Regenerated API key id={}", id);

        return CreateApiKeyResponse.builder()
                .id(model.getId())
                .name(model.getName())
                .apiKey(model.getCode() + "." + rawKey)
                .secretKey(plainSecret)
                .build();
    }

    @Override
    @Transactional
    public void revoke(String id) {
        ApiKeyCredentialsModel model = getById(id);
        model.setStatus("INACTIVE");
        credentialsRepository.save(model);
        log.info("Revoked API key id={}", id);
    }

    @Override
    public ApiKeyCredentialsModel getById(String id) {
        return credentialsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApiKeyCredentials", "id", id));
    }

    @Override
    public List<ApiKeyCredentialsModel> getAll() {
        return credentialsRepository.findAll();
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = apiKeyGenerator.generateCode();
        } while (credentialsRepository.existsByCode(code));
        return code;
    }
}
