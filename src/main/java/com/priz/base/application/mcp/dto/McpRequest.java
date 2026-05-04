package com.priz.base.application.mcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpRequest {
    private String jsonrpc;
    private String method;
    private Object id;
    // Object instead of JsonNode: Spring Boot 4.x uses Jackson 3.x (tools.jackson) for HTTP deserialization
    // which cannot deserialize into Jackson 2.x JsonNode. McpDispatcherImpl converts to JsonNode internally.
    private Object params;
}
