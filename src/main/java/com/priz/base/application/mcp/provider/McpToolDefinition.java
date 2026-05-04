package com.priz.base.application.mcp.provider;

public record McpToolDefinition(
        String name,
        String description,
        Object inputSchema   // Map<String,Object> — serialized by Jackson 3.x without issues
) {}
