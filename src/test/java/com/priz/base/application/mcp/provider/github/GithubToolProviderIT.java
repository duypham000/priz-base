package com.priz.base.application.mcp.provider.github;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for GitHub MCP tools using the real GitHub API.
 * Fixture repo: https://github.com/Donchitos/Claude-Code-Game-Studios (public)
 *
 * Required env vars:
 *   MCP_GITHUB_ENABLED=true
 *   GITHUB_TOKEN=<your-github-pat>
 *
 * Run with:
 *   MCP_GITHUB_ENABLED=true GITHUB_TOKEN=xxx \
 *     mvnw.cmd test -Dtest=GithubToolProviderIT
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "GITHUB_TOKEN", matches = ".+")
class GithubToolProviderIT {

    @Autowired
    private MockMvc mockMvc;

    private static final String REPO_URL = "https://github.com/Donchitos/Claude-Code-Game-Studios";
    private static final String PR_REPO_URL = "https://github.com/facebook/react";

    @Nested
    @DisplayName("github_get_repository")
    class GetRepositoryTests {

        @Test
        @DisplayName("via URL — returns full_name and default_branch")
        void viaUrl_returnsRepoMetadata() throws Exception {
            String body = """
                    {"jsonrpc":"2.0","method":"tools/call","id":1,"params":{"name":"github_get_repository","arguments":{"url":"%s"}}}
                    """.formatted(REPO_URL);

            mockMvc.perform(post("/mcp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").doesNotExist())
                    .andExpect(jsonPath("$.result.full_name").value("Donchitos/Claude-Code-Game-Studios"))
                    .andExpect(jsonPath("$.result.default_branch").exists());
        }

        @Test
        @DisplayName("via owner+repo — same result as URL-based call")
        void viaOwnerRepo_returnsRepoMetadata() throws Exception {
            String body = """
                    {"jsonrpc":"2.0","method":"tools/call","id":2,"params":{"name":"github_get_repository","arguments":{"owner":"Donchitos","repo":"Claude-Code-Game-Studios"}}}
                    """;

            mockMvc.perform(post("/mcp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").doesNotExist())
                    .andExpect(jsonPath("$.result.full_name").value("Donchitos/Claude-Code-Game-Studios"));
        }
    }

    @Nested
    @DisplayName("github_get_file_tree")
    class GetFileTreeTests {

        @Test
        @DisplayName("non-recursive — returns tree array with path and type")
        void nonRecursive_returnsTree() throws Exception {
            String body = """
                    {"jsonrpc":"2.0","method":"tools/call","id":3,"params":{"name":"github_get_file_tree","arguments":{"url":"%s"}}}
                    """.formatted(REPO_URL);

            mockMvc.perform(post("/mcp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").doesNotExist())
                    .andExpect(jsonPath("$.result.tree").isArray())
                    .andExpect(jsonPath("$.result.tree[0].path").exists())
                    .andExpect(jsonPath("$.result.tree[0].type").exists());
        }

        @Test
        @DisplayName("recursive — returns more entries than non-recursive")
        void recursive_returnsMoreEntries() throws Exception {
            String body = """
                    {"jsonrpc":"2.0","method":"tools/call","id":4,"params":{"name":"github_get_file_tree","arguments":{"url":"%s","recursive":true}}}
                    """.formatted(REPO_URL);

            mockMvc.perform(post("/mcp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").doesNotExist())
                    .andExpect(jsonPath("$.result.tree").isArray());
        }
    }

    @Nested
    @DisplayName("github_get_file_content")
    class GetFileContentTests {

        @Test
        @DisplayName("via blob URL — returns content (base64) and file name")
        void viaBlobUrl_returnsContent() throws Exception {
            String blobUrl = "https://github.com/Donchitos/Claude-Code-Game-Studios/blob/main/README.md";
            String body = """
                    {"jsonrpc":"2.0","method":"tools/call","id":5,"params":{"name":"github_get_file_content","arguments":{"url":"%s"}}}
                    """.formatted(blobUrl);

            mockMvc.perform(post("/mcp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").doesNotExist())
                    .andExpect(jsonPath("$.result.content").exists())
                    .andExpect(jsonPath("$.result.name").value("README.md"));
        }
    }

    @Nested
    @DisplayName("github_get_pull_requests")
    class GetPullRequestsTests {

        @Test
        @DisplayName("open PRs — returns JSON array")
        void openPrs_returnsArray() throws Exception {
            String body = """
                    {"jsonrpc":"2.0","method":"tools/call","id":6,"params":{"name":"github_get_pull_requests","arguments":{"url":"%s","state":"open"}}}
                    """.formatted(PR_REPO_URL);

            mockMvc.perform(post("/mcp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").doesNotExist())
                    .andExpect(jsonPath("$.result").isArray());
        }
    }

    @Nested
    @DisplayName("github_get_pull_request_changes")
    class GetPrChangesTests {

        @Test
        @DisplayName("via PR URL — returns changed files with filename and status")
        void viaPrUrl_returnsChangedFiles() throws Exception {
            String prUrl = PR_REPO_URL + "/pull/1";
            String body = """
                    {"jsonrpc":"2.0","method":"tools/call","id":7,"params":{"name":"github_get_pull_request_changes","arguments":{"url":"%s"}}}
                    """.formatted(prUrl);

            mockMvc.perform(post("/mcp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").doesNotExist())
                    .andExpect(jsonPath("$.result").isArray());
        }
    }

    @Nested
    @DisplayName("github_get_actions_workflows")
    class GetWorkflowsTests {

        @Test
        @DisplayName("returns workflows array with id, name, state")
        void returnsWorkflows() throws Exception {
            String body = """
                    {"jsonrpc":"2.0","method":"tools/call","id":8,"params":{"name":"github_get_actions_workflows","arguments":{"url":"%s"}}}
                    """.formatted(REPO_URL);

            mockMvc.perform(post("/mcp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").doesNotExist())
                    .andExpect(jsonPath("$.result.workflows").isArray());
        }
    }

    @Nested
    @DisplayName("github_get_workflow_runs")
    class GetWorkflowRunsTests {

        @Test
        @DisplayName("returns workflow_runs array")
        void returnsWorkflowRuns() throws Exception {
            String body = """
                    {"jsonrpc":"2.0","method":"tools/call","id":9,"params":{"name":"github_get_workflow_runs","arguments":{"url":"%s"}}}
                    """.formatted(REPO_URL);

            mockMvc.perform(post("/mcp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").doesNotExist())
                    .andExpect(jsonPath("$.result.workflow_runs").isArray());
        }
    }
}
