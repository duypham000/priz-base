package com.priz.base.application.mcp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpResponse {
    @Builder.Default
    private String jsonrpc = "2.0";
    private Object id;
    private Object result;   // Map/List/primitive — serialized by Jackson 3.x as-is
    private McpError error;
}
