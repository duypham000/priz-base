package com.priz.base.application.features.files.dto;

import com.priz.interfaces.admin.dto.PageRequestDto;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileSearchRequest {

    @NotBlank(message = "Search query is required")
    private String query;

    @Builder.Default
    private PageRequestDto pagination = new PageRequestDto();
}
