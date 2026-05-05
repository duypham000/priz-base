package com.priz.base.interfaces.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priz.base.application.features.files.event.FileProcessEvent;
import com.priz.base.application.features.notification.NotificationService;
import com.priz.base.application.features.notification.dto.SendNotificationRequest;
import com.priz.base.application.integration.telegram.TelegramService;
import com.priz.base.application.integration.telegram.dto.TelegramUploadResult;
import com.priz.base.common.storage.LocalStorageService;
import com.priz.base.domain.elasticsearch.file.document.FileDocument;
import com.priz.base.domain.elasticsearch.file.repository.FileDocumentRepository;
import com.priz.base.domain.mysql_priz_base.model.FileUploadJobModel;
import com.priz.base.domain.mysql_priz_base.model.NotificationModel;
import com.priz.base.domain.mysql_priz_base.repository.FileUploadJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileProcessEventConsumer {

    private final FileUploadJobRepository jobRepository;
    private final LocalStorageService storageService;
    private final TelegramService telegramService;
    private final NotificationService notificationService;
    private final FileDocumentRepository fileDocumentRepository;
    private final ObjectMapper objectMapper;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000L, multiplier = 2.0),
            autoCreateTopics = "true",
            dltTopicSuffix = "-dlt",
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {RuntimeException.class},
            listenerContainerFactory = "retryableContainerFactory"
    )
    @KafkaListener(
            topics = "${app.kafka.topics.file-process}",
            groupId = "priz-file-process-group",
            containerFactory = "retryableContainerFactory"
    )
    @Transactional
    public void consume(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(name = "traceId", required = false) byte[] traceIdBytes,
            @Header(name = "userId", required = false) byte[] userIdBytes) {

        String traceId = traceIdBytes != null ? new String(traceIdBytes) : "N/A";
        String userId = userIdBytes != null ? new String(userIdBytes) : "N/A";

        MDC.put("traceId", traceId);
        MDC.put("userId", userId);
        MDC.put("kafkaPartition", String.valueOf(partition));
        MDC.put("kafkaOffset", String.valueOf(offset));

        try {
            FileProcessEvent event = objectMapper.readValue(payload, FileProcessEvent.class);
            log.info("Processing FileProcessEvent operation={} fileId={} partition={} offset={}",
                    event.getOperation(), event.getFileId(), partition, offset);

            switch (event.getOperation()) {
                case UPLOAD -> handleUpload(event, partition, offset);
                case SYNC -> handleSync(event, partition, offset);
                case DELETE -> handleDelete(event);
                case UPDATE -> handleUpdate(event);
            }

        } catch (IOException e) {
            log.error("Failed to parse FileProcessEvent payload: {}", e.getMessage());
        } finally {
            MDC.remove("traceId");
            MDC.remove("userId");
            MDC.remove("kafkaPartition");
            MDC.remove("kafkaOffset");
        }
    }

    private void handleUpload(FileProcessEvent event, int partition, long offset) {
        FileUploadJobModel job = jobRepository.findById(event.getJobId()).orElseThrow();
        job.setStatus(FileUploadJobModel.Status.UPLOADING);
        job.setKafkaPartition(partition);
        job.setKafkaOffset(offset);
        jobRepository.save(job);

        TelegramUploadResult result = uploadLocalToTelegram(event);

        job.setStatus(FileUploadJobModel.Status.COMPLETED);
        job.setTelegramUrl(result.getDownloadUrl());
        job.setTelegramMessageId(result.getMessageId());
        jobRepository.save(job);

        indexIfNecessary(event);
        notifySuccess(event, result.getDownloadUrl());
    }

    private void handleSync(FileProcessEvent event, int partition, long offset) {
        FileUploadJobModel job = jobRepository.findById(event.getJobId()).orElseThrow();
        job.setStatus(FileUploadJobModel.Status.UPLOADING);
        job.setKafkaPartition(partition);
        job.setKafkaOffset(offset);
        jobRepository.save(job);

        TelegramUploadResult result = uploadRemoteToTelegram(event);

        job.setStatus(FileUploadJobModel.Status.COMPLETED);
        job.setTelegramUrl(result.getDownloadUrl());
        job.setTelegramMessageId(result.getMessageId());
        jobRepository.save(job);

        indexIfNecessary(event);
        notifySuccess(event, result.getDownloadUrl());
    }

    private void handleDelete(FileProcessEvent event) {
        fileDocumentRepository.deleteById(event.getFileId());
        log.info("Deleted ES index for fileId={}", event.getFileId());
    }

    private void handleUpdate(FileProcessEvent event) {
        indexIfNecessary(event);
        log.info("Updated ES index for fileId={}", event.getFileId());
    }

    private void indexIfNecessary(FileProcessEvent event) {
        String extension = getFileExtension(event.getOriginalName());
        if (isIndexable(event.getContentType(), extension)) {
            FileDocument doc = FileDocument.builder()
                    .id(event.getFileId())
                    .originalName(event.getOriginalName())
                    .content(event.getFileContent())
                    .description(event.getDescription())
                    .userId(event.getUserId())
                    .fileType(extension)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            fileDocumentRepository.save(doc);
            log.info("Indexed fileId={} to Elasticsearch", event.getFileId());
        }
    }

    private TelegramUploadResult uploadLocalToTelegram(FileProcessEvent event) {
        try {
            java.nio.file.Path filePath = storageService.getRootLocation()
                    .resolve(event.getStoredName()).normalize();
            try (InputStream inputStream = Files.newInputStream(filePath)) {
                return telegramService.upload(
                        event.getOriginalName(),
                        inputStream,
                        event.getFileSize(),
                        event.getContentType(),
                        "Uploaded by user " + event.getUserId()
                );
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read local file for Telegram: " + event.getStoredName(), e);
        }
    }

    private TelegramUploadResult uploadRemoteToTelegram(FileProcessEvent event) {
        try (InputStream inputStream = new URL(event.getSourceUrl()).openStream()) {
            return telegramService.upload(
                    event.getOriginalName(),
                    inputStream,
                    event.getFileSize() != null ? event.getFileSize() : 0,
                    event.getContentType(),
                    "Synced from " + event.getSourceUrl()
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to download remote file for Telegram: " + event.getSourceUrl(), e);
        }
    }

    private void notifySuccess(FileProcessEvent event, String telegramUrl) {
        try {
            SendNotificationRequest request = SendNotificationRequest.builder()
                    .channel(NotificationModel.Channel.DISCORD)
                    .notificationType(NotificationModel.NotificationType.PLAIN_TEXT)
                    .priority(NotificationModel.Priority.NORMAL)
                    .subject("File Processing Completed")
                    .body(String.format("File '%s' processed successfully. Operation: %s. URL: %s",
                            event.getOriginalName(), event.getOperation(), telegramUrl))
                    .build();
            notificationService.send(request);
        } catch (Exception e) {
            log.warn("Failed to send completion notification: {}", e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".") + 1);
        }
        return "";
    }

    private boolean isIndexable(String contentType, String extension) {
        return "txt".equalsIgnoreCase(extension) || "md".equalsIgnoreCase(extension)
                || "text/plain".equalsIgnoreCase(contentType) || "text/markdown".equalsIgnoreCase(contentType);
    }

    @DltHandler
    @Transactional
    public void handleDlt(
            @Payload String payload,
            @Header(name = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage) {
        log.error("DLT received — error={}", exceptionMessage);
        try {
            FileProcessEvent event = objectMapper.readValue(payload, FileProcessEvent.class);
            if (event.getJobId() != null) {
                jobRepository.findById(event.getJobId()).ifPresent(job -> {
                    job.setStatus(FileUploadJobModel.Status.FAILED);
                    job.setErrorMessage(exceptionMessage);
                    jobRepository.save(job);
                });
            }
        } catch (IOException e) {
            log.error("Could not parse DLT payload: {}", e.getMessage());
        }
    }
}
