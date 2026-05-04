package com.priz.base.infrastructure.kafka.producer;

import com.priz.base.application.features.notification.event.NotificationEvent;
import com.priz.base.config.kafka.KafkaTopicProperties;
import com.priz.base.domain.mysql.priz_base.model.NotificationModel;
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
public class NotificationEventProducer {

    @Qualifier("kafkaTemplate")
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final KafkaTopicProperties topicProperties;

    public void publish(NotificationEvent event) {
        String topic = topicProperties.getTopic("notification");
        int partition = resolvePartition(event.getPriority());

        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, partition, event.getNotificationId(), event);
        addHeaders(record, event);

        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish NotificationEvent id={} channel={} priority={}: {}",
                        event.getNotificationId(), event.getChannel(), event.getPriority(), ex.getMessage());
            } else {
                log.info("Published NotificationEvent id={} channel={} priority={} partition={} offset={}",
                        event.getNotificationId(), event.getChannel(), event.getPriority(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    private int resolvePartition(String priority) {
        if (priority == null) return 1;
        return switch (NotificationModel.Priority.valueOf(priority)) {
            case HIGH -> 0;
            case NORMAL -> 1;
            case LOW -> 2;
        };
    }

    private void addHeaders(ProducerRecord<String, Object> record, NotificationEvent event) {
        record.headers().add(new RecordHeader("eventType", "NOTIFICATION".getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("timestamp", Instant.now().toString().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("priority", event.getPriority().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("channel", event.getChannel().getBytes(StandardCharsets.UTF_8)));
        if (event.getTraceId() != null) {
            record.headers().add(new RecordHeader("traceId", event.getTraceId().getBytes(StandardCharsets.UTF_8)));
        }
        if (event.getUserId() != null) {
            record.headers().add(new RecordHeader("userId", event.getUserId().getBytes(StandardCharsets.UTF_8)));
        }
    }
}
