package com.priz.base.application.mcp.registry;

import com.fasterxml.jackson.databind.JsonNode;
import com.priz.base.application.mcp.provider.McpToolDefinition;
import com.priz.base.application.mcp.provider.McpToolResult;

import java.util.List;

public interface McpToolRegistry {

    List<McpToolDefinition> listAllTools();

    McpToolResult callTool(String toolName, JsonNode arguments);
}
