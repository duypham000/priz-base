package com.priz.base.application.integration.discord.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DiscordMessageResult {

    /** Discord snowflake ID of the created/updated message. */
    private String messageId;

    private String channelId;

    /** Plain-text content of the message (empty for embed-only messages). */
    private String content;

    @Builder.Default
    private List<DiscordAttachment> attachments = List.of();
}
