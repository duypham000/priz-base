package com.priz.base.config.telegram;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("telegram")
public class TelegramProperties {

    private boolean enabled = false;
    private String botToken;
    private String channelId;
    private String baseUrl = "https://api.telegram.org";
}
