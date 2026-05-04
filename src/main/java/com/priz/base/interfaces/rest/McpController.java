package com.priz.base.interfaces.rest;

import com.priz.base.application.mcp.McpDispatcher;
import com.priz.base.application.mcp.dto.McpRequest;
import com.priz.base.application.mcp.dto.McpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MCP (Model Context Protocol) inbound adapter.
 *
 * Returns raw JSON-RPC 2.0 — intentionally does NOT use ApiResponse<T> wrapper.
 * No @Secured — agent connects without JWT auth headers.
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpController {

    private final McpDispatcher mcpDispatcher;

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public McpResponse handle(@RequestBody McpRequest request) {
        return mcpDispatcher.dispatch(request);
    }
}
