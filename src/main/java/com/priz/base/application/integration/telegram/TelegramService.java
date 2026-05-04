package com.priz.base.application.integration.telegram;

import com.priz.base.application.integration.telegram.dto.TelegramFileInfo;
import com.priz.base.application.integration.telegram.dto.TelegramUploadResult;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface TelegramService {

    // -------------------------------------------------------------------------
    // Upload
    // -------------------------------------------------------------------------

    /** Upload bất kỳ file — auto-detect send method từ content type. */
    TelegramUploadResult upload(MultipartFile file, String caption);

    /** Upload từ raw bytes — dùng cho sync từ nguồn ngoài hoặc generated content. */
    TelegramUploadResult upload(String fileName, byte[] content, String contentType, String caption);

    /**
     * Upload từ InputStream — streaming, không buffer toàn bộ file vào RAM.
     * Caller chịu trách nhiệm đóng stream sau khi gọi xong.
     */
    TelegramUploadResult upload(String fileName, InputStream content, long size, String contentType, String caption);

    // -------------------------------------------------------------------------
    // Download
    // -------------------------------------------------------------------------

    /**
     * Tải file về dưới dạng {@link Resource} để stream về client.
     * Luôn gọi /getFile trước để lấy filePath mới nhất (URL expire sau ~1 giờ).
     */
    Resource downloadAsResource(String telegramFileId);

    /**
     * Trả về temporary download URL.
     * Dùng khi muốn redirect client thay vì proxy qua server.
     */
    String getDownloadUrl(String telegramFileId);

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    /** Lấy thông tin file từ Telegram (size, filePath, v.v.). */
    TelegramFileInfo getFileInfo(String telegramFileId);

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    /** Xóa message khỏi channel — đây là cách duy nhất "xóa file" trên Telegram. */
    void deleteMessage(long messageId);

    // -------------------------------------------------------------------------
    // Health
    // -------------------------------------------------------------------------

    /** Kiểm tra bot có hoạt động và có quyền post vào channel không. */
    boolean isAvailable();
}
