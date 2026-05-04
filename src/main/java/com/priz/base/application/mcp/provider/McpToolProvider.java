package com.priz.base.application.mcp.provider;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Contract for every MCP tool provider.
 *
 * To add a new provider (e.g. Slack):
 *   1. Implement this interface
 *   2. Annotate with @Component + @ConditionalOnProperty("mcp.{name}.enabled")
 *   3. Add an @Bean for its API client in McpConfig
 *   4. Add config under mcp.{name} in application.yaml
 *
 * McpToolRegistryImpl discovers all beans implementing this interface automatically.
 */
public interface McpToolProvider {

    String providerName();

    List<McpToolDefinition> getToolDefinitions();

    McpToolResult callTool(String toolName, JsonNode arguments);

    default boolean supports(String toolName) {
        return toolName != null && toolName.startsWith(providerName() + "_");
    }
}
