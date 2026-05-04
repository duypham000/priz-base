package com.priz.base.application.integration.gmail.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class GmailMessage {

    private long uid;
    private String messageId;
    private String from;
    private String to;
    private String cc;
    private String subject;
    private String body;
    private String htmlBody;
    private LocalDateTime receivedAt;
    private LocalDateTime sentAt;
    private boolean read;
    private List<GmailAttachment> attachments;
}
