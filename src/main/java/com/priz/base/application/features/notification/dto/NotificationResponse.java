package com.priz.base.application.features.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private String notificationId;
    private String status;
    private String channel;
    private String priority;
    private String subject;
    private String recipient;
    private String errorMessage;
    private Instant sentAt;
    private Instant createdAt;
}
