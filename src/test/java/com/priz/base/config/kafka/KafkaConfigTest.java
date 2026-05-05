package com.priz.base.config.kafka;

import com.priz.base.interfaces.kafka.BaseKafkaIT;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.TopicDescription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.test.context.TestPropertySource;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestPropertySource(properties = {
        "kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "app.kafka.topics.file-upload=file-upload-events",
        "app.kafka.topics.notification=notification-events",
        "app.kafka.partitions.file-upload=3",
        "app.kafka.partitions.notification=3",
        "app.kafka.replicas=1",
        "telegram.enabled=false",
        "discord.enabled=false",
        "gmail.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class KafkaConfigTest extends BaseKafkaIT {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private KafkaTopicProperties topicProperties;

    @Autowired
    private ConcurrentKafkaListenerContainerFactory<String, Object> retryableContainerFactory;

    @Test
    void kafkaTemplate_IsNotNull() {
        assertThat(kafkaTemplate).isNotNull();
    }

    @Test
    void topicProperties_LoadsCorrectTopicNames() {
        assertThat(topicProperties.getTopic("file-upload")).isEqualTo("file-upload-events");
        assertThat(topicProperties.getTopic("notification")).isEqualTo("notification-events");
    }

    @Test
    void retryableContainerFactory_IsNotNull() {
        assertThat(retryableContainerFactory).isNotNull();
    }

    @Test
    void retryableContainerFactory_HasRecordAckMode() {
        assertThat(retryableContainerFactory.getContainerProperties().getAckMode())
                .isEqualTo(ContainerProperties.AckMode.RECORD);
    }

    @Test
    void topics_AreCreatedWithCorrectPartitions() throws Exception {
        String brokers = "localhost:9092";

        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("localhost", 9092), 1000);
        } catch (Exception e) {
            assumeTrue(false, "Kafka not available at localhost:9092 — skipping");
        }

        try (AdminClient admin = AdminClient.create(
                Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, brokers))) {

            Set<String> topics = admin.listTopics().names().get();
            assertThat(topics).contains("file-upload-events", "notification-events");

            Map<String, TopicDescription> descriptions = admin.describeTopics(
                    Set.of("file-upload-events", "notification-events")).allTopicNames().get();

            assertThat(descriptions.get("file-upload-events").partitions()).hasSize(3);
            assertThat(descriptions.get("notification-events").partitions()).hasSize(3);
        }
    }
}
