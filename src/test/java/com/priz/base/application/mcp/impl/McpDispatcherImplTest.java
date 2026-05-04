package com.priz.base.application.mcp.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.priz.base.application.mcp.dto.McpRequest;
import com.priz.base.application.mcp.dto.McpResponse;
import com.priz.base.application.mcp.provider.McpToolDefinition;
import com.priz.base.application.mcp.provider.McpToolResult;
import com.priz.base.application.mcp.registry.McpToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpDispatcherImplTest {

    @Mock
    private McpToolRegistry toolRegistry;

    @InjectMocks
    private McpDispatcherImpl dispatcher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        dispatcher = new McpDispatcherImpl(toolRegistry);
    }

    @Nested
    @DisplayName("initialize")
    class InitializeTests {

        @Test
        void validRequest_returnsServerInfoAndProtocolVersion() {
            // Arrange
            McpRequest request = buildRequest("initialize", 1L, null);

            // Act
            McpResponse response = dispatcher.dispatch(request);

            // Assert
            assertThat(response.getJsonrpc()).isEqualTo("2.0");
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getError()).isNull();
            JsonNode result = toJsonNode(response.getResult());
            assertThat(result.path("serverInfo").path("name").asText()).isEqualTo("priz-base");
            assertThat(result.path("serverInfo").path("version").asText()).isNotBlank();
            assertThat(result.path("protocolVersion").asText()).isEqualTo("2024-11-05");
        }
    }

    @Nested
    @DisplayName("tools/list")
    class ToolsListTests {

        @Test
        void withRegisteredTools_returnsToolArray() {
            // Arrange
            McpToolDefinition def = new McpToolDefinition("test_tool", "A test tool",
                    Map.of("type", "object"));
            when(toolRegistry.listAllTools()).thenReturn(List.of(def));
            McpRequest request = buildRequest("tools/list", 2L, null);

            // Act
            McpResponse response = dispatcher.dispatch(request);

            // Assert
            assertThat(response.getError()).isNull();
            JsonNode result = toJsonNode(response.getResult());
            JsonNode tools = result.path("tools");
            assertThat(tools.isArray()).isTrue();
            assertThat(tools.size()).isEqualTo(1);
            assertThat(tools.get(0).path("name").asText()).isEqualTo("test_tool");
            assertThat(tools.get(0).path("description").asText()).isEqualTo("A test tool");
        }

        @Test
        void withNoTools_returnsEmptyArray() {
            // Arrange
            when(toolRegistry.listAllTools()).thenReturn(List.of());
            McpRequest request = buildRequest("tools/list", 3L, null);

            // Act
            McpResponse response = dispatcher.dispatch(request);

            // Assert
            assertThat(response.getError()).isNull();
            JsonNode result = toJsonNode(response.getResult());
            assertThat(result.path("tools").isArray()).isTrue();
            assertThat(result.path("tools").size()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("tools/call")
    class ToolsCallTests {

        @Test
        void knownTool_success_returnsResult() throws Exception {
            // Arrange
            ObjectNode content = objectMapper.createObjectNode().put("data", "value");
            when(toolRegistry.callTool(eq("github_get_repository"), any()))
                    .thenReturn(McpToolResult.success(content));

            ObjectNode params = objectMapper.createObjectNode();
            params.put("name", "github_get_repository");
            params.set("arguments", objectMapper.createObjectNode().put("owner", "octocat").put("repo", "Hello-World"));
            McpRequest request = buildRequest("tools/call", 4L, params);

            // Act
            McpResponse response = dispatcher.dispatch(request);

            // Assert
            assertThat(response.getError()).isNull();
            assertThat(toJsonNode(response.getResult()).path("data").asText()).isEqualTo("value");
            verify(toolRegistry).callTool(eq("github_get_repository"), any());
        }

        @Test
        void knownTool_error_returnsErrorInBody() {
            // Arrange
            when(toolRegistry.callTool(eq("error_tool"), any()))
                    .thenReturn(McpToolResult.error("Generic tool error: 500"));

            ObjectNode params = objectMapper.createObjectNode();
            params.put("name", "error_tool");
            McpRequest request = buildRequest("tools/call", 5L, params);

            // Act
            McpResponse response = dispatcher.dispatch(request);

            // Assert
            assertThat(response.getResult()).isNull();
            assertThat(response.getError()).isNotNull();
            assertThat(response.getError().getCode()).isEqualTo(-32603);
            assertThat(response.getError().getMessage()).contains("Generic tool error");
        }

        @Test
        void missingName_returnsInvalidParamsError() {
            // Arrange
            ObjectNode params = objectMapper.createObjectNode();
            // no "name" field
            McpRequest request = buildRequest("tools/call", 6L, params);

            // Act
            McpResponse response = dispatcher.dispatch(request);

            // Assert
            assertThat(response.getError()).isNotNull();
            assertThat(response.getError().getCode()).isEqualTo(-32602);
        }

        @Test
        void nullParams_returnsInvalidParamsError() {
            // Arrange
            McpRequest request = buildRequest("tools/call", 7L, null);

            // Act
            McpResponse response = dispatcher.dispatch(request);

            // Assert
            assertThat(response.getError()).isNotNull();
            assertThat(response.getError().getCode()).isEqualTo(-32602);
        }
    }

    @Nested
    @DisplayName("unknown method")
    class UnknownMethodTests {

        @Test
        void unknownMethod_returnsMethodNotFoundError() {
            // Arrange
            McpRequest request = buildRequest("unknown/method", 8L, null);

            // Act
            McpResponse response = dispatcher.dispatch(request);

            // Assert
            assertThat(response.getError()).isNotNull();
            assertThat(response.getError().getCode()).isEqualTo(-32601);
            assertThat(response.getError().getMessage()).contains("Method not found");
        }
    }

    @Nested
    @DisplayName("null/invalid request")
    class InvalidRequestTests {

        @Test
        void nullRequest_returnsInvalidRequestError() {
            // Act
            McpResponse response = dispatcher.dispatch(null);

            // Assert
            assertThat(response.getError()).isNotNull();
            assertThat(response.getError().getCode()).isEqualTo(-32600);
            assertThat(response.getId()).isNull();
        }

        @Test
        void nullMethod_returnsInvalidRequestError() {
            // Arrange
            McpRequest request = new McpRequest();
            request.setId(9L);
            // method is null

            // Act
            McpResponse response = dispatcher.dispatch(request);

            // Assert
            assertThat(response.getError()).isNotNull();
            assertThat(response.getError().getCode()).isEqualTo(-32600);
        }
    }

    private McpRequest buildRequest(String method, Object id, Object params) {
        McpRequest request = new McpRequest();
        request.setJsonrpc("2.0");
        request.setMethod(method);
        request.setId(id);
        request.setParams(params);
        return request;
    }

    private JsonNode toJsonNode(Object value) {
        return objectMapper.valueToTree(value);
    }
}
