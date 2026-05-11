package com.priz.base.application.integration.gmail.impl;

import com.priz.base.application.integration.gmail.GmailService;
import com.priz.base.application.integration.gmail.dto.GmailMessage;
import com.priz.base.application.integration.gmail.dto.GmailSendRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "gmail", name = "enabled", havingValue = "false", matchIfMissing = true)
public class GmailServiceNoOpImpl implements GmailService {

    @Override public void sendEmail(String to, String subject, String body) { log.warn("Gmail disabled — skipping sendEmail to {}", to); }
    @Override public void sendHtmlEmail(String to, String subject, String htmlBody) { log.warn("Gmail disabled — skipping sendHtmlEmail to {}", to); }
    @Override public void sendEmail(GmailSendRequest request) { log.warn("Gmail disabled — skipping sendEmail"); }
    @Override public void sendEmailWithAttachments(GmailSendRequest request, List<MultipartFile> attachments) { log.warn("Gmail disabled — skipping sendEmailWithAttachments"); }
    @Override public List<GmailMessage> getEmails(String folder, int limit) { return List.of(); }
    @Override public GmailMessage getEmail(String folder, long uid) { throw new UnsupportedOperationException("Gmail is disabled"); }
    @Override public List<GmailMessage> searchEmails(String query, int limit) { return List.of(); }
    @Override public void markAsRead(String folder, long uid) {}
    @Override public void markAsUnread(String folder, long uid) {}
    @Override public void deleteEmail(String folder, long uid) {}
    @Override public void moveEmail(String folder, long uid, String targetFolder) {}
    @Override public List<String> getFolders() { return List.of(); }
    @Override public boolean isAvailable() { return false; }
}
