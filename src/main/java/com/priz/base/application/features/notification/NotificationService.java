package com.priz.base.application.features.notification;

import com.priz.base.application.features.notification.dto.NotificationResponse;
import com.priz.base.application.features.notification.dto.SendNotificationRequest;

public interface NotificationService {

    NotificationResponse send(SendNotificationRequest request);

    NotificationResponse getStatus(String notificationId);
}
