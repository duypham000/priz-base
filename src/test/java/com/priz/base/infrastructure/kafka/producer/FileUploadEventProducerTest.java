package com.priz.base.infrastructure.kafka.producer;

import com.priz.base.application.features.files.event.FileUploadEvent;
import com.priz.base.config.kafka.KafkaTopicProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileUploadEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> transactionalKafkaTemplate;
    @Mock
    private KafkaTopicProperties topicProperties;

    @InjectMocks
    private FileUploadEventProducer producer;

    @BeforeEach
    void setUp() {
        when(topicProperties.getTopic("file-upload")).thenReturn("file-upload-events");

        RecordMetadata meta = new RecordMetadata(new TopicPartition("file-upload-events", 0), 0, 0, 0, 0, 0);
        SendResult<String, Object> sendResult = new SendResult<>(new ProducerRecord<>("file-upload-events", "key", "value"), meta);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(transactionalKafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        when(transactionalKafkaTemplate.executeInTransaction(any())).thenAnswer(invocation -> {
            KafkaOperations.OperationsCallback<String, Object, ?> callback = invocation.getArgument(0);
            return callback.doInOperations(transactionalKafkaTemplate);
        });
    }

    @Test
    void publish_ValidEvent_ExecutesInTransaction() {
        // Arrange
        FileUploadEvent event = FileUploadEvent.builder()
                .jobId("job-1")
                .fileId("file-1")
                .userId("user-1")
                .storedName("uuid.pdf")
                .originalName("document.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .traceId("trace-abc")
                .build();

        // Act
        producer.publish(event);

        // Assert
        verify(transactionalKafkaTemplate).executeInTransaction(any());
    }

    @Test
    void publish_UsesCorrectTopic() {
        // Arrange
        when(topicProperties.getTopic("file-upload")).thenReturn("custom-file-topic");
        when(transactionalKafkaTemplate.executeInTransaction(any())).thenReturn(null);

        FileUploadEvent event = FileUploadEvent.builder()
                .jobId("job-2")
                .fileId("file-2")
                .userId("user-2")
                .storedName("file.png")
                .originalName("photo.png")
                .contentType("image/png")
                .build();

        // Act
        producer.publish(event);

        // Assert
        verify(topicProperties).getTopic("file-upload");
    }
}
