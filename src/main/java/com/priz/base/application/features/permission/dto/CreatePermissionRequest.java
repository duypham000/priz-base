package com.priz.base.application.features.permission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePermissionRequest {

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[a-z0-9:_-]+$", message = "Code chỉ chứa chữ thường, số, dấu ':', '_', '-'")
    private String code;

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 500)
    private String description;

    @Size(max = 100)
    private String groupCode;
}
