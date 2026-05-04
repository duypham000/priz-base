package com.priz.base.application.features.notification.dto;

import com.priz.base.domain.mysql.priz_base.model.NotificationModel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {

    @NotNull
    private NotificationModel.Channel channel;

    @NotNull
    @Builder.Default
    private NotificationModel.NotificationType notificationType = NotificationModel.NotificationType.PLAIN_TEXT;

    @NotNull
    @Builder.Default
    private NotificationModel.Priority priority = NotificationModel.Priority.NORMAL;

    @NotBlank
    private String subject;

    @NotBlank
    private String body;

    private String recipient;

    @Builder.Default
    private boolean hasAttachment = false;
}
