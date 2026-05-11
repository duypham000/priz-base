package com.priz.base.application.integration.discord.impl;

import com.priz.base.application.integration.discord.DiscordService;
import com.priz.base.application.integration.discord.dto.DiscordEmbed;
import com.priz.base.application.integration.discord.dto.DiscordMessageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "discord", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DiscordServiceNoOpImpl implements DiscordService {

    private static final DiscordMessageResult NOOP = DiscordMessageResult.builder().messageId("").channelId("").content("").build();

    @Override public DiscordMessageResult upload(MultipartFile file, String caption) { log.warn("Discord disabled — skipping upload"); return NOOP; }
    @Override public DiscordMessageResult upload(String fileName, byte[] content, String contentType, String caption) { log.warn("Discord disabled — skipping upload"); return NOOP; }
    @Override public DiscordMessageResult upload(String fileName, InputStream content, long size, String contentType, String caption) { log.warn("Discord disabled — skipping upload"); return NOOP; }
    @Override public DiscordMessageResult uploadWithEmbed(MultipartFile file, DiscordEmbed embed) { log.warn("Discord disabled — skipping uploadWithEmbed"); return NOOP; }
    @Override public DiscordMessageResult sendMessage(String content) { log.warn("Discord disabled — skipping sendMessage: {}", content); return NOOP; }
    @Override public DiscordMessageResult sendEmbed(DiscordEmbed embed) { log.warn("Discord disabled — skipping sendEmbed"); return NOOP; }
    @Override public DiscordMessageResult editMessage(String messageId, String newContent) { return NOOP; }
    @Override public Resource downloadAsResource(String cdnUrl) { throw new UnsupportedOperationException("Discord is disabled"); }
    @Override public DiscordMessageResult getMessageInfo(String messageId) { throw new UnsupportedOperationException("Discord is disabled"); }
    @Override public void deleteMessage(String messageId) { log.warn("Discord disabled — skipping deleteMessage {}", messageId); }
    @Override public boolean isAvailable() { return false; }
}
