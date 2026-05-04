package com.priz.base.application.features.notification.impl;

import com.priz.base.application.features.notification.NotificationService;
import com.priz.base.application.features.notification.dto.NotificationResponse;
import com.priz.base.application.features.notification.dto.SendNotificationRequest;
import com.priz.base.application.features.notification.event.NotificationEvent;
import com.priz.base.domain.mysql.priz_base.model.NotificationModel;
import com.priz.base.domain.mysql.priz_base.repository.NotificationRepository;
import com.priz.base.infrastructure.kafka.producer.NotificationEventProducer;
import com.priz.common.exception.ResourceNotFoundException;
import com.priz.common.security.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationEventProducer eventProducer;

    @Override
    @Transactional
    public NotificationResponse send(SendNotificationRequest request) {
        String userId = safeGetUserId();

        NotificationModel notification = NotificationModel.builder()
                .userId(userId)
                .channel(request.getChannel())
                .notificationType(request.getNotificationType())
                .priority(request.getPriority())
                .subject(request.getSubject())
                .body(request.getBody())
                .recipient(request.getRecipient())
                .hasAttachment(request.isHasAttachment())
                .status(NotificationModel.Status.PENDING)
                .build();
        notification = notificationRepository.save(notification);

        NotificationEvent event = NotificationEvent.builder()
                .notificationId(notification.getId())
                .channel(notification.getChannel().name())
                .notificationType(notification.getNotificationType().name())
                .priority(notification.getPriority().name())
                .subject(notification.getSubject())
                .body(notification.getBody())
                .recipient(notification.getRecipient())
                .hasAttachment(notification.getHasAttachment())
                .userId(userId)
                .build();

        eventProducer.publish(event);

        log.info("Queued notification id={} channel={} priority={}", notification.getId(),
                notification.getChannel(), notification.getPriority());

        return toResponse(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getStatus(String notificationId) {
        NotificationModel notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        return toResponse(notification);
    }

    private NotificationResponse toResponse(NotificationModel notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getId())
                .status(notification.getStatus().name())
                .channel(notification.getChannel().name())
                .priority(notification.getPriority().name())
                .subject(notification.getSubject())
                .recipient(notification.getRecipient())
                .errorMessage(notification.getErrorMessage())
                .sentAt(notification.getSentAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private String safeGetUserId() {
        try {
            return SecurityContextHolder.getCurrentUserId();
        } catch (Exception e) {
            return "system";
        }
    }
}
