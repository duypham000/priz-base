package com.priz.base.application.features.files.dto;

import com.priz.interfaces.admin.dto.PageRequestDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileFilterRequest {

    private String originalName;
    private String fileType;
    private String contentType;
    private Boolean isSynced;
    private Long minSize;
    private Long maxSize;

    @Builder.Default
    private PageRequestDto pagination = new PageRequestDto();
}
