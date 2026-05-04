package com.priz.base.config.discord;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("discord")
public class DiscordProperties {

    private boolean enabled = false;
    private String botToken;
    private String channelId;
    private String baseUrl = "https://discord.com/api/v10";
}
