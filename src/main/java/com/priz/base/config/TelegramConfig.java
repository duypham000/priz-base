package com.priz.base.config;

import com.priz.base.application.integration.telegram.TelegramService;
import com.priz.base.application.integration.telegram.impl.TelegramServiceImpl;
import com.priz.base.config.telegram.TelegramProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(prefix = "telegram", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(TelegramProperties.class)
public class TelegramConfig {

    @Bean
    public TelegramService telegramService(TelegramProperties props) {
        RestClient restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl() + "/bot" + props.getBotToken())
                .build();
        String fileDownloadBaseUrl = props.getBaseUrl() + "/file/bot" + props.getBotToken();
        return new TelegramServiceImpl(restClient, props.getChannelId(), fileDownloadBaseUrl);
    }
}
