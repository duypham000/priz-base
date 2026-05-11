package com.priz.base.application.features.apikey.impl;

import com.priz.base.application.features.apikey.dto.CreateApiKeyRequest;
import com.priz.base.application.features.apikey.dto.CreateApiKeyResponse;
import com.priz.base.application.features.apikey.util.ApiKeyGenerator;
import com.priz.base.domain.mysql_priz_base.model.ApiKeyCredentialsModel;
import com.priz.base.domain.mysql_priz_base.repository.ApiKeyCredentialsRepository;
import com.priz.base.infrastructure.security.apikey.SecretCipher;
import com.priz.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceImplTest {

    @Mock
    private ApiKeyCredentialsRepository credentialsRepository;
    @Mock
    private ApiKeyGenerator apiKeyGenerator;
    @Mock
    private SecretCipher secretCipher;

    private PasswordEncoder passwordEncoder;
    private ApiKeyServiceImpl apiKeyService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        apiKeyService = new ApiKeyServiceImpl(
                credentialsRepository, apiKeyGenerator, passwordEncoder, secretCipher);
    }

    // ---- create ----

    @Test
    void create_withoutSecret_savesAndReturnsApiKey() {
        CreateApiKeyRequest request = new CreateApiKeyRequest();
        request.setName("My Key");
        request.setDescription("For testing");
        request.setGenerateSecret(false);

        when(apiKeyGenerator.generateCode()).thenReturn("testcode12");
        when(apiKeyGenerator.generateRawKey()).thenReturn("rawKeyValue");
        when(credentialsRepository.existsByCode("testcode12")).thenReturn(false);

        ApiKeyCredentialsModel saved = ApiKeyCredentialsModel.builder()
                .code("testcode12").keyHash("hash").name("My Key").build();
        saved.setId("cred-id-1");
        when(credentialsRepository.save(any())).thenReturn(saved);

        CreateApiKeyResponse response = apiKeyService.create(request);

        assertThat(response.getId()).isEqualTo("cred-id-1");
        assertThat(response.getApiKey()).isEqualTo("testcode12.rawKeyValue");
        assertThat(response.getSecretKey()).isNull();
    }

    @Test
    void create_withSecret_encryptsAndReturnsSecret() {
        CreateApiKeyRequest request = new CreateApiKeyRequest();
        request.setName("Signed Key");
        request.setGenerateSecret(true);

        when(apiKeyGenerator.generateCode()).thenReturn("code123456");
        when(apiKeyGenerator.generateRawKey()).thenReturn("rawKey");
        when(apiKeyGenerator.generateSecret()).thenReturn("plainSecret");
        when(credentialsRepository.existsByCode("code123456")).thenReturn(false);
        when(secretCipher.encrypt("plainSecret")).thenReturn("encryptedSecret");

        ApiKeyCredentialsModel saved = ApiKeyCredentialsModel.builder()
                .code("code123456").keyHash("hash").name("Signed Key").build();
        saved.setId("cred-id-2");
        when(credentialsRepository.save(any())).thenReturn(saved);

        CreateApiKeyResponse response = apiKeyService.create(request);

        assertThat(response.getSecretKey()).isEqualTo("plainSecret");
        assertThat(response.getApiKey()).isEqualTo("code123456.rawKey");
        verify(secretCipher).encrypt("plainSecret");
    }

    @Test
    void create_codeCollision_retriesUntilUnique() {
        CreateApiKeyRequest request = new CreateApiKeyRequest();
        request.setName("Key");
        request.setGenerateSecret(false);

        when(apiKeyGenerator.generateCode())
                .thenReturn("collision1").thenReturn("collision1").thenReturn("unique123");
        when(apiKeyGenerator.generateRawKey()).thenReturn("rawKey");
        when(credentialsRepository.existsByCode("collision1")).thenReturn(true);
        when(credentialsRepository.existsByCode("unique123")).thenReturn(false);

        ApiKeyCredentialsModel saved = ApiKeyCredentialsModel.builder()
                .code("unique123").keyHash("hash").name("Key").build();
        saved.setId("cred-id-3");
        when(credentialsRepository.save(any())).thenReturn(saved);

        CreateApiKeyResponse response = apiKeyService.create(request);

        assertThat(response.getApiKey()).startsWith("unique123.");
    }

    @Test
    void create_keyHashStored_notRawKey() {
        CreateApiKeyRequest request = new CreateApiKeyRequest();
        request.setName("Key");
        request.setGenerateSecret(false);

        when(apiKeyGenerator.generateCode()).thenReturn("code001");
        when(apiKeyGenerator.generateRawKey()).thenReturn("myRawKey");
        when(credentialsRepository.existsByCode("code001")).thenReturn(false);

        ApiKeyCredentialsModel saved = ApiKeyCredentialsModel.builder()
                .code("code001").name("Key").build();
        saved.setId("id");
        when(credentialsRepository.save(any())).thenReturn(saved);

        apiKeyService.create(request);

        ArgumentCaptor<ApiKeyCredentialsModel> captor =
                ArgumentCaptor.forClass(ApiKeyCredentialsModel.class);
        verify(credentialsRepository).save(captor.capture());
        String storedHash = captor.getValue().getKeyHash();
        // Stored hash must NOT equal raw key
        assertThat(storedHash).isNotEqualTo("myRawKey");
        // But raw key must match the stored hash
        assertThat(passwordEncoder.matches("myRawKey", storedHash)).isTrue();
    }

    // ---- revoke ----

    @Test
    void revoke_existingKey_setsInactive() {
        ApiKeyCredentialsModel cred = ApiKeyCredentialsModel.builder()
                .code("code").keyHash("hash").status("ACTIVE").build();
        cred.setId("id-1");
        when(credentialsRepository.findById("id-1")).thenReturn(Optional.of(cred));
        when(credentialsRepository.save(any())).thenReturn(cred);

        apiKeyService.revoke("id-1");

        ArgumentCaptor<ApiKeyCredentialsModel> captor =
                ArgumentCaptor.forClass(ApiKeyCredentialsModel.class);
        verify(credentialsRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void revoke_notFound_throwsResourceNotFound() {
        when(credentialsRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyService.revoke("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- regenerate ----

    @Test
    void regenerate_noSecret_updatesHashAndReturnsNewKey() {
        ApiKeyCredentialsModel cred = ApiKeyCredentialsModel.builder()
                .code("code001").keyHash("old-hash").name("My Key")
                .secretKeyEncrypted(null).build();
        cred.setId("id-1");
        when(credentialsRepository.findById("id-1")).thenReturn(Optional.of(cred));
        when(apiKeyGenerator.generateRawKey()).thenReturn("newRawKey");
        when(credentialsRepository.save(any())).thenReturn(cred);

        CreateApiKeyResponse response = apiKeyService.regenerate("id-1");

        assertThat(response.getApiKey()).isEqualTo("code001.newRawKey");
        assertThat(response.getSecretKey()).isNull();
        assertThat(passwordEncoder.matches("newRawKey", cred.getKeyHash())).isTrue();
    }

    @Test
    void regenerate_withSecret_regeneratesSecretToo() {
        ApiKeyCredentialsModel cred = ApiKeyCredentialsModel.builder()
                .code("code001").keyHash("old-hash").name("Signed Key")
                .secretKeyEncrypted("old-encrypted").build();
        cred.setId("id-1");
        when(credentialsRepository.findById("id-1")).thenReturn(Optional.of(cred));
        when(apiKeyGenerator.generateRawKey()).thenReturn("newRawKey");
        when(apiKeyGenerator.generateSecret()).thenReturn("newPlainSecret");
        when(secretCipher.encrypt("newPlainSecret")).thenReturn("newEncrypted");
        when(credentialsRepository.save(any())).thenReturn(cred);

        CreateApiKeyResponse response = apiKeyService.regenerate("id-1");

        assertThat(response.getSecretKey()).isEqualTo("newPlainSecret");
        assertThat(cred.getSecretKeyEncrypted()).isEqualTo("newEncrypted");
    }
}
