package com.priz.base.config.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaTopicProperties {

    private Map<String, String> topics;
    private Map<String, Integer> partitions;
    private int replicas = 1;

    public String getTopic(String key) {
        return topics.getOrDefault(key, key);
    }

    public int getPartitions(String key) {
        return partitions.getOrDefault(key, 1);
    }
}
