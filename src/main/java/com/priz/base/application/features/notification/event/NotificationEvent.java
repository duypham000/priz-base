package com.priz.base.application.features.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    private String notificationId;
    private String channel;
    private String notificationType;
    private String priority;
    private String subject;
    private String body;
    private String recipient;
    private boolean hasAttachment;
    private String traceId;
    private String userId;
    @Builder.Default
    private Instant createdAt = Instant.now();
}
