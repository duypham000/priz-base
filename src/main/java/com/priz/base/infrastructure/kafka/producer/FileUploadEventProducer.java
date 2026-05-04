package com.priz.base.infrastructure.kafka.producer;

import com.priz.base.application.features.files.event.FileUploadEvent;
import com.priz.base.config.kafka.KafkaTopicProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileUploadEventProducer {

    @Qualifier("transactionalKafkaTemplate")
    private final KafkaTemplate<String, Object> transactionalKafkaTemplate;

    private final KafkaTopicProperties topicProperties;

    public void publish(FileUploadEvent event) {
        String topic = topicProperties.getTopic("file-upload");

        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, event.getFileId(), event);
        addHeaders(record, event);

        transactionalKafkaTemplate.executeInTransaction(ops -> {
            var future = ops.send(record);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish FileUploadEvent jobId={} fileId={}: {}",
                            event.getJobId(), event.getFileId(), ex.getMessage());
                } else {
                    log.info("Published FileUploadEvent jobId={} fileId={} partition={} offset={}",
                            event.getJobId(), event.getFileId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
            return null;
        });
    }

    private void addHeaders(ProducerRecord<String, Object> record, FileUploadEvent event) {
        record.headers().add(new RecordHeader("eventType", "FILE_UPLOAD".getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("timestamp", Instant.now().toString().getBytes(StandardCharsets.UTF_8)));
        if (event.getTraceId() != null) {
            record.headers().add(new RecordHeader("traceId", event.getTraceId().getBytes(StandardCharsets.UTF_8)));
        }
        if (event.getUserId() != null) {
            record.headers().add(new RecordHeader("userId", event.getUserId().getBytes(StandardCharsets.UTF_8)));
        }
    }
}
