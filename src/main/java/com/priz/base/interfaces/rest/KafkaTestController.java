package com.priz.base.interfaces.rest;

import com.priz.base.application.features.files.event.FileProcessEvent;
import com.priz.base.application.features.notification.event.NotificationEvent;
import com.priz.base.infrastructure.kafka.producer.FileProcessEventProducer;
import com.priz.base.infrastructure.kafka.producer.NotificationEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/test/kafka")
@RequiredArgsConstructor
public class KafkaTestController {

    private final FileProcessEventProducer fileProcessEventProducer;
    private final NotificationEventProducer notificationEventProducer;

    @PostMapping("/file-upload")
    public ResponseEntity<String> testFileUpload(@RequestParam(defaultValue = "test-file.txt") String fileName) {
        String jobId = UUID.randomUUID().toString();
        FileProcessEvent event = FileProcessEvent.builder()
                .jobId(jobId)
                .fileId(UUID.randomUUID().toString())
                .userId("test-user-1")
                .originalName(fileName)
                .storedName("stored-" + fileName)
                .contentType("text/plain")
                .fileSize(1024L)
                .fileContent("This is a test content for indexing.")
                .operation(FileProcessEvent.OperationType.UPLOAD)
                .traceId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .build();

        log.info("Triggering test FileProcessEvent: {}", event);
        fileProcessEventProducer.publish(event);
        return ResponseEntity.ok("FileProcessEvent (UPLOAD) triggered with jobId: " + jobId);
    }

    @PostMapping("/notification")
    public ResponseEntity<String> testNotification(@RequestParam(defaultValue = "Hello Kafka") String message) {
        String notificationId = UUID.randomUUID().toString();
        NotificationEvent event = NotificationEvent.builder()
                .notificationId(notificationId)
                .channel("DISCORD")
                .notificationType("TEST")
                .priority("HIGH")
                .subject("Kafka Test Notification")
                .body(message)
                .recipient("test-recipient")
                .traceId(UUID.randomUUID().toString())
                .userId("test-user-1")
                .build();

        log.info("Triggering test NotificationEvent: {}", event);
        notificationEventProducer.publish(event);
        return ResponseEntity.ok("NotificationEvent triggered with notificationId: " + notificationId);
    }
}
