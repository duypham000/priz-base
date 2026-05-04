package com.priz.base.config.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnClass(MeterRegistry.class)
public class KafkaMetricsConfig {

    // Spring Boot auto-configuration binds Kafka JMX metrics to Micrometer automatically
    // when spring-kafka and spring-boot-starter-actuator are both on the classpath.
    // Metrics are exposed at /actuator/metrics with prefix kafka.producer.* and kafka.consumer.*

    public KafkaMetricsConfig(MeterRegistry meterRegistry) {
        log.info("Kafka metrics registered — available at /actuator/metrics");
    }
}
