package com.priz.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.priz.base.application.integration.github.GithubService;
import com.priz.base.application.mcp.McpDispatcher;
import com.priz.base.application.mcp.dto.McpRequest;
import com.priz.base.application.mcp.dto.McpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify GitHub integration after refactoring.
 * 
 * To run with your real token:
 * 1. Set GITHUB_TOKEN environment variable
 * 2. Run: .\mvnw test -Dtest=GithubRefactorTest -DMCP_GITHUB_ENABLED=true
 */
@SpringBootTest(properties = {
    "mcp.github.enabled=true"
})
public class GithubRefactorTest {

    @Autowired
    private GithubService githubService;

    @Autowired
    private McpDispatcher mcpDispatcher;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testDirectServiceCall() {
        assertNotNull(githubService, "GithubService should be wired");
        
        try {
            // Using a public repo for testing
            Object result = githubService.getRepository("google", "gson");
            assertNotNull(result);
            System.out.println("Direct Service Call Success: " + result);
        } catch (Exception e) {
            System.err.println("Direct Service Call failed: " + e.getMessage());
            // Fail the test if it's not a 401/403 (which might happen if no token is provided)
            if (!e.getMessage().contains("401") && !e.getMessage().contains("403")) {
                fail("Expected success or 401/403, but got: " + e.getMessage());
            }
        }
    }

    @Test
    public void testMcpDispatcherCall() {
        assertNotNull(mcpDispatcher, "McpDispatcher should be wired");

        McpRequest request = new McpRequest();
        request.setJsonrpc("2.0");
        request.setMethod("tools/call");
        request.setId(1);

        ObjectNode params = mapper.createObjectNode();
        params.put("name", "github_get_repository");
        ObjectNode arguments = mapper.createObjectNode();
        arguments.put("owner", "google");
        arguments.put("repo", "gson");
        params.set("arguments", arguments);
        request.setParams(params);

        McpResponse response = mcpDispatcher.dispatch(request);
        
        assertNotNull(response);
        if (response.getError() != null) {
            System.err.println("MCP Dispatcher error: " + response.getError().getMessage());
            String msg = response.getError().getMessage();
            if (!msg.contains("401") && !msg.contains("403")) {
                fail("Expected MCP success or 401/403, but got: " + msg);
            }
        } else {
            assertNotNull(response.getResult());
            System.out.println("MCP Dispatcher Success: " + response.getResult());
        }
    }
}
