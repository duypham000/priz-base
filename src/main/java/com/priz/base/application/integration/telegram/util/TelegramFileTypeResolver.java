package com.priz.base.application.integration.telegram.util;

public class TelegramFileTypeResolver {

    private TelegramFileTypeResolver() {}

    /**
     * Resolve MIME type → Telegram Bot API send method name.
     *
     * Telegram compresses photos/videos/audio natively.
     * sendDocument là fallback an toàn — không mất chất lượng.
     */
    public static String resolveMethod(String contentType) {
        if (contentType == null) {
            return "sendDocument";
        }
        if (contentType.startsWith("image/")) {
            return "sendPhoto";
        }
        if (contentType.startsWith("video/")) {
            return "sendVideo";
        }
        if (contentType.startsWith("audio/")) {
            return "sendAudio";
        }
        return "sendDocument";
    }

    /**
     * Trả về tên field trong multipart body tương ứng với method.
     * sendDocument → "document", sendPhoto → "photo", v.v.
     */
    public static String resolveFieldName(String method) {
        return switch (method) {
            case "sendPhoto" -> "photo";
            case "sendVideo" -> "video";
            case "sendAudio" -> "audio";
            default -> "document";
        };
    }
}
