package com.priz.base.application.integration.discord.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DiscordAttachment {

    private String id;
    private String filename;
    private long size;

    /** Permanent CDN URL — does not expire. */
    private String url;

    @JsonProperty("proxy_url")
    private String proxyUrl;

    @JsonProperty("content_type")
    private String contentType;
}
