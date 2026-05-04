package com.priz.base.interfaces.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priz.base.application.features.notification.event.NotificationEvent;
import com.priz.base.application.integration.discord.DiscordService;
import com.priz.base.application.integration.gmail.GmailService;
import com.priz.base.domain.mysql.priz_base.model.NotificationModel;
import com.priz.base.domain.mysql.priz_base.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationRepository notificationRepository;
    private final GmailService gmailService;
    private final DiscordService discordService;
    private final ObjectMapper objectMapper;

    /**
     * HIGH priority — partition 0 — single-record, lowest latency.
     */
    @KafkaListener(
            topicPartitions = @TopicPartition(
                    topic = "${app.kafka.topics.notification}",
                    partitions = {"0"}
            ),
            groupId = "priz-notification-high-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeHighPriority(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(name = "traceId", required = false) byte[] traceIdBytes,
            Acknowledgment acknowledgment) {

        setupMdc(traceIdBytes, partition, offset);
        try {
            NotificationEvent event = objectMapper.readValue(payload, NotificationEvent.class);
            log.info("HIGH priority notification id={} channel={}", event.getNotificationId(), event.getChannel());
            processAndAck(event, acknowledgment);
        } catch (IOException e) {
            log.error("Failed to parse notification payload: {}", e.getMessage());
            acknowledgment.acknowledge();
        } finally {
            clearMdc();
        }
    }

    /**
     * NORMAL + LOW priority — partitions 1 & 2 — batch processing for throughput.
     */
    @KafkaListener(
            topicPartitions = @TopicPartition(
                    topic = "${app.kafka.topics.notification}",
                    partitions = {"1", "2"}
            ),
            groupId = "priz-notification-batch-group",
            containerFactory = "batchContainerFactory"
    )
    @Transactional
    public void consumeBatch(
            @Payload List<String> payloads,
            @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
            @Header(KafkaHeaders.OFFSET) List<Long> offsets,
            Acknowledgment acknowledgment) {

        log.info("Batch notification consumer — batch size={}", payloads.size());

        for (int i = 0; i < payloads.size(); i++) {
            setupMdc(null, partitions.get(i), offsets.get(i));
            try {
                NotificationEvent event = objectMapper.readValue(payloads.get(i), NotificationEvent.class);
                log.info("Batch item {}/{} id={} channel={} priority={}",
                        i + 1, payloads.size(), event.getNotificationId(), event.getChannel(), event.getPriority());
                dispatch(event);
                markSent(event.getNotificationId());
            } catch (IOException e) {
                log.error("Failed to parse batch notification item {}: {}", i, e.getMessage());
            } catch (Exception e) {
                log.error("Failed to send batch notification item {}: {}", i, e.getMessage());
                markFailed(payloads.get(i), e.getMessage());
            } finally {
                clearMdc();
            }
        }
        acknowledgment.acknowledge();
    }

    private void processAndAck(NotificationEvent event, Acknowledgment acknowledgment) {
        try {
            dispatch(event);
            markSent(event.getNotificationId());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to send notification id={}: {}", event.getNotificationId(), e.getMessage());
            markFailed(event.getNotificationId(), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void dispatch(NotificationEvent event) {
        NotificationModel.Channel channel = NotificationModel.Channel.valueOf(event.getChannel());
        boolean isHtml = NotificationModel.NotificationType.HTML.name().equals(event.getNotificationType());

        switch (channel) {
            case GMAIL -> sendViaGmail(event, isHtml);
            case DISCORD -> sendViaDiscord(event);
            case BOTH -> {
                sendViaGmail(event, isHtml);
                sendViaDiscord(event);
            }
        }
    }

    private void sendViaGmail(NotificationEvent event, boolean isHtml) {
        String to = event.getRecipient() != null ? event.getRecipient() : "prizfroras@gmail.com";
        if (isHtml) {
            gmailService.sendHtmlEmail(to, event.getSubject(), event.getBody());
        } else {
            gmailService.sendEmail(to, event.getSubject(), event.getBody());
        }
        log.info("Sent Gmail notification id={} to={}", event.getNotificationId(), to);
    }

    private void sendViaDiscord(NotificationEvent event) {
        String content = String.format("**%s**\n%s", event.getSubject(), event.getBody());
        discordService.sendMessage(content);
        log.info("Sent Discord notification id={}", event.getNotificationId());
    }

    private void markSent(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setStatus(NotificationModel.Status.SENT);
            n.setSentAt(Instant.now());
            notificationRepository.save(n);
        });
    }

    private void markFailed(String notificationIdOrPayload, String errorMessage) {
        notificationRepository.findById(notificationIdOrPayload).ifPresent(n -> {
            n.setStatus(NotificationModel.Status.FAILED);
            n.setErrorMessage(errorMessage);
            notificationRepository.save(n);
        });
    }

    private void setupMdc(byte[] traceIdBytes, int partition, long offset) {
        MDC.put("traceId", traceIdBytes != null ? new String(traceIdBytes) : "N/A");
        MDC.put("kafkaPartition", String.valueOf(partition));
        MDC.put("kafkaOffset", String.valueOf(offset));
    }

    private void clearMdc() {
        MDC.remove("traceId");
        MDC.remove("kafkaPartition");
        MDC.remove("kafkaOffset");
    }
}
