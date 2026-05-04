package com.priz.base.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priz.base.application.integration.discord.DiscordService;
import com.priz.base.application.integration.discord.impl.DiscordServiceImpl;
import com.priz.base.config.discord.DiscordProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(prefix = "discord", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(DiscordProperties.class)
public class DiscordConfig {

    @Bean
    public DiscordService discordService(DiscordProperties props, ObjectMapper objectMapper) {
        RestClient apiClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Authorization", "Bot " + props.getBotToken())
                .build();
        RestClient cdnClient = RestClient.builder().build();
        return new DiscordServiceImpl(apiClient, cdnClient, props.getChannelId(), objectMapper);
    }
}
