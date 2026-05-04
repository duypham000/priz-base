package com.priz.base.application.features.files.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileSyncRequest {

    @NotEmpty(message = "File sources must not be empty")
    private List<FileSyncItem> files;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileSyncItem {

        @NotBlank(message = "Source URL is required")
        private String sourceUrl;

        @NotBlank(message = "Original name is required")
        private String originalName;

        private String fileType;
        private Long fileSize;
        private String contentType;
        private String description;
    }
}
