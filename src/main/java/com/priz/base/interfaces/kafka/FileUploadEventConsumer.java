package com.priz.base.interfaces.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priz.base.application.features.files.event.FileUploadEvent;
import com.priz.base.application.features.notification.NotificationService;
import com.priz.base.application.features.notification.dto.SendNotificationRequest;
import com.priz.base.application.integration.telegram.TelegramService;
import com.priz.base.application.integration.telegram.dto.TelegramUploadResult;
import com.priz.base.common.storage.LocalStorageService;
import com.priz.base.domain.mysql.priz_base.model.FileUploadJobModel;
import com.priz.base.domain.mysql.priz_base.model.NotificationModel;
import com.priz.base.domain.mysql.priz_base.repository.FileUploadJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileUploadEventConsumer {

    private final FileUploadJobRepository jobRepository;
    private final LocalStorageService storageService;
    private final TelegramService telegramService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.file-upload}",
            groupId = "priz-file-upload-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(name = "traceId", required = false) byte[] traceIdBytes,
            @Header(name = "userId", required = false) byte[] userIdBytes,
            Acknowledgment acknowledgment) {

        String traceId = traceIdBytes != null ? new String(traceIdBytes) : "N/A";
        String userId = userIdBytes != null ? new String(userIdBytes) : "N/A";

        MDC.put("traceId", traceId);
        MDC.put("userId", userId);
        MDC.put("kafkaPartition", String.valueOf(partition));
        MDC.put("kafkaOffset", String.valueOf(offset));

        try {
            FileUploadEvent event = objectMapper.readValue(payload, FileUploadEvent.class);
            log.info("Processing FileUploadEvent jobId={} fileId={} partition={} offset={}",
                    event.getJobId(), event.getFileId(), partition, offset);

            FileUploadJobModel job = jobRepository.findById(event.getJobId()).orElse(null);
            if (job == null) {
                log.warn("Job not found jobId={} — skipping", event.getJobId());
                acknowledgment.acknowledge();
                return;
            }

            job.setStatus(FileUploadJobModel.Status.UPLOADING);
            job.setKafkaPartition(partition);
            job.setKafkaOffset(offset);
            jobRepository.save(job);

            TelegramUploadResult result = uploadToTelegram(event);

            job.setStatus(FileUploadJobModel.Status.COMPLETED);
            job.setTelegramUrl(result.getDownloadUrl());
            job.setTelegramMessageId(result.getMessageId());
            jobRepository.save(job);

            log.info("Upload COMPLETED jobId={} fileId={} telegramMsgId={}",
                    event.getJobId(), event.getFileId(), result.getMessageId());

            acknowledgment.acknowledge();

            notifySuccess(event, result);

        } catch (IOException e) {
            log.error("Failed to parse FileUploadEvent payload: {}", e.getMessage());
            acknowledgment.acknowledge();
        } finally {
            MDC.remove("traceId");
            MDC.remove("userId");
            MDC.remove("kafkaPartition");
            MDC.remove("kafkaOffset");
        }
    }

    private TelegramUploadResult uploadToTelegram(FileUploadEvent event) {
        try {
            java.nio.file.Path filePath = storageService.getRootLocation()
                    .resolve(event.getStoredName()).normalize();
            try (InputStream inputStream = Files.newInputStream(filePath)) {
                long fileSize = Files.size(filePath);
                return telegramService.upload(
                        event.getOriginalName(),
                        inputStream,
                        fileSize,
                        event.getContentType(),
                        "Uploaded by user " + event.getUserId()
                );
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file for Telegram upload: " + event.getStoredName(), e);
        }
    }

    private void notifySuccess(FileUploadEvent event, TelegramUploadResult result) {
        try {
            SendNotificationRequest request = SendNotificationRequest.builder()
                    .channel(NotificationModel.Channel.DISCORD)
                    .notificationType(NotificationModel.NotificationType.PLAIN_TEXT)
                    .priority(NotificationModel.Priority.NORMAL)
                    .subject("File Upload Completed")
                    .body(String.format("File '%s' uploaded successfully to Telegram. URL: %s",
                            event.getOriginalName(), result.getDownloadUrl()))
                    .build();
            notificationService.send(request);
        } catch (Exception e) {
            log.warn("Failed to send upload completion notification: {}", e.getMessage());
        }
    }
}
