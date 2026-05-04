package com.priz.base.domain.mysql.priz_base.model;

import com.priz.base.common.model.BaseModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_status", columnList = "status"),
        @Index(name = "idx_notifications_channel", columnList = "channel"),
        @Index(name = "idx_notifications_user_id", columnList = "user_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationModel extends BaseModel {

    public enum Channel {
        GMAIL, DISCORD, BOTH
    }

    public enum NotificationType {
        PLAIN_TEXT, HTML
    }

    public enum Priority {
        HIGH, NORMAL, LOW
    }

    public enum Status {
        PENDING, SENT, FAILED
    }

    @Column(name = "user_id", length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 20)
    @Builder.Default
    private NotificationType notificationType = NotificationType.PLAIN_TEXT;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    @Builder.Default
    private Priority priority = Priority.NORMAL;

    @Column(name = "subject", length = 255)
    private String subject;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "recipient", length = 255)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "has_attachment", nullable = false)
    @Builder.Default
    private Boolean hasAttachment = false;

    @Column(name = "sent_at")
    private Instant sentAt;
}
