package com.priz.base.application.mcp.provider;

public record McpToolResult(
        Object content,          // Map/List from API — serialized by Jackson 3.x without issues
        boolean isError,
        String errorMessage
) {
    public static McpToolResult success(Object content) {
        return new McpToolResult(content, false, null);
    }

    public static McpToolResult error(String message) {
        return new McpToolResult(null, true, message);
    }
}
