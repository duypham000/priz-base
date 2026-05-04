package com.priz.base.interfaces.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priz.base.application.features.notification.event.NotificationEvent;
import com.priz.base.domain.mysql.priz_base.model.NotificationModel;
import com.priz.base.domain.mysql.priz_base.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDltConsumer {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.notification-dlt}",
            groupId = "priz-notification-dlt-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeDlt(
            @Payload String payload,
            @Header(name = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage,
            @Header(name = KafkaHeaders.ORIGINAL_TOPIC, required = false) byte[] originalTopicBytes,
            @Header(name = KafkaHeaders.ORIGINAL_PARTITION, required = false) Integer originalPartition,
            @Header(name = KafkaHeaders.ORIGINAL_OFFSET, required = false) Long originalOffset,
            Acknowledgment acknowledgment) {

        log.error("Notification DLT — originalTopic={} partition={} offset={} error={}",
                originalTopicBytes != null ? new String(originalTopicBytes) : "unknown",
                originalPartition, originalOffset, exceptionMessage);

        try {
            NotificationEvent event = objectMapper.readValue(payload, NotificationEvent.class);

            notificationRepository.findById(event.getNotificationId()).ifPresent(n -> {
                n.setStatus(NotificationModel.Status.FAILED);
                n.setErrorMessage(truncate(exceptionMessage, 1000));
                notificationRepository.save(n);
                log.error("Marked notification FAILED id={} channel={}", n.getId(), n.getChannel());
            });

        } catch (IOException e) {
            log.error("Could not parse Notification DLT payload: {}", e.getMessage());
        }

        acknowledgment.acknowledge();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
