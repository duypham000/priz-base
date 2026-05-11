package com.priz.base.application.features.apikey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateApiKeyRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 500)
    private String description;

    /** Danh sách IP/CIDR cho phép, phân cách bởi dấu phẩy. Null = không giới hạn IP. */
    @Size(max = 1000)
    private String allowIps;

    /** Thời gian hết hạn (unix epoch ms). Null = không hết hạn. */
    private Long expiresAtMs;

    /** Nếu true, tự động generate HMAC secret key cho key này. */
    private boolean generateSecret;
}
