package com.priz.base.application.mcp.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.priz.base.application.mcp.McpDispatcher;
import com.priz.base.application.mcp.dto.McpError;
import com.priz.base.application.mcp.dto.McpRequest;
import com.priz.base.application.mcp.dto.McpResponse;
import com.priz.base.application.mcp.provider.McpToolDefinition;
import com.priz.base.application.mcp.provider.McpToolResult;
import com.priz.base.application.mcp.registry.McpToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpDispatcherImpl implements McpDispatcher {

    private static final String SERVER_NAME = "priz-base";
    private static final String SERVER_VERSION = "1.0.0";
    private static final String PROTOCOL_VERSION = "2024-11-05";

    // Spring Boot 4.x uses Jackson 3.x (tools.jackson) — no com.fasterxml.jackson.databind.ObjectMapper bean.
    // We instantiate directly; ObjectMapper is thread-safe after construction.
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final McpToolRegistry toolRegistry;

    @Override
    public McpResponse dispatch(McpRequest request) {
        if (request == null || request.getMethod() == null) {
            return errorResponse(null, -32600, "Invalid Request");
        }
        log.debug("MCP dispatch: method={}", request.getMethod());
        return switch (request.getMethod()) {
            case "initialize"  -> handleInitialize(request);
            case "tools/list"  -> handleToolsList(request);
            case "tools/call"  -> handleToolsCall(request);
            default            -> errorResponse(request.getId(), -32601, "Method not found: " + request.getMethod());
        };
    }

    private McpResponse handleInitialize(McpRequest request) {
        Map<String, Object> serverInfo = Map.of("name", SERVER_NAME, "version", SERVER_VERSION);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serverInfo", serverInfo);
        result.put("protocolVersion", PROTOCOL_VERSION);
        result.put("capabilities", Map.of("tools", Map.of()));
        return successResponse(request.getId(), result);
    }

    private McpResponse handleToolsList(McpRequest request) {
        List<McpToolDefinition> defs = toolRegistry.listAllTools();
        List<Map<String, Object>> tools = defs.stream()
                .map(def -> Map.<String, Object>of(
                        "name", def.name(),
                        "description", def.description(),
                        "inputSchema", def.inputSchema()))
                .toList();
        log.debug("MCP tools/list: returning {} tools", defs.size());
        return successResponse(request.getId(), Map.of("tools", tools));
    }

    private McpResponse handleToolsCall(McpRequest request) {
        if (request.getParams() == null) {
            return errorResponse(request.getId(), -32602, "Invalid params: missing params");
        }
        JsonNode params = toJsonNode(request.getParams());
        String toolName = params.path("name").asText(null);
        if (toolName == null || toolName.isBlank()) {
            return errorResponse(request.getId(), -32602, "Invalid params: 'name' is required");
        }
        JsonNode arguments = params.has("arguments") ? params.path("arguments") : MAPPER.createObjectNode();

        log.debug("MCP tools/call: tool={}", toolName);
        McpToolResult toolResult = toolRegistry.callTool(toolName, arguments);
        if (toolResult.isError()) {
            return errorResponse(request.getId(), -32603, toolResult.errorMessage());
        }
        return successResponse(request.getId(), toolResult.content());
    }

    private McpResponse successResponse(Object id, Object result) {
        return McpResponse.builder().id(id).result(result).build();
    }

    private McpResponse errorResponse(Object id, int code, String message) {
        return McpResponse.builder().id(id).error(new McpError(code, message)).build();
    }

    // Jackson 3.x (tools.jackson) deserializes HTTP request bodies; params arrives as Map<String,Object>.
    // Convert back to Jackson 2.x JsonNode for use within the MCP tool layer.
    private static JsonNode toJsonNode(Object value) {
        if (value == null) return MAPPER.createObjectNode();
        return MAPPER.valueToTree(value);
    }
}
