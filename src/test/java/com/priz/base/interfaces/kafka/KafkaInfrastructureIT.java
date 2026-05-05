package com.priz.base.interfaces.kafka;

import com.priz.base.application.features.notification.NotificationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class KafkaInfrastructureIT extends BaseKafkaIT {

    @MockitoBean
    private NotificationService notificationService;

    private static final String TOPIC_SUFFIX = String.valueOf(System.currentTimeMillis());
    public static final String INFRA_TOPIC = "infra-test-topic-" + TOPIC_SUFFIX;
    public static final String INFRA_DLT_TOPIC = INFRA_TOPIC + "-dlt";
    public static final String BATCH_TOPIC = "infra-batch-topic-" + TOPIC_SUFFIX;

    private static final Logger log = LoggerFactory.getLogger(KafkaInfrastructureIT.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        TestListener.receivedMessages.clear();
        TestListener.retryCount.set(0);
        TestListener.dltReceived.set(false);
        TestListener.batchReceived.set(false);
    }

    @Test
    void testRoundTrip_MessagingWorks() {
        String message = "Hello Kafka " + System.currentTimeMillis();
        kafkaTemplate.send(INFRA_TOPIC, message);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(TestListener.receivedMessages).contains(message));
    }

    @Test
    void testRetryAndDlt_MechanismWorks() {
        String failingMessage = "fail-me-" + System.currentTimeMillis();
        kafkaTemplate.send(INFRA_TOPIC, failingMessage);

        // Non-blocking retries: attempts=4 (1 original + 3 retry topics), delay=100ms
        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> {
            assertThat(TestListener.retryCount.get()).isGreaterThanOrEqualTo(4);
            assertThat(TestListener.dltReceived.get()).isTrue();
        });
    }

    @Test
    void testBatching_FactoryWorks() {
        for (int i = 0; i < 5; i++) {
            kafkaTemplate.send(BATCH_TOPIC, "batch-" + i + "-" + System.currentTimeMillis());
        }

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(TestListener.batchReceived.get()).isTrue());
    }

    @TestConfiguration
    static class KafkaTestInfraConfig {
        @Bean
        public org.apache.kafka.clients.admin.NewTopic testTopic() {
            return new org.apache.kafka.clients.admin.NewTopic(INFRA_TOPIC, 1, (short) 1);
        }

        @Bean
        public org.apache.kafka.clients.admin.NewTopic testBatchTopic() {
            return new org.apache.kafka.clients.admin.NewTopic(BATCH_TOPIC, 1, (short) 1);
        }

        @Bean
        public TestListener testListener() {
            return new TestListener();
        }
    }

    static class TestListener {
        static final List<String> receivedMessages = new CopyOnWriteArrayList<>();
        static final AtomicInteger retryCount = new AtomicInteger(0);
        static final AtomicBoolean dltReceived = new AtomicBoolean(false);
        static final AtomicBoolean batchReceived = new AtomicBoolean(false);

        @RetryableTopic(
                attempts = "4",
                backOff = @BackOff(delay = 100L),
                autoCreateTopics = "true",
                dltTopicSuffix = "-dlt",
                dltStrategy = DltStrategy.FAIL_ON_ERROR,
                include = {RuntimeException.class},
                listenerContainerFactory = "retryableContainerFactory"
        )
        @KafkaListener(
                topics = "#{T(com.priz.base.interfaces.kafka.KafkaInfrastructureIT).INFRA_TOPIC}",
                groupId = "infra-test-group-${random.uuid}",
                containerFactory = "retryableContainerFactory"
        )
        public void listen(String message) {
            log.info("Received raw message: {}", message);
            String cleanMessage = message.replace("\"", "").replace("\\", "");
            receivedMessages.add(cleanMessage);
            if (cleanMessage.startsWith("fail-me")) {
                retryCount.incrementAndGet();
                throw new RuntimeException("Simulated failure for retry/dlt test");
            }
        }

        @DltHandler
        public void listenDlt(String message) {
            log.info("Received DLT message: {}", message);
            String cleanMessage = message.replace("\"", "").replace("\\", "");
            if (cleanMessage.startsWith("fail-me")) {
                dltReceived.set(true);
            }
        }

        @KafkaListener(
                topics = "#{T(com.priz.base.interfaces.kafka.KafkaInfrastructureIT).BATCH_TOPIC}",
                groupId = "infra-batch-group-${random.uuid}",
                containerFactory = "batchContainerFactory"
        )
        public void listenBatch(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
            log.info("Received batch of {} messages", records.size());
            if (!records.isEmpty()) {
                batchReceived.set(true);
            }
            ack.acknowledge();
        }
    }
}
