package com.priz.base.application.mcp.registry.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.priz.base.application.mcp.provider.McpToolDefinition;
import com.priz.base.application.mcp.provider.McpToolProvider;
import com.priz.base.application.mcp.provider.McpToolResult;
import com.priz.base.application.mcp.registry.McpToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class McpToolRegistryImpl implements McpToolRegistry {

    private final List<McpToolProvider> providers;

    @Override
    public List<McpToolDefinition> listAllTools() {
        if (providers == null || providers.isEmpty()) {
            return Collections.emptyList();
        }
        return providers.stream()
                .flatMap(p -> p.getToolDefinitions().stream())
                .toList();
    }

    @Override
    public McpToolResult callTool(String toolName, JsonNode arguments) {
        if (providers == null) {
            return McpToolResult.error("No tool providers registered");
        }
        return providers.stream()
                .filter(p -> p.supports(toolName))
                .findFirst()
                .map(p -> {
                    log.debug("Dispatching tool '{}' to provider '{}'", toolName, p.providerName());
                    return p.callTool(toolName, arguments);
                })
                .orElse(McpToolResult.error("Unknown tool: " + toolName));
    }
}
