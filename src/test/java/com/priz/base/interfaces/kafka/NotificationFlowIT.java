package com.priz.base.interfaces.kafka;

import com.priz.base.application.features.notification.NotificationService;
import com.priz.base.application.features.notification.dto.NotificationResponse;
import com.priz.base.application.features.notification.dto.SendNotificationRequest;
import com.priz.base.application.integration.discord.DiscordService;
import com.priz.base.application.integration.gmail.GmailService;
import com.priz.base.domain.mysql_priz_base.model.NotificationModel;
import com.priz.base.domain.mysql_priz_base.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

class NotificationFlowIT extends BaseKafkaIT {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void testNotificationFlow_HighPriority_Gmail() {
        // Arrange
        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(NotificationModel.Channel.GMAIL)
                .priority(NotificationModel.Priority.HIGH)
                .subject("High Priority Test")
                .body("Hello from IT")
                .recipient("test@example.com")
                .build();

        // Act
        NotificationResponse response = notificationService.send(request);
        String notificationId = response.getNotificationId();

        // Assert - verify interaction and DB update
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            verify(gmailService, atLeastOnce()).sendEmail(eq("test@example.com"), eq("High Priority Test"), anyString());
            
            NotificationModel notification = notificationRepository.findById(notificationId).orElseThrow();
            assertThat(notification.getStatus()).isEqualTo(NotificationModel.Status.SENT);
        });
    }

    @Test
    void testNotificationFlow_NormalPriority_Discord() {
        // Arrange
        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(NotificationModel.Channel.DISCORD)
                .priority(NotificationModel.Priority.NORMAL)
                .subject("Normal Priority Test")
                .body("Hello Discord")
                .build();

        // Act
        NotificationResponse response = notificationService.send(request);
        String notificationId = response.getNotificationId();

        // Assert
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            verify(discordService, atLeastOnce()).sendMessage(anyString());
            
            NotificationModel notification = notificationRepository.findById(notificationId).orElseThrow();
            assertThat(notification.getStatus()).isEqualTo(NotificationModel.Status.SENT);
        });
    }
}
