package com.priz.base.interfaces.kafka;

import com.priz.base.application.features.files.FileService;
import com.priz.base.application.features.files.dto.AsyncUploadResponse;
import com.priz.base.application.integration.telegram.TelegramService;
import com.priz.base.application.integration.telegram.dto.TelegramUploadResult;
import com.priz.base.common.storage.LocalStorageService;
import com.priz.base.domain.mysql_priz_base.model.FileUploadJobModel;
import com.priz.base.domain.mysql_priz_base.repository.FileUploadJobRepository;
import com.priz.common.security.SecurityContext;
import com.priz.common.security.SecurityContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class FileUploadFlowIT extends BaseKafkaIT {

    @Autowired
    private FileService fileService;

    @Autowired
    private FileUploadJobRepository jobRepository;

    @BeforeEach
    void setUp() throws IOException {
        SecurityContextHolder.set(SecurityContext.builder()
                .userId("test-user-id")
                .email("test@example.com")
                .build());

        // Create temp directory and dummy file
        Path tempDir = Path.of("target/test-storage");
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }
        Files.writeString(tempDir.resolve("stored-test.txt"), "dummy content");
    }

    @Test
    void testFileUploadFlow_EndToEnd() throws IOException {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello Kafka".getBytes());
        
        when(storageService.store(any())).thenReturn("stored-test.txt");
        when(storageService.getRootLocation()).thenReturn(Path.of("target/test-storage"));
        
        TelegramUploadResult mockResult = TelegramUploadResult.builder()
                .messageId(12345L)
                .downloadUrl("http://telegram.me/file")
                .build();
        
        when(telegramService.upload(anyString(), any(InputStream.class), anyLong(), anyString(), anyString()))
                .thenReturn(mockResult);

        // Act
        AsyncUploadResponse response = fileService.upload(file, "Test upload");
        String jobId = response.getJobId();

        // Assert
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            FileUploadJobModel job = jobRepository.findById(jobId).orElseThrow();
            assertThat(job.getStatus()).isEqualTo(FileUploadJobModel.Status.COMPLETED);
            assertThat(job.getTelegramUrl()).isEqualTo("http://telegram.me/file");
            assertThat(job.getTelegramMessageId()).isEqualTo(12345L);
        });
    }
}
