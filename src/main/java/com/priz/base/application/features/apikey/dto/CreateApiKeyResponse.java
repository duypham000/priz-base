package com.priz.base.application.features.apikey.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateApiKeyResponse {

    private String id;
    private String name;

    /**
     * Full API key để client lưu trữ (format: {code}.{rawKey}).
     * CHỈ trả về 1 lần khi tạo/regenerate. Sau đó không thể lấy lại.
     */
    private String apiKey;

    /** Secret key cho HMAC signature (nếu generateSecret = true). Chỉ trả về 1 lần. */
    private String secretKey;
}
