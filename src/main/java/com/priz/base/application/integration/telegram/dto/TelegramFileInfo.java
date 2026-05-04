package com.priz.base.application.integration.telegram.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TelegramFileInfo {

    private String fileId;
    private String fileUniqueId;
    private Long fileSize;

    /** Relative path dùng để build download URL (expires) */
    private String filePath;

    /** Absolute download URL = baseUrl/file/bot{token}/{filePath} */
    private String downloadUrl;
}
