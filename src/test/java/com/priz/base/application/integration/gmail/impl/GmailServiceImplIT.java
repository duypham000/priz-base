package com.priz.base.application.integration.gmail.impl;

import com.priz.base.application.integration.gmail.GmailService;
import com.priz.base.application.integration.gmail.dto.GmailMessage;
import com.priz.base.application.integration.gmail.dto.GmailSendRequest;
import com.priz.base.config.gmail.GmailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live integration test — gọi thẳng Gmail SMTP/IMAP với credentials thật (App Password).
 * Chạy: mvnw.cmd test -Dtest=GmailServiceImplIT
 */
@SpringBootTest(properties = "spring.grpc.server.port=0")
@ActiveProfiles("local")
@EnableConfigurationProperties(GmailProperties.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GmailServiceImplIT {

    @Autowired
    private GmailProperties properties;

    private GmailService service;

    // Shared state giữa các test
    private static long sentEmailUid = -1;
    private static String trashFolder = null;

    @BeforeEach
    void setUp() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(properties.getSmtpHost());
        sender.setPort(properties.getSmtpPort());
        sender.setUsername(properties.getUsername());
        sender.setPassword(properties.getAppPassword());

        Properties javaMailProps = sender.getJavaMailProperties();
        javaMailProps.put("mail.transport.protocol", "smtp");
        javaMailProps.put("mail.smtp.auth", "true");
        javaMailProps.put("mail.smtp.starttls.enable", "true");
        javaMailProps.put("mail.smtp.starttls.required", "true");
        javaMailProps.put("mail.debug", "false");

        service = new GmailServiceImpl(sender, properties);
    }

    // ── 1. Health check ────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("isAvailable — IMAP kết nối thành công → true")
    void isAvailable_validCredentials_returnsTrue() {
        boolean result = service.isAvailable();

        assertThat(result).isTrue();
        System.out.println("[OK] isAvailable = " + result);
    }

    // ── 2. getFolders ──────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("getFolders — liệt kê toàn bộ folder IMAP")
    void getFolders_returnsNonEmptyList() {
        List<String> folders = service.getFolders();

        assertThat(folders).isNotEmpty();
        System.out.println("[OK] getFolders → " + folders.size() + " folders");
        folders.forEach(f -> System.out.println("     " + f));

        // Tìm folder Trash (tên khác nhau theo locale: Trash / Thùng rác / ...)
        trashFolder = folders.stream()
                .filter(f -> f.toLowerCase().contains("trash") || f.contains("Thùng") || f.contains("thùng") || f.contains("Thùng"))
                .findFirst()
                .orElse(folders.stream().filter(f -> f.startsWith("[Gmail]") && !f.equals("[Gmail]")).findFirst().orElse(null));
        System.out.println("     → Trash folder detected: " + trashFolder);
    }

    // ── 3. sendEmail (text) ────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("sendEmail — gửi plain text email đến chính mình")
    void sendEmail_plainText_noException() {
        String self = properties.getUsername();

        service.sendEmail(self, "[IT] GmailService plain-text test", "Đây là email test plain-text từ GmailServiceImplIT.");

        System.out.println("[OK] sendEmail (plain text) → gửi đến " + self);
    }

    // ── 4. sendHtmlEmail ──────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("sendHtmlEmail — gửi HTML email đến chính mình")
    void sendHtmlEmail_noException() {
        String self = properties.getUsername();
        String html = "<h2>Gmail Integration Test</h2><p>Email <b>HTML</b> từ <code>GmailServiceImplIT</code>.</p>";

        service.sendHtmlEmail(self, "[IT] GmailService HTML test", html);

        System.out.println("[OK] sendHtmlEmail → gửi đến " + self);
    }

    // ── 5. sendEmail (request object) ─────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("sendEmail(GmailSendRequest) — cc, bcc, replyTo")
    void sendEmail_requestObject_noException() {
        String self = properties.getUsername();
        GmailSendRequest request = GmailSendRequest.builder()
                .to(List.of(self))
                .subject("[IT] GmailService request object test")
                .body("Test via GmailSendRequest với cc và replyTo.")
                .html(false)
                .replyTo(self)
                .build();

        service.sendEmail(request);

        System.out.println("[OK] sendEmail(GmailSendRequest) → gửi đến " + self);
    }

    // ── 6. sendEmailWithAttachments ───────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("sendEmailWithAttachments — gửi email kèm file txt")
    void sendEmailWithAttachments_noException() {
        String self = properties.getUsername();
        GmailSendRequest request = GmailSendRequest.builder()
                .to(List.of(self))
                .subject("[IT] GmailService attachment test")
                .body("Email kèm attachment từ GmailServiceImplIT.")
                .html(false)
                .build();

        MockMultipartFile attachment = new MockMultipartFile(
                "file", "priz-test.txt", "text/plain",
                "Nội dung file đính kèm từ integration test.".getBytes());

        service.sendEmailWithAttachments(request, List.of(attachment));

        System.out.println("[OK] sendEmailWithAttachments → gửi kèm 1 file đến " + self);
    }

    // ── 7. getEmails ──────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("getEmails — lấy 10 email mới nhất từ INBOX")
    void getEmails_inbox_returnsMessages() throws InterruptedException {
        // Đợi email gửi ở các test trước vào INBOX
        Thread.sleep(3000);

        List<GmailMessage> emails = service.getEmails("INBOX", 10);

        assertThat(emails).isNotEmpty();
        GmailMessage first = emails.get(0);
        assertThat(first.getSubject()).isNotNull();
        assertThat(first.getUid()).isPositive();

        // Lưu UID cho các test tiếp theo
        sentEmailUid = first.getUid();

        System.out.println("[OK] getEmails → " + emails.size() + " emails");
        emails.forEach(m -> System.out.printf("     uid=%-8d | read=%-5b | subject=%s%n",
                m.getUid(), m.isRead(), m.getSubject()));
    }

    // ── 8. getEmail (by UID) ───────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("getEmail — lấy email theo UID → trả về đầy đủ thông tin")
    void getEmail_validUid_returnsMessage() {
        assertThat(sentEmailUid).as("sentEmailUid phải được set từ test Order(7)").isPositive();

        GmailMessage message = service.getEmail("INBOX", sentEmailUid);

        assertThat(message).isNotNull();
        assertThat(message.getUid()).isEqualTo(sentEmailUid);
        assertThat(message.getFrom()).isNotBlank();

        System.out.printf("[OK] getEmail uid=%d → from=%s | subject=%s%n",
                message.getUid(), message.getFrom(), message.getSubject());
        if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
            System.out.println("     attachments: " + message.getAttachments().stream()
                    .map(a -> a.getFileName() + " (" + a.getSize() + " bytes)").toList());
        }
    }

    // ── 9. searchEmails ───────────────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("searchEmails — tìm theo subject chứa '[IT]'")
    void searchEmails_bySubject_returnsResults() {
        List<GmailMessage> results = service.searchEmails("[IT]", 10);

        assertThat(results).isNotEmpty();
        System.out.println("[OK] searchEmails '[IT]' → " + results.size() + " kết quả");
        results.forEach(m -> System.out.printf("     uid=%-8d | %s%n", m.getUid(), m.getSubject()));
    }

    // ── 10. markAsRead ────────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("markAsRead — đánh dấu email đã đọc")
    void markAsRead_validUid_noException() {
        assertThat(sentEmailUid).isPositive();

        service.markAsRead("INBOX", sentEmailUid);

        GmailMessage after = service.getEmail("INBOX", sentEmailUid);
        assertThat(after.isRead()).isTrue();
        System.out.printf("[OK] markAsRead uid=%d → read=%b%n", sentEmailUid, after.isRead());
    }

    // ── 11. markAsUnread ──────────────────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("markAsUnread — đánh dấu email chưa đọc")
    void markAsUnread_validUid_noException() {
        assertThat(sentEmailUid).isPositive();

        service.markAsUnread("INBOX", sentEmailUid);

        GmailMessage after = service.getEmail("INBOX", sentEmailUid);
        assertThat(after.isRead()).isFalse();
        System.out.printf("[OK] markAsUnread uid=%d → read=%b%n", sentEmailUid, after.isRead());
    }

    // ── 12. moveEmail ─────────────────────────────────────────────────────────

    @Test
    @Order(12)
    @DisplayName("moveEmail — chuyển email sang folder Trash")
    void moveEmail_toTrash_noException() {
        assertThat(sentEmailUid).isPositive();
        assertThat(trashFolder).as("trashFolder phải được detect từ test Order(2)").isNotNull();

        service.moveEmail("INBOX", sentEmailUid, trashFolder);

        System.out.printf("[OK] moveEmail uid=%d → %s%n", sentEmailUid, trashFolder);
    }

    // ── 13. deleteEmail ───────────────────────────────────────────────────────

    @Test
    @Order(13)
    @DisplayName("deleteEmail — xóa vĩnh viễn email trong Trash")
    void deleteEmail_fromTrash_noException() {
        assertThat(trashFolder).as("trashFolder phải được detect từ test Order(2)").isNotNull();

        List<GmailMessage> trashEmails = service.getEmails(trashFolder, 5);
        assertThat(trashEmails).isNotEmpty();

        long trashUid = trashEmails.get(0).getUid();
        service.deleteEmail(trashFolder, trashUid);

        System.out.printf("[OK] deleteEmail uid=%d từ %s%n", trashUid, trashFolder);
    }
}
