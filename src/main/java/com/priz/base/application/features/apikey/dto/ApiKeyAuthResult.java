package com.priz.base.application.features.apikey.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class ApiKeyAuthResult {

    private boolean valid;
    private String apiKeyId;
    private String code;
    private String name;
    private Set<String> permissions;
    private boolean signatureVerified;
    private String errorCode;
    private String errorMessage;

    public static ApiKeyAuthResult failure(String errorCode, String message) {
        return ApiKeyAuthResult.builder()
                .valid(false)
                .errorCode(errorCode)
                .errorMessage(message)
                .build();
    }
}
