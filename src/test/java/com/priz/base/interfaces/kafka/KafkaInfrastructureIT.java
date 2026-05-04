package com.priz.base.interfaces.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.priz.base.application.integration.telegram.TelegramService;
import com.priz.base.application.integration.gmail.GmailService;
import com.priz.base.application.integration.discord.DiscordService;
import com.priz.base.application.features.notification.NotificationService;
import com.priz.base.common.storage.LocalStorageService;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
class KafkaInfrastructureIT {

    @MockitoBean
    private TelegramService telegramService;
    @MockitoBean
    private GmailService gmailService;
    @MockitoBean
    private DiscordService discordService;
    @MockitoBean
    private NotificationService notificationService;
    @MockitoBean
    private LocalStorageService storageService;

    private static final String TOPIC_SUFFIX = String.valueOf(System.currentTimeMillis());
    public static final String INFRA_TOPIC = "infra-test-topic-" + TOPIC_SUFFIX;
    public static final String INFRA_DLT_TOPIC = INFRA_TOPIC + ".DLT";
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

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(TestListener.receivedMessages).contains(message);
        });
    }

    @Test
    void testRetryAndDlt_MechanismWorks() {
        String failingMessage = "fail-me-" + System.currentTimeMillis();
        kafkaTemplate.send(INFRA_TOPIC, failingMessage);

        await().atMost(Duration.ofSeconds(40)).untilAsserted(() -> {
            assertThat(TestListener.retryCount.get()).isGreaterThanOrEqualTo(3);
            assertThat(TestListener.dltReceived.get()).isTrue();
        });
    }

    @Test
    void testBatching_FactoryWorks() {
        for (int i = 0; i < 5; i++) {
            kafkaTemplate.send(BATCH_TOPIC, "batch-" + i + "-" + System.currentTimeMillis());
        }

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(TestListener.batchReceived.get()).isTrue();
        });
    }

    @TestConfiguration
    static class KafkaTestInfraConfig {
        @Bean
        public org.apache.kafka.clients.admin.NewTopic testTopic() {
            return new org.apache.kafka.clients.admin.NewTopic(INFRA_TOPIC, 1, (short) 1);
        }

        @Bean
        public org.apache.kafka.clients.admin.NewTopic testDltTopic() {
            return new org.apache.kafka.clients.admin.NewTopic(INFRA_DLT_TOPIC, 1, (short) 1);
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
        static final java.util.concurrent.atomic.AtomicBoolean dltReceived = new java.util.concurrent.atomic.AtomicBoolean(false);
        static final java.util.concurrent.atomic.AtomicBoolean batchReceived = new java.util.concurrent.atomic.AtomicBoolean(false);

        @KafkaListener(topics = "#{T(com.priz.base.interfaces.kafka.KafkaInfrastructureIT).INFRA_TOPIC}", groupId = "infra-test-group-${random.uuid}")
        public void listen(String message, Acknowledgment ack) {
            log.info("Received raw message: {}", message);
            String cleanMessage = message.replace("\"", "").replace("\\", "");
            receivedMessages.add(cleanMessage);
            if (cleanMessage.startsWith("fail-me")) {
                retryCount.incrementAndGet();
                throw new RuntimeException("Simulated failure for retry/dlt test");
            }
            ack.acknowledge();
        }

        @KafkaListener(topics = "#{T(com.priz.base.interfaces.kafka.KafkaInfrastructureIT).INFRA_DLT_TOPIC}", groupId = "infra-dlt-group-${random.uuid}")
        public void listenDlt(String message) {
            log.info("Received raw DLT message: {}", message);
            String cleanMessage = message.replace("\"", "").replace("\\", "");
            if (cleanMessage.startsWith("fail-me")) {
                dltReceived.set(true);
            }
        }

        @KafkaListener(topics = "#{T(com.priz.base.interfaces.kafka.KafkaInfrastructureIT).BATCH_TOPIC}", groupId = "infra-batch-group-${random.uuid}", containerFactory = "batchContainerFactory")
        public void listenBatch(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
            log.info("Received batch of {} messages", records.size());
            if (!records.isEmpty()) {
                batchReceived.set(true);
            }
            ack.acknowledge();
        }
    }
}
