package com.priz.base.application.integration.telegram.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TelegramUploadResult {

    /** Telegram message ID — cần để deleteMessage */
    private long messageId;

    /** Telegram file_id — dùng để download/getFileInfo */
    private String fileId;

    /** Stable unique ID — không thay đổi dù file_id rotate */
    private String fileUniqueId;

    private long fileSize;
    private String mimeType;

    /** Temporary download URL (expires ~1 giờ) */
    private String downloadUrl;
}
