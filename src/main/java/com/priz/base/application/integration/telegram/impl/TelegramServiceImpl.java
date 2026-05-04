package com.priz.base.application.integration.telegram.impl;

import com.priz.base.application.integration.telegram.TelegramService;
import com.priz.base.application.integration.telegram.dto.TelegramFileInfo;
import com.priz.base.application.integration.telegram.dto.TelegramUploadResult;
import com.priz.base.application.integration.telegram.util.TelegramFileTypeResolver;
import com.priz.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Slf4j
public class TelegramServiceImpl implements TelegramService {

    private final RestClient restClient;
    private final String channelId;
    private final String fileDownloadBaseUrl;
    public TelegramServiceImpl(RestClient restClient, String channelId, String fileDownloadBaseUrl) {
        this.restClient = restClient;
        this.channelId = channelId;
        this.fileDownloadBaseUrl = fileDownloadBaseUrl;
    }

    // -------------------------------------------------------------------------
    // Upload
    // -------------------------------------------------------------------------

    @Override
    public TelegramUploadResult upload(MultipartFile file, String caption) {
        log.debug("Telegram upload: file={} size={}", file.getOriginalFilename(), file.getSize());
        String contentType = file.getContentType();
        String method = TelegramFileTypeResolver.resolveMethod(contentType);
        String fieldName = TelegramFileTypeResolver.resolveFieldName(method);

        try {
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            return doUpload(method, fieldName, fileResource, contentType, caption);
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to read file for upload: " + file.getOriginalFilename());
        }
    }

    @Override
    public TelegramUploadResult upload(String fileName, byte[] content, String contentType, String caption) {
        log.debug("Telegram upload bytes: file={} size={}", fileName, content.length);
        String method = TelegramFileTypeResolver.resolveMethod(contentType);
        String fieldName = TelegramFileTypeResolver.resolveFieldName(method);

        ByteArrayResource fileResource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
        return doUpload(method, fieldName, fileResource, contentType, caption);
    }

    @Override
    public TelegramUploadResult upload(String fileName, InputStream content, long size, String contentType, String caption) {
        log.debug("Telegram upload stream: file={} size={}", fileName, size);
        String method = TelegramFileTypeResolver.resolveMethod(contentType);
        String fieldName = TelegramFileTypeResolver.resolveFieldName(method);

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
        return doUpload(method, fieldName, streamResource, contentType, caption);
    }

    private TelegramUploadResult doUpload(String method, String fieldName, Resource fileResource,
                                          String contentType, String caption) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("chat_id", channelId);
        body.add(fieldName, fileResource);
        if (caption != null && !caption.isBlank()) {
            body.add("caption", caption);
        }

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/" + method)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<>() {});

            return parseUploadResponse(response, contentType);
        } catch (RestClientException e) {
            log.error("Telegram upload failed via {}: {}", method, e.getMessage());
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "Telegram upload failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private TelegramUploadResult parseUploadResponse(Map<String, Object> raw, String contentType) {
        if (raw == null || !Boolean.TRUE.equals(raw.get("ok"))) {
            String description = raw != null ? String.valueOf(raw.get("description")) : "unknown error";
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Telegram API error: " + description);
        }

        Map<String, Object> message = (Map<String, Object>) raw.get("result");
        long messageId = ((Number) message.get("message_id")).longValue();

        // Telegram trả về field khác nhau tuỳ loại: document, photo (array), video, audio
        Map<String, Object> fileObj = null;
        for (String key : new String[]{"document", "video", "audio"}) {
            if (message.containsKey(key)) {
                fileObj = (Map<String, Object>) message.get(key);
                break;
            }
        }
        // photo là array — lấy phần tử cuối (độ phân giải cao nhất)
        if (fileObj == null && message.containsKey("photo")) {
            var photos = (java.util.List<Map<String, Object>>) message.get("photo");
            fileObj = photos.get(photos.size() - 1);
        }

        if (fileObj == null) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Telegram response missing file object");
        }

        String fileId = (String) fileObj.get("file_id");
        String fileUniqueId = (String) fileObj.get("file_unique_id");
        long fileSize = fileObj.containsKey("file_size")
                ? ((Number) fileObj.get("file_size")).longValue() : 0L;
        String mimeType = (String) fileObj.getOrDefault("mime_type", contentType);

        log.debug("Telegram upload success: messageId={} fileId={}", messageId, fileId);
        return TelegramUploadResult.builder()
                .messageId(messageId)
                .fileId(fileId)
                .fileUniqueId(fileUniqueId)
                .fileSize(fileSize)
                .mimeType(mimeType)
                .build();
    }

    // -------------------------------------------------------------------------
    // Download
    // -------------------------------------------------------------------------

    @Override
    public Resource downloadAsResource(String telegramFileId) {
        log.debug("Telegram download: fileId={}", telegramFileId);
        TelegramFileInfo info = getFileInfo(telegramFileId);
        try {
            byte[] bytes = restClient.get()
                    .uri(fileDownloadBaseUrl + "/" + info.getFilePath())
                    .retrieve()
                    .body(byte[].class);
            return new ByteArrayResource(bytes != null ? bytes : new byte[0]);
        } catch (RestClientException e) {
            log.error("Telegram download failed: fileId={}", telegramFileId, e);
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "Failed to download file from Telegram: " + e.getMessage());
        }
    }

    @Override
    public String getDownloadUrl(String telegramFileId) {
        TelegramFileInfo info = getFileInfo(telegramFileId);
        return info.getDownloadUrl();
    }

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public TelegramFileInfo getFileInfo(String telegramFileId) {
        log.debug("Telegram getFile: fileId={}", telegramFileId);
        try {
            Map<String, Object> raw = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/getFile")
                            .queryParam("file_id", telegramFileId)
                            .build())
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<>() {});

            if (raw == null || !Boolean.TRUE.equals(raw.get("ok"))) {
                String description = raw != null ? String.valueOf(raw.get("description")) : "unknown error";
                throw new BusinessException(HttpStatus.NOT_FOUND, "Telegram getFile error: " + description);
            }

            Map<String, Object> result = (Map<String, Object>) raw.get("result");
            String filePath = (String) result.get("file_path");
            Long fileSize = result.containsKey("file_size")
                    ? ((Number) result.get("file_size")).longValue() : null;

            return TelegramFileInfo.builder()
                    .fileId((String) result.get("file_id"))
                    .fileUniqueId((String) result.get("file_unique_id"))
                    .fileSize(fileSize)
                    .filePath(filePath)
                    .downloadUrl(fileDownloadBaseUrl + "/" + filePath)
                    .build();
        } catch (RestClientException e) {
            log.error("Telegram getFile failed: fileId={}", telegramFileId, e);
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "Failed to get file info from Telegram: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Override
    public void deleteMessage(long messageId) {
        log.debug("Telegram deleteMessage: messageId={}", messageId);
        try {
            Map<String, Object> body = Map.of(
                    "chat_id", channelId,
                    "message_id", messageId
            );
            Map<String, Object> raw = restClient.post()
                    .uri("/deleteMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<>() {});

            if (raw == null || !Boolean.TRUE.equals(raw.get("ok"))) {
                String description = raw != null ? String.valueOf(raw.get("description")) : "unknown error";
                log.warn("Telegram deleteMessage returned error: {}", description);
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "Telegram deleteMessage error: " + description);
            }
            log.debug("Telegram deleteMessage success: messageId={}", messageId);
        } catch (RestClientException e) {
            log.error("Telegram deleteMessage failed: messageId={}", messageId, e);
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "Failed to delete Telegram message: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Health
    // -------------------------------------------------------------------------

    @Override
    public boolean isAvailable() {
        try {
            Map<String, Object> raw = restClient.get()
                    .uri("/getMe")
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<>() {});
            return raw != null && Boolean.TRUE.equals(raw.get("ok"));
        } catch (RestClientException e) {
            log.warn("Telegram isAvailable check failed: {}", e.getMessage());
            return false;
        }
    }
}
