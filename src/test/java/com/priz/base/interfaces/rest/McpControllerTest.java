package com.priz.base.interfaces.rest;

import com.priz.base.application.mcp.McpDispatcher;
import com.priz.common.security.jwt.JwtService;
import com.priz.base.application.mcp.dto.McpError;
import com.priz.base.application.mcp.dto.McpRequest;
import com.priz.base.application.mcp.dto.McpResponse;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(McpController.class)
class McpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private McpDispatcher mcpDispatcher;

    @Nested
    @DisplayName("POST /mcp")
    class PostMcpTests {

        @Test
        void validRequest_returns200WithJsonRpc() throws Exception {
            // Arrange
            McpResponse mockResponse = McpResponse.builder()
                    .id(1L)
                    .result(Map.of("serverInfo", "priz-base"))
                    .build();
            when(mcpDispatcher.dispatch(any(McpRequest.class))).thenReturn(mockResponse);

            String body = """
                    {"jsonrpc":"2.0","method":"initialize","id":1,"params":{"protocolVersion":"2024-11-05","clientInfo":{"name":"test","version":"1"}}}
                    """;

            // Act & Assert
            mockMvc.perform(post("/mcp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.result").exists())
                    .andExpect(jsonPath("$.error").doesNotExist());
        }

        @Test
        void errorResponse_returns200WithErrorField_notHttpError() throws Exception {
            // Arrange — JSON-RPC errors must be HTTP 200, not 4xx
            McpResponse mockResponse = McpResponse.builder()
                    .id(2L)
                    .error(new McpError(-32601, "Method not found: bad/method"))
                    .build();
            when(mcpDispatcher.dispatch(any(McpRequest.class))).thenReturn(mockResponse);

            String body = """
                    {"jsonrpc":"2.0","method":"bad/method","id":2}
                    """;

            // Act & Assert
            mockMvc.perform(post("/mcp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())   // Must be 200, not 404/400
                    .andExpect(jsonPath("$.error.code").value(-32601))
                    .andExpect(jsonPath("$.error.message").value("Method not found: bad/method"))
                    .andExpect(jsonPath("$.result").doesNotExist());
        }

        @Test
        void response_doesNotHaveApiResponseWrapper() throws Exception {
            // Arrange — /mcp must NOT wrap in ApiResponse<T>
            McpResponse mockResponse = McpResponse.builder().id(3L).result(Map.of("tools", "[]")).build();
            when(mcpDispatcher.dispatch(any())).thenReturn(mockResponse);

            String body = """
                    {"jsonrpc":"2.0","method":"tools/list","id":3,"params":{}}
                    """;

            // Act & Assert
            mockMvc.perform(post("/mcp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").doesNotExist())   // No ApiResponse.status
                    .andExpect(jsonPath("$.data").doesNotExist())     // No ApiResponse.data
                    .andExpect(jsonPath("$.jsonrpc").value("2.0"));
        }
    }
}
