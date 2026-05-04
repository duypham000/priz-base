package com.priz.base.application.integration.discord;

import com.priz.base.application.integration.discord.dto.DiscordEmbed;
import com.priz.base.application.integration.discord.dto.DiscordMessageResult;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface DiscordService {

    // -------------------------------------------------------------------------
    // Upload
    // -------------------------------------------------------------------------

    /** Upload bất kỳ file với optional plain-text caption. */
    DiscordMessageResult upload(MultipartFile file, String caption);

    /** Upload từ raw bytes. */
    DiscordMessageResult upload(String fileName, byte[] content, String contentType, String caption);

    /**
     * Upload từ InputStream — streaming, không buffer toàn bộ file vào RAM.
     * Caller chịu trách nhiệm đóng stream sau khi gọi xong.
     */
    DiscordMessageResult upload(String fileName, InputStream content, long size, String contentType, String caption);

    /** Upload file kèm rich embed thay cho plain caption. */
    DiscordMessageResult uploadWithEmbed(MultipartFile file, DiscordEmbed embed);

    // -------------------------------------------------------------------------
    // Messaging
    // -------------------------------------------------------------------------

    /** Gửi tin nhắn văn bản thuần đến channel đã cấu hình. */
    DiscordMessageResult sendMessage(String content);

    /** Gửi rich embed (không kèm file). */
    DiscordMessageResult sendEmbed(DiscordEmbed embed);

    /**
     * Chỉnh sửa nội dung text của một message.
     * Attachments và embeds hiện có không bị ảnh hưởng.
     */
    DiscordMessageResult editMessage(String messageId, String newContent);

    // -------------------------------------------------------------------------
    // Download
    // -------------------------------------------------------------------------

    /**
     * Tải file từ Discord CDN về dạng {@link Resource}.
     * Dùng trực tiếp {@code url} từ {@link com.priz.base.application.integration.discord.dto.DiscordAttachment}.
     * CDN URL của Discord là permanent — không cần gọi thêm bước lấy URL như Telegram.
     */
    Resource downloadAsResource(String cdnUrl);

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    /** Lấy thông tin message bao gồm danh sách attachments. */
    DiscordMessageResult getMessageInfo(String messageId);

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    /** Xóa message khỏi channel. Ném BusinessException nếu message không tồn tại. */
    void deleteMessage(String messageId);

    // -------------------------------------------------------------------------
    // Health
    // -------------------------------------------------------------------------

    /** Trả về true khi bot token hợp lệ (GET /users/@me thành công). */
    boolean isAvailable();
}
