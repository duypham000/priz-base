package com.priz.base.interfaces.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end JSON-RPC 2.0 protocol tests for the MCP endpoint.
 * Runs with GitHub disabled (default in test profile).
 * Verifies protocol correctness without real API calls.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class McpProtocolIT {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("initialize")
    class InitializeTests {

        @Test
        void initialize_returnsServerInfoAndProtocolVersion() throws Exception {
            String body = """
                    {"jsonrpc":"2.0","method":"initialize","id":1,"params":{"protocolVersion":"2024-11-05","clientInfo":{"name":"test","version":"1"}}}
                    """;

            mockMvc.perform(post("/mcp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.result.serverInfo.name").value("priz-base"))
                    .andExpect(jsonPath("$.result.protocolVersion").value("2024-11-05"))
                    .andExpect(jsonPath("$.error").doesNotExist());
        }
    }

    @Nested
    @DisplayName("tools/list")
    class ToolsListTests {

        @Test
        void toolsList_returnsToolsArray() throws Exception {
            String body = """
                    {"jsonrpc":"2.0","method":"tools/list","id":2,"params":{}}
                    """;

            mockMvc.perform(post("/mcp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                    .andExpect(jsonPath("$.result.tools").isArray())
                    .andExpect(jsonPath("$.error").doesNotExist());
        }
    }

    @Nested
    @DisplayName("tools/call — error paths")
    class ToolsCallErrorTests {

        @Test
        void callUnknownTool_returnsErrorInBody_notHttpError() throws Exception {
            // JSON-RPC 2.0: errors must come back as HTTP 200 with error field — never 4xx/5xx
            String body = """
                    {"jsonrpc":"2.0","method":"tools/call","id":3,"params":{"name":"nonexistent_tool","arguments":{}}}
                    """;

            mockMvc.perform(post("/mcp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.error.code").value(-32603))
                    .andExpect(jsonPath("$.result").doesNotExist());
        }

        @Test
        void callWithoutName_returnsInvalidParamsError() throws Exception {
            String body = """
                    {"jsonrpc":"2.0","method":"tools/call","id":4,"params":{"arguments":{}}}
                    """;

            mockMvc.perform(post("/mcp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error.code").value(-32602));
        }
    }

    @Nested
    @DisplayName("unknown method")
    class UnknownMethodTests {

        @Test
        void unknownMethod_returnsMethodNotFound_asHttp200() throws Exception {
            String body = """
                    {"jsonrpc":"2.0","method":"bad/method","id":5}
                    """;

            mockMvc.perform(post("/mcp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())   // Must be 200 per JSON-RPC 2.0 spec
                    .andExpect(jsonPath("$.error.code").value(-32601))
                    .andExpect(jsonPath("$.error.message").value("Method not found: bad/method"));
        }
    }

    @Nested
    @DisplayName("response shape")
    class ResponseShapeTests {

        @Test
        void response_neverWrappedInApiResponse() throws Exception {
            // Confirm no ApiResponse<T> wrapper (no .status, .data, .message fields)
            String body = """
                    {"jsonrpc":"2.0","method":"initialize","id":6,"params":{"protocolVersion":"2024-11-05","clientInfo":{"name":"test","version":"1"}}}
                    """;

            mockMvc.perform(post("/mcp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").doesNotExist())
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(jsonPath("$.message").doesNotExist())
                    .andExpect(jsonPath("$.jsonrpc").value("2.0"));
        }
    }
}
