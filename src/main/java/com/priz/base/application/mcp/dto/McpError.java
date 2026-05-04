package com.priz.base.application.mcp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class McpError {
    private int code;
    private String message;
}
