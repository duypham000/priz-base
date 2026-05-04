package com.priz.base.config.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.TopicDescription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "app.kafka.topics.file-upload=file-upload-events",
        "app.kafka.topics.file-upload-dlt=file-upload-events.DLT",
        "app.kafka.topics.notification=notification-events",
        "app.kafka.topics.notification-dlt=notification-events.DLT",
        "app.kafka.partitions.file-upload=3",
        "app.kafka.partitions.notification=3",
        "app.kafka.replicas=1",
        "telegram.enabled=false",
        "discord.enabled=false",
        "gmail.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class KafkaConfigTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private KafkaTopicProperties topicProperties;

    @Test
    void kafkaTemplate_IsNotNull() {
        assertThat(kafkaTemplate).isNotNull();
    }

    @Test
    void topicProperties_LoadsCorrectTopicNames() {
        assertThat(topicProperties.getTopic("file-upload")).isEqualTo("file-upload-events");
        assertThat(topicProperties.getTopic("notification")).isEqualTo("notification-events");
        assertThat(topicProperties.getTopic("file-upload-dlt")).isEqualTo("file-upload-events.DLT");
        assertThat(topicProperties.getTopic("notification-dlt")).isEqualTo("notification-events.DLT");
    }

    @Test
    void topics_AreCreatedWithCorrectPartitions() throws Exception {
        String brokers = "localhost:9092";

        try (AdminClient admin = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, brokers))) {

            Set<String> topics = admin.listTopics().names().get();
            assertThat(topics).contains(
                    "file-upload-events",
                    "file-upload-events.DLT",
                    "notification-events",
                    "notification-events.DLT"
            );

            Map<String, TopicDescription> descriptions = admin.describeTopics(
                    Set.of("file-upload-events", "notification-events")).allTopicNames().get();

            assertThat(descriptions.get("file-upload-events").partitions()).hasSize(3);
            assertThat(descriptions.get("notification-events").partitions()).hasSize(3);
        }
    }
}
