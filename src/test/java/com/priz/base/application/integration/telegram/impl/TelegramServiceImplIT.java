package com.priz.base.application.integration.telegram.impl;

import com.priz.base.application.integration.telegram.TelegramService;
import com.priz.base.application.integration.telegram.dto.TelegramFileInfo;
import com.priz.base.application.integration.telegram.dto.TelegramUploadResult;
import com.priz.base.config.telegram.TelegramProperties;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live integration test — gọi thẳng Telegram Bot API với credentials thật.
 * Sử dụng Spring context để load cấu hình từ profile.
 *
 * Chạy: mvnw.cmd test -Dtest=TelegramServiceImplIT
 */
@SpringBootTest
@ActiveProfiles("local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TelegramServiceImplIT {

    @Autowired
    private TelegramProperties properties;

    private static TelegramService service;

    // Shared state giữa các test (chạy theo thứ tự)
    private static long   uploadedMessageId;
    private static String uploadedFileId;

    @BeforeEach
    void setUp() {
        String botToken = properties.getBotToken();
        String baseUrl = properties.getBaseUrl();
        
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl + "/bot" + botToken)
                .build();
        String fileDownloadBaseUrl = baseUrl + "/file/bot" + botToken;
        service = new TelegramServiceImpl(restClient, properties.getChannelId(), fileDownloadBaseUrl);
    }

    // -------------------------------------------------------------------------
    // 1. Health check
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("isAvailable — bot token hợp lệ → true")
    void isAvailable_validBot_returnsTrue() {
        // Act
        boolean result = service.isAvailable();

        // Assert
        assertThat(result).isTrue();
        System.out.println("[OK] isAvailable = " + result);
    }

    // -------------------------------------------------------------------------
    // 2. Upload MultipartFile
    // -------------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("upload MultipartFile — text file → trả về messageId và fileId")
    void upload_multipartFile_returnsUploadResult() {
        // Arrange
        byte[] content = "Hello from Telegram Storage Integration Test!".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "test-upload.txt", "text/plain", content);

        // Act
        TelegramUploadResult result = service.upload(file, "Integration test upload");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMessageId()).isPositive();
        assertThat(result.getFileId()).isNotBlank();
        assertThat(result.getFileUniqueId()).isNotBlank();
        assertThat(result.getFileSize()).isPositive();

        uploadedMessageId = result.getMessageId();
        uploadedFileId    = result.getFileId();

        System.out.printf("[OK] upload MultipartFile → messageId=%d  fileId=%s  size=%d%n",
                result.getMessageId(), result.getFileId(), result.getFileSize());
    }

    // -------------------------------------------------------------------------
    // 3. Upload raw bytes
    // -------------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("upload bytes — JSON content → trả về result")
    void upload_rawBytes_returnsUploadResult() {
        // Arrange
        byte[] json = "{\"test\":true,\"source\":\"priz-base\"}".getBytes();

        // Act
        TelegramUploadResult result = service.upload(
                "data.json", json, "application/json", "Raw bytes upload test");

        // Assert
        assertThat(result.getMessageId()).isPositive();
        assertThat(result.getFileId()).isNotBlank();

        // Cleanup ngay — xóa message test này
        service.deleteMessage(result.getMessageId());
        System.out.printf("[OK] upload bytes → messageId=%d (đã xóa)%n", result.getMessageId());
    }

    // -------------------------------------------------------------------------
    // 4. Get file info
    // -------------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("getFileInfo — fileId hợp lệ → trả về filePath và downloadUrl")
    void getFileInfo_validFileId_returnsFileInfo() {
        // Arrange — dùng fileId từ test upload ở Order(2)
        assertThat(uploadedFileId).as("uploadedFileId phải được set từ test Order(2)").isNotBlank();

        // Act
        TelegramFileInfo info = service.getFileInfo(uploadedFileId);

        // Assert
        assertThat(info).isNotNull();
        assertThat(info.getFileId()).isNotBlank();
        assertThat(info.getFilePath()).isNotBlank();
        assertThat(info.getDownloadUrl()).startsWith("https://api.telegram.org/file/bot");

        System.out.printf("[OK] getFileInfo → filePath=%s%n", info.getFilePath());
        System.out.printf("     downloadUrl=%s%n", info.getDownloadUrl());
    }

    // -------------------------------------------------------------------------
    // 5. Get download URL
    // -------------------------------------------------------------------------

    @Test
    @Order(5)
    @DisplayName("getDownloadUrl — fileId hợp lệ → trả về URL hợp lệ")
    void getDownloadUrl_validFileId_returnsUrl() {
        // Arrange
        assertThat(uploadedFileId).isNotBlank();

        // Act
        String url = service.getDownloadUrl(uploadedFileId);

        // Assert
        assertThat(url).isNotBlank();
        assertThat(url).startsWith("https://");

        System.out.println("[OK] getDownloadUrl → " + url);
    }

    // -------------------------------------------------------------------------
    // 6. Download as Resource
    // -------------------------------------------------------------------------

    @Test
    @Order(6)
    @DisplayName("downloadAsResource — fileId hợp lệ → trả về Resource có nội dung")
    void downloadAsResource_validFileId_returnsResource() throws Exception {
        // Arrange
        assertThat(uploadedFileId).isNotBlank();

        // Act
        Resource resource = service.downloadAsResource(uploadedFileId);

        // Assert
        assertThat(resource).isNotNull();
        assertThat(resource.contentLength()).isPositive();

        byte[] bytes = resource.getContentAsByteArray();
        String content = new String(bytes);
        assertThat(content).contains("Hello from Telegram Storage Integration Test!");

        System.out.printf("[OK] downloadAsResource → %d bytes, content=%s%n",
                bytes.length, content.trim());
    }

    // -------------------------------------------------------------------------
    // 7. Delete message
    // -------------------------------------------------------------------------

    @Test
    @Order(7)
    @DisplayName("deleteMessage — messageId hợp lệ → không ném exception")
    void deleteMessage_validMessageId_deletesSuccessfully() {
        // Arrange
        assertThat(uploadedMessageId).as("uploadedMessageId phải được set từ test Order(2)").isPositive();

        // Act & Assert — không ném exception là thành công
        service.deleteMessage(uploadedMessageId);

        System.out.printf("[OK] deleteMessage → messageId=%d đã xóa%n", uploadedMessageId);
    }
}
