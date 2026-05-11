package com.priz.base.application.integration.telegram.impl;

import com.priz.base.application.integration.telegram.TelegramService;
import com.priz.base.application.integration.telegram.dto.TelegramFileInfo;
import com.priz.base.application.integration.telegram.dto.TelegramUploadResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "telegram", name = "enabled", havingValue = "false", matchIfMissing = true)
public class TelegramServiceNoOpImpl implements TelegramService {

    private static final TelegramUploadResult NOOP_RESULT = TelegramUploadResult.builder()
            .messageId(0).fileId("").fileUniqueId("").fileSize(0).downloadUrl("").build();

    @Override
    public TelegramUploadResult upload(MultipartFile file, String caption) {
        log.warn("Telegram is disabled — upload skipped for {}", file.getOriginalFilename());
        return NOOP_RESULT;
    }

    @Override
    public TelegramUploadResult upload(String fileName, byte[] content, String contentType, String caption) {
        log.warn("Telegram is disabled — upload skipped for {}", fileName);
        return NOOP_RESULT;
    }

    @Override
    public TelegramUploadResult upload(String fileName, InputStream content, long size, String contentType, String caption) {
        log.warn("Telegram is disabled — upload skipped for {}", fileName);
        return NOOP_RESULT;
    }

    @Override
    public Resource downloadAsResource(String telegramFileId) {
        throw new UnsupportedOperationException("Telegram is disabled");
    }

    @Override
    public String getDownloadUrl(String telegramFileId) {
        throw new UnsupportedOperationException("Telegram is disabled");
    }

    @Override
    public TelegramFileInfo getFileInfo(String telegramFileId) {
        throw new UnsupportedOperationException("Telegram is disabled");
    }

    @Override
    public void deleteMessage(long messageId) {
        log.warn("Telegram is disabled — deleteMessage skipped for messageId={}", messageId);
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
