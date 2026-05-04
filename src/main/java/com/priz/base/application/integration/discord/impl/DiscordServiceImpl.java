package com.priz.base.application.integration.discord.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.priz.base.application.integration.discord.DiscordService;
import com.priz.base.application.integration.discord.dto.DiscordAttachment;
import com.priz.base.application.integration.discord.dto.DiscordEmbed;
import com.priz.base.application.integration.discord.dto.DiscordMessageResult;
import com.priz.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Slf4j
public class DiscordServiceImpl implements DiscordService {

    private final RestClient apiClient;
    private final RestClient cdnClient;
    private final String channelId;
    private final ObjectMapper objectMapper;

    public DiscordServiceImpl(RestClient apiClient, RestClient cdnClient,
                              String channelId, ObjectMapper objectMapper) {
        this.apiClient = apiClient;
        this.cdnClient = cdnClient;
        this.channelId = channelId;
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Upload
    // -------------------------------------------------------------------------

    @Override
    public DiscordMessageResult upload(MultipartFile file, String caption) {
        log.debug("Discord upload: file={} size={}", file.getOriginalFilename(), file.getSize());
        try {
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            return doUpload(fileResource, buildPayloadJson(caption));
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to read file for upload: " + file.getOriginalFilename());
        }
    }

    @Override
    public DiscordMessageResult upload(String fileName, byte[] content, String contentType, String caption) {
        log.debug("Discord upload bytes: file={} size={}", fileName, content.length);
        ByteArrayResource fileResource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
        return doUpload(fileResource, buildPayloadJson(caption));
    }

    @Override
    public DiscordMessageResult upload(String fileName, InputStream content, long size, String contentType, String caption) {
        log.debug("Discord upload stream: file={} size={}", fileName, size);
        InputStreamResource streamResource = new InputStreamResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }

            @Override
            public long contentLength() {
                return size;
            }
        };
        return doUpload(streamResource, buildPayloadJson(caption));
    }

    @Override
    public DiscordMessageResult uploadWithEmbed(MultipartFile file, DiscordEmbed embed) {
        log.debug("Discord upload with embed: file={}", file.getOriginalFilename());
        try {
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            return doUpload(fileResource, buildPayloadJson(embed));
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to read file for upload: " + file.getOriginalFilename());
        }
    }

    private DiscordMessageResult doUpload(Resource fileResource, String payloadJson) {
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("payload_json", new HttpEntity<>(payloadJson, jsonHeaders));
        body.add("files[0]", fileResource);

        try {
            Map<String, Object> raw = apiClient.post()
                    .uri("/channels/{channelId}/messages", channelId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return parseMessageResponse(raw);
        } catch (RestClientException e) {
            log.error("Discord upload failed: {}", e.getMessage());
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Discord upload failed: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Messaging
    // -------------------------------------------------------------------------

    @Override
    public DiscordMessageResult sendMessage(String content) {
        log.debug("Discord sendMessage: channelId={}", channelId);
        try {
            Map<String, Object> raw = apiClient.post()
                    .uri("/channels/{channelId}/messages", channelId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("content", content))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return parseMessageResponse(raw);
        } catch (RestClientException e) {
            log.error("Discord sendMessage failed: {}", e.getMessage());
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Discord sendMessage failed: " + e.getMessage());
        }
    }

    @Override
    public DiscordMessageResult sendEmbed(DiscordEmbed embed) {
        log.debug("Discord sendEmbed: channelId={}", channelId);
        try {
            Map<String, Object> body = Map.of("content", "", "embeds", List.of(embed));
            Map<String, Object> raw = apiClient.post()
                    .uri("/channels/{channelId}/messages", channelId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return parseMessageResponse(raw);
        } catch (RestClientException e) {
            log.error("Discord sendEmbed failed: {}", e.getMessage());
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Discord sendEmbed failed: " + e.getMessage());
        }
    }

    @Override
    public DiscordMessageResult editMessage(String messageId, String newContent) {
        log.debug("Discord editMessage: messageId={}", messageId);
        try {
            Map<String, Object> raw = apiClient.patch()
                    .uri("/channels/{channelId}/messages/{messageId}", channelId, messageId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("content", newContent))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return parseMessageResponse(raw);
        } catch (RestClientException e) {
            log.error("Discord editMessage failed: messageId={}", messageId, e);
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Discord editMessage failed: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Download
    // -------------------------------------------------------------------------

    @Override
    public Resource downloadAsResource(String cdnUrl) {
        log.debug("Discord download: url={}", cdnUrl);
        try {
            byte[] bytes = cdnClient.get()
                    .uri(cdnUrl)
                    .retrieve()
                    .body(byte[].class);
            return new ByteArrayResource(bytes != null ? bytes : new byte[0]);
        } catch (RestClientException e) {
            log.error("Discord download failed: url={}", cdnUrl, e);
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "Failed to download from Discord CDN: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    @Override
    public DiscordMessageResult getMessageInfo(String messageId) {
        log.debug("Discord getMessageInfo: messageId={}", messageId);
        try {
            Map<String, Object> raw = apiClient.get()
                    .uri("/channels/{channelId}/messages/{messageId}", channelId, messageId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return parseMessageResponse(raw);
        } catch (RestClientException e) {
            log.error("Discord getMessageInfo failed: messageId={}", messageId, e);
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "Failed to get Discord message info: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Override
    public void deleteMessage(String messageId) {
        log.debug("Discord deleteMessage: messageId={}", messageId);
        try {
            apiClient.delete()
                    .uri("/channels/{channelId}/messages/{messageId}", channelId, messageId)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Discord deleteMessage success: messageId={}", messageId);
        } catch (RestClientException e) {
            log.error("Discord deleteMessage failed: messageId={}", messageId, e);
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "Failed to delete Discord message: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Health
    // -------------------------------------------------------------------------

    @Override
    public boolean isAvailable() {
        try {
            Map<String, Object> raw = apiClient.get()
                    .uri("/users/@me")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return raw != null && raw.containsKey("id");
        } catch (RestClientException e) {
            log.warn("Discord isAvailable check failed: {}", e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String buildPayloadJson(String caption) {
        try {
            return objectMapper.writeValueAsString(
                    Map.of("content", caption != null ? caption : ""));
        } catch (JsonProcessingException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to serialize Discord payload");
        }
    }

    private String buildPayloadJson(DiscordEmbed embed) {
        try {
            return objectMapper.writeValueAsString(
                    Map.of("content", "", "embeds", List.of(embed)));
        } catch (JsonProcessingException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to serialize Discord embed payload");
        }
    }

    @SuppressWarnings("unchecked")
    private DiscordMessageResult parseMessageResponse(Map<String, Object> raw) {
        if (raw == null) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Discord returned null response");
        }

        String messageId = String.valueOf(raw.get("id"));
        String responseChannelId = String.valueOf(raw.get("channel_id"));
        String content = (String) raw.getOrDefault("content", "");

        List<DiscordAttachment> attachments = List.of();
        if (raw.containsKey("attachments")) {
            List<Map<String, Object>> rawAttachments = (List<Map<String, Object>>) raw.get("attachments");
            attachments = rawAttachments.stream()
                    .map(a -> {
                        DiscordAttachment att = new DiscordAttachment();
                        att.setId(String.valueOf(a.get("id")));
                        att.setFilename((String) a.get("filename"));
                        att.setSize(a.containsKey("size") ? ((Number) a.get("size")).longValue() : 0L);
                        att.setUrl((String) a.get("url"));
                        att.setProxyUrl((String) a.get("proxy_url"));
                        att.setContentType((String) a.get("content_type"));
                        return att;
                    })
                    .toList();
        }

        log.debug("Discord message response: messageId={} attachments={}", messageId, attachments.size());
        return DiscordMessageResult.builder()
                .messageId(messageId)
                .channelId(responseChannelId)
                .content(content)
                .attachments(attachments)
                .build();
    }
}
