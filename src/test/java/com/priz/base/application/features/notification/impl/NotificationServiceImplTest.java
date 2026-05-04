package com.priz.base.application.features.notification.impl;

import com.priz.base.application.features.notification.dto.NotificationResponse;
import com.priz.base.application.features.notification.dto.SendNotificationRequest;
import com.priz.base.application.features.notification.event.NotificationEvent;
import com.priz.base.domain.mysql.priz_base.model.NotificationModel;
import com.priz.base.domain.mysql.priz_base.repository.NotificationRepository;
import com.priz.base.infrastructure.kafka.producer.NotificationEventProducer;
import com.priz.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationEventProducer eventProducer;

    @InjectMocks
    private NotificationServiceImpl service;

    @Test
    void send_ValidRequest_SavesPendingAndPublishesEvent() {
        // Arrange
        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(NotificationModel.Channel.DISCORD)
                .notificationType(NotificationModel.NotificationType.PLAIN_TEXT)
                .priority(NotificationModel.Priority.HIGH)
                .subject("Test subject")
                .body("Test body")
                .recipient("test@example.com")
                .build();

        NotificationModel saved = NotificationModel.builder()
                .channel(NotificationModel.Channel.DISCORD)
                .notificationType(NotificationModel.NotificationType.PLAIN_TEXT)
                .priority(NotificationModel.Priority.HIGH)
                .subject("Test subject")
                .body("Test body")
                .status(NotificationModel.Status.PENDING)
                .build();
        saved.setId("notif-id-1");
        saved.setCreatedAt(Instant.now());
        when(notificationRepository.save(any(NotificationModel.class))).thenReturn(saved);

        // Act
        NotificationResponse response = service.send(request);

        // Assert
        assertThat(response.getNotificationId()).isEqualTo("notif-id-1");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getChannel()).isEqualTo("DISCORD");
        assertThat(response.getPriority()).isEqualTo("HIGH");
        verify(notificationRepository).save(any(NotificationModel.class));
        verify(eventProducer).publish(any(NotificationEvent.class));
    }

    @Test
    void send_PublishesEventWithCorrectChannelAndPriority() {
        // Arrange
        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(NotificationModel.Channel.GMAIL)
                .priority(NotificationModel.Priority.LOW)
                .subject("Low priority")
                .body("Content")
                .build();

        NotificationModel saved = NotificationModel.builder()
                .channel(NotificationModel.Channel.GMAIL)
                .priority(NotificationModel.Priority.LOW)
                .subject("Low priority")
                .status(NotificationModel.Status.PENDING)
                .build();
        saved.setId("notif-id-2");
        saved.setCreatedAt(Instant.now());
        when(notificationRepository.save(any(NotificationModel.class))).thenReturn(saved);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);

        // Act
        service.send(request);

        // Assert
        verify(eventProducer).publish(eventCaptor.capture());
        NotificationEvent captured = eventCaptor.getValue();
        assertThat(captured.getNotificationId()).isEqualTo("notif-id-2");
        assertThat(captured.getChannel()).isEqualTo("GMAIL");
        assertThat(captured.getPriority()).isEqualTo("LOW");
    }

    @Test
    void send_BothChannel_PublishesWithBothChannel() {
        // Arrange
        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(NotificationModel.Channel.BOTH)
                .priority(NotificationModel.Priority.NORMAL)
                .subject("Both channels")
                .body("Send to all")
                .build();

        NotificationModel saved = NotificationModel.builder()
                .channel(NotificationModel.Channel.BOTH)
                .priority(NotificationModel.Priority.NORMAL)
                .status(NotificationModel.Status.PENDING)
                .build();
        saved.setId("notif-id-3");
        saved.setCreatedAt(Instant.now());
        when(notificationRepository.save(any(NotificationModel.class))).thenReturn(saved);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);

        // Act
        service.send(request);

        // Assert
        verify(eventProducer).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getChannel()).isEqualTo("BOTH");
    }

    @Test
    void getStatus_ExistingNotification_ReturnsCorrectStatus() {
        // Arrange
        NotificationModel notification = NotificationModel.builder()
                .channel(NotificationModel.Channel.GMAIL)
                .priority(NotificationModel.Priority.NORMAL)
                .subject("Hello")
                .status(NotificationModel.Status.SENT)
                .build();
        notification.setId("notif-id-4");
        notification.setCreatedAt(Instant.now());
        when(notificationRepository.findById("notif-id-4")).thenReturn(Optional.of(notification));

        // Act
        NotificationResponse response = service.getStatus("notif-id-4");

        // Assert
        assertThat(response.getStatus()).isEqualTo("SENT");
        assertThat(response.getChannel()).isEqualTo("GMAIL");
    }

    @Test
    void getStatus_NonExistingNotification_ThrowsResourceNotFoundException() {
        // Arrange
        when(notificationRepository.findById("missing")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.getStatus("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
