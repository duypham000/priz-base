package com.priz.base.application.integration.gmail;

import com.priz.base.application.integration.gmail.dto.GmailMessage;
import com.priz.base.application.integration.gmail.dto.GmailSendRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface GmailService {

    // ── Gửi mail ──────────────────────────────────────────────────────────────

    void sendEmail(String to, String subject, String body);

    void sendHtmlEmail(String to, String subject, String htmlBody);

    void sendEmail(GmailSendRequest request);

    void sendEmailWithAttachments(GmailSendRequest request, List<MultipartFile> attachments);

    // ── Đọc / liệt kê ─────────────────────────────────────────────────────────

    List<GmailMessage> getEmails(String folder, int limit);

    GmailMessage getEmail(String folder, long uid);

    List<GmailMessage> searchEmails(String query, int limit);

    // ── Quản lý ────────────────────────────────────────────────────────────────

    void markAsRead(String folder, long uid);

    void markAsUnread(String folder, long uid);

    void deleteEmail(String folder, long uid);

    void moveEmail(String folder, long uid, String targetFolder);

    List<String> getFolders();

    // ── Health ─────────────────────────────────────────────────────────────────

    boolean isAvailable();
}
