package com.priz.base.interfaces.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priz.base.application.features.files.event.FileUploadEvent;
import com.priz.base.application.features.notification.NotificationService;
import com.priz.base.application.features.notification.dto.SendNotificationRequest;
import com.priz.base.domain.mysql.priz_base.model.FileUploadJobModel;
import com.priz.base.domain.mysql.priz_base.model.NotificationModel;
import com.priz.base.domain.mysql.priz_base.repository.FileUploadJobRepository;
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
public class FileUploadDltConsumer {

    private final FileUploadJobRepository jobRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.file-upload-dlt}",
            groupId = "priz-file-upload-dlt-group",
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

        log.error("DLT received — originalTopic={} partition={} offset={} error={}",
                originalTopicBytes != null ? new String(originalTopicBytes) : "unknown",
                originalPartition, originalOffset, exceptionMessage);

        try {
            FileUploadEvent event = objectMapper.readValue(payload, FileUploadEvent.class);

            jobRepository.findById(event.getJobId()).ifPresent(job -> {
                job.setStatus(FileUploadJobModel.Status.FAILED);
                job.setErrorMessage(truncate(exceptionMessage, 1000));
                jobRepository.save(job);
                log.error("Marked job FAILED jobId={} fileId={}", event.getJobId(), event.getFileId());
            });

            sendFailureAlert(event, exceptionMessage);

        } catch (IOException e) {
            log.error("Could not parse DLT payload: {}", e.getMessage());
        }

        acknowledgment.acknowledge();
    }

    private void sendFailureAlert(FileUploadEvent event, String errorMessage) {
        try {
            SendNotificationRequest request = SendNotificationRequest.builder()
                    .channel(NotificationModel.Channel.GMAIL)
                    .notificationType(NotificationModel.NotificationType.HTML)
                    .priority(NotificationModel.Priority.HIGH)
                    .subject("[ALERT] File Upload Failed")
                    .body(String.format(
                            "<h3>File upload failed after all retries</h3>"
                                    + "<p><b>Job ID:</b> %s</p>"
                                    + "<p><b>File:</b> %s</p>"
                                    + "<p><b>User:</b> %s</p>"
                                    + "<p><b>Error:</b> %s</p>",
                            event.getJobId(), event.getOriginalName(),
                            event.getUserId(), errorMessage))
                    .build();
            notificationService.send(request);
        } catch (Exception e) {
            log.warn("Failed to send DLT alert notification: {}", e.getMessage());
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
