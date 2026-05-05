package com.priz.base.infrastructure.kafka.producer;

import com.priz.base.application.features.files.event.FileProcessEvent;
import com.priz.base.config.kafka.KafkaTopicProperties;
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
public class FileProcessEventProducer {

    private final KafkaTemplate<String, Object> transactionalKafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public FileProcessEventProducer(
            @Qualifier("transactionalKafkaTemplate") KafkaTemplate<String, Object> transactionalKafkaTemplate,
            KafkaTopicProperties topicProperties) {
        this.transactionalKafkaTemplate = transactionalKafkaTemplate;
        this.topicProperties = topicProperties;
    }

    public void publish(FileProcessEvent event) {
        String topic = topicProperties.getTopic("file-process");

        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, event.getFileId(), event);
        addHeaders(record, event);

        transactionalKafkaTemplate.executeInTransaction(ops -> {
            var future = ops.send(record);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish FileProcessEvent operation={} fileId={}: {}",
                            event.getOperation(), event.getFileId(), ex.getMessage());
                } else {
                    log.info("Published FileProcessEvent operation={} fileId={} partition={} offset={}",
                            event.getOperation(), event.getFileId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
            return null;
        });
    }

    private void addHeaders(ProducerRecord<String, Object> record, FileProcessEvent event) {
        record.headers().add(new RecordHeader("eventType", "FILE_PROCESS".getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("operation", event.getOperation().name().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("timestamp", Instant.now().toString().getBytes(StandardCharsets.UTF_8)));
        if (event.getTraceId() != null) {
            record.headers().add(new RecordHeader("traceId", event.getTraceId().getBytes(StandardCharsets.UTF_8)));
        }
        if (event.getUserId() != null) {
            record.headers().add(new RecordHeader("userId", event.getUserId().getBytes(StandardCharsets.UTF_8)));
        }
    }
}
