package com.priz.base.application.integration.discord.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priz.base.application.integration.discord.DiscordService;
import com.priz.base.application.integration.discord.dto.DiscordEmbed;
import com.priz.base.application.integration.discord.dto.DiscordMessageResult;
import org.junit.jupiter.api.*;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live integration test — gọi thẳng Discord Bot API với credentials thật.
 * Không cần Spring context.
 *
 * Chạy: mvnw.cmd test -Dtest=DiscordServiceImplIT
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DiscordServiceImplIT {

    private static final String BOT_TOKEN  = System.getenv("DISCORD_BOT_TOKEN");
    private static final String CHANNEL_ID = System.getenv("DISCORD_CHANNEL_ID");
    private static final String BASE_URL   = "https://discord.com/api/v10";

    private static DiscordService service;

    // Shared state giữa các test (chạy theo thứ tự)
    private static String textMessageId;
    private static String uploadedMessageId;
    private static String uploadedCdnUrl;

    @BeforeAll
    static void setUp() {
        RestClient apiClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "Bot " + BOT_TOKEN)
                .build();
        RestClient cdnClient = RestClient.builder().build();
        service = new DiscordServiceImpl(apiClient, cdnClient, CHANNEL_ID, new ObjectMapper());
    }

    // -------------------------------------------------------------------------
    // 1. Health check
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("isAvailable — bot token hợp lệ → true")
    void isAvailable_validToken_returnsTrue() {
        boolean result = service.isAvailable();

        assertThat(result).isTrue();
        System.out.println("[OK] isAvailable = " + result);
    }

    // -------------------------------------------------------------------------
    // 2. Send plain text message
    // -------------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("sendMessage — nội dung text → trả về messageId")
    void sendMessage_textContent_returnsResult() {
        DiscordMessageResult result = service.sendMessage("**[priz-base IT]** Hello from Discord Integration Test!");

        assertThat(result).isNotNull();
        assertThat(result.getMessageId()).isNotBlank();
        assertThat(result.getChannelId()).isEqualTo(CHANNEL_ID);

        textMessageId = result.getMessageId();
        System.out.printf("[OK] sendMessage → messageId=%s%n", result.getMessageId());
    }

    // -------------------------------------------------------------------------
    // 3. Edit message
    // -------------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("editMessage — messageId hợp lệ → nội dung được cập nhật")
    void editMessage_validMessageId_updatesContent() {
        assertThat(textMessageId).as("textMessageId phải được set từ test Order(2)").isNotBlank();

        DiscordMessageResult result = service.editMessage(textMessageId, "**[priz-base IT]** Message edited ✅");

        assertThat(result.getMessageId()).isEqualTo(textMessageId);
        assertThat(result.getContent()).contains("edited");
        System.out.printf("[OK] editMessage → messageId=%s content=%s%n",
                result.getMessageId(), result.getContent());
    }

    // -------------------------------------------------------------------------
    // 4. Send embed
    // -------------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("sendEmbed — embed đầy đủ → trả về messageId")
    void sendEmbed_fullEmbed_returnsResult() {
        DiscordEmbed embed = DiscordEmbed.builder()
                .title("priz-base Integration Test")
                .description("Discord integration service hoạt động bình thường.")
                .color(0x5865F2)
                .fields(List.of(
                        DiscordEmbed.Field.builder().name("Môi trường").value("local").inline(true).build(),
                        DiscordEmbed.Field.builder().name("Service").value("DiscordServiceImpl").inline(true).build()
                ))
                .footer(DiscordEmbed.Footer.builder().text("priz-base @ " + java.time.LocalDateTime.now()).build())
                .build();

        DiscordMessageResult result = service.sendEmbed(embed);

        assertThat(result).isNotNull();
        assertThat(result.getMessageId()).isNotBlank();
        assertThat(result.getAttachments()).isEmpty();

        // Xóa embed message sau khi test
        service.deleteMessage(result.getMessageId());
        System.out.printf("[OK] sendEmbed → messageId=%s (đã xóa)%n", result.getMessageId());
    }

    // -------------------------------------------------------------------------
    // 5. Upload MultipartFile
    // -------------------------------------------------------------------------

    @Test
    @Order(5)
    @DisplayName("upload MultipartFile — text file → trả về messageId và cdnUrl")
    void upload_multipartFile_returnsResultWithAttachment() {
        byte[] content = "Hello from Discord Storage Integration Test!".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "test-upload.txt", "text/plain", content);

        DiscordMessageResult result = service.upload(file, "Integration test upload");

        assertThat(result).isNotNull();
        assertThat(result.getMessageId()).isNotBlank();
        assertThat(result.getAttachments()).hasSize(1);

        var attachment = result.getAttachments().get(0);
        assertThat(attachment.getUrl()).startsWith("https://cdn.discordapp.com");
        assertThat(attachment.getFilename()).isEqualTo("test-upload.txt");
        assertThat(attachment.getSize()).isPositive();

        uploadedMessageId = result.getMessageId();
        uploadedCdnUrl    = attachment.getUrl();

        System.out.printf("[OK] upload MultipartFile → messageId=%s  url=%s  size=%d%n",
                result.getMessageId(), attachment.getUrl(), attachment.getSize());
    }

    // -------------------------------------------------------------------------
    // 6. Upload raw bytes
    // -------------------------------------------------------------------------

    @Test
    @Order(6)
    @DisplayName("upload bytes — JSON content → trả về result")
    void upload_rawBytes_returnsResult() {
        byte[] json = "{\"test\":true,\"source\":\"priz-base\"}".getBytes();

        DiscordMessageResult result = service.upload(
                "data.json", json, "application/json", "Raw bytes upload test");

        assertThat(result.getMessageId()).isNotBlank();
        assertThat(result.getAttachments()).hasSize(1);
        assertThat(result.getAttachments().get(0).getFilename()).isEqualTo("data.json");

        // Xóa ngay
        service.deleteMessage(result.getMessageId());
        System.out.printf("[OK] upload bytes → messageId=%s (đã xóa)%n", result.getMessageId());
    }

    // -------------------------------------------------------------------------
    // 7. Upload with embed
    // -------------------------------------------------------------------------

    @Test
    @Order(7)
    @DisplayName("uploadWithEmbed — file + embed → trả về attachment và messageId")
    void uploadWithEmbed_fileAndEmbed_returnsResult() throws Exception {
        byte[] content = "embed attachment content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "embed-test.txt", "text/plain", content);

        DiscordEmbed embed = DiscordEmbed.builder()
                .title("Upload With Embed Test")
                .description("File kèm embed")
                .color(0x57F287)
                .build();

        DiscordMessageResult result = service.uploadWithEmbed(file, embed);

        assertThat(result.getMessageId()).isNotBlank();
        assertThat(result.getAttachments()).hasSize(1);

        // Xóa ngay
        service.deleteMessage(result.getMessageId());
        System.out.printf("[OK] uploadWithEmbed → messageId=%s (đã xóa)%n", result.getMessageId());
    }

    // -------------------------------------------------------------------------
    // 8. Get message info
    // -------------------------------------------------------------------------

    @Test
    @Order(8)
    @DisplayName("getMessageInfo — messageId hợp lệ → trả về đầy đủ thông tin")
    void getMessageInfo_validMessageId_returnsInfo() {
        assertThat(uploadedMessageId).as("uploadedMessageId phải được set từ test Order(5)").isNotBlank();

        DiscordMessageResult result = service.getMessageInfo(uploadedMessageId);

        assertThat(result).isNotNull();
        assertThat(result.getMessageId()).isEqualTo(uploadedMessageId);
        assertThat(result.getAttachments()).hasSize(1);
        assertThat(result.getAttachments().get(0).getUrl()).isNotBlank();

        System.out.printf("[OK] getMessageInfo → messageId=%s attachments=%d%n",
                result.getMessageId(), result.getAttachments().size());
    }

    // -------------------------------------------------------------------------
    // 9. Download from CDN
    // -------------------------------------------------------------------------

    @Test
    @Order(9)
    @DisplayName("downloadAsResource — CDN URL hợp lệ → trả về Resource có nội dung")
    void downloadAsResource_validCdnUrl_returnsResource() throws Exception {
        assertThat(uploadedCdnUrl).as("uploadedCdnUrl phải được set từ test Order(5)").isNotBlank();

        Resource resource = service.downloadAsResource(uploadedCdnUrl);

        assertThat(resource).isNotNull();
        assertThat(resource.contentLength()).isPositive();

        byte[] bytes = resource.getContentAsByteArray();
        String content = new String(bytes);
        assertThat(content).contains("Hello from Discord Storage Integration Test!");

        System.out.printf("[OK] downloadAsResource → %d bytes, content=%s%n",
                bytes.length, content.trim());
    }

    // -------------------------------------------------------------------------
    // 10. Delete messages (cleanup)
    // -------------------------------------------------------------------------

    @Test
    @Order(10)
    @DisplayName("deleteMessage — messageId hợp lệ → không ném exception")
    void deleteMessage_validMessageId_deletesSuccessfully() {
        assertThat(uploadedMessageId).as("uploadedMessageId phải được set từ test Order(5)").isNotBlank();

        service.deleteMessage(uploadedMessageId);
        System.out.printf("[OK] deleteMessage → messageId=%s đã xóa%n", uploadedMessageId);

        // Dọn dẹp text message từ Order(2)
        if (textMessageId != null) {
            service.deleteMessage(textMessageId);
            System.out.printf("[OK] deleteMessage → messageId=%s đã xóa%n", textMessageId);
        }
    }
}
