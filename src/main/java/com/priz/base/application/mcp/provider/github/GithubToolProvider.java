package com.priz.base.application.mcp.provider.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.priz.base.application.mcp.provider.McpToolDefinition;
import com.priz.base.application.mcp.provider.McpToolProvider;
import com.priz.base.application.mcp.provider.McpToolResult;
import com.priz.base.infrastructure.mcp.github.GithubApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mcp.github", name = "enabled", havingValue = "true")
public class GithubToolProvider implements McpToolProvider {

    private final GithubApiClient githubApiClient;

    @Override
    public String providerName() {
        return "github";
    }

    @Override
    public List<McpToolDefinition> getToolDefinitions() {
        return List.of(
                new McpToolDefinition(
                        "github_get_repository",
                        "Get repository info and metadata. Accepts a GitHub URL (https://github.com/{owner}/{repo}) or separate owner + repo params.",
                        buildSchema(new String[][]{
                                {"url", "string", "GitHub repository URL (alternative to owner+repo)", "false"},
                                {"owner", "string", "Repository owner / organization (alternative to url)", "false"},
                                {"repo", "string", "Repository name (alternative to url)", "false"}
                        })
                ),
                new McpToolDefinition(
                        "github_get_file_tree",
                        "Get file and folder tree at a given ref (branch, tag, or commit SHA). Accepts a GitHub URL or owner+repo params.",
                        buildSchema(new String[][]{
                                {"url", "string", "GitHub URL — can include /tree/{branch} to set ref", "false"},
                                {"owner", "string", "Repository owner (alternative to url)", "false"},
                                {"repo", "string", "Repository name (alternative to url)", "false"},
                                {"ref", "string", "Branch, tag, or commit SHA (default: HEAD)", "false"},
                                {"recursive", "boolean", "If true, returns the full recursive tree (default: false)", "false"}
                        })
                ),
                new McpToolDefinition(
                        "github_get_file_content",
                        "Get file content at a given ref. Accepts a blob URL (https://github.com/{owner}/{repo}/blob/{branch}/{path}) or explicit params.",
                        buildSchema(new String[][]{
                                {"url", "string", "GitHub blob URL — automatically extracts path and ref", "false"},
                                {"owner", "string", "Repository owner (alternative to url)", "false"},
                                {"repo", "string", "Repository name (alternative to url)", "false"},
                                {"path", "string", "File path within the repository (alternative to url)", "false"},
                                {"ref", "string", "Branch, tag, or commit SHA (optional)", "false"}
                        })
                ),
                new McpToolDefinition(
                        "github_get_pull_requests",
                        "List pull requests with optional state filter. Accepts a GitHub repo URL or owner+repo params.",
                        buildSchema(new String[][]{
                                {"url", "string", "GitHub repository URL", "false"},
                                {"owner", "string", "Repository owner (alternative to url)", "false"},
                                {"repo", "string", "Repository name (alternative to url)", "false"},
                                {"state", "string", "Filter by state: open, closed, or all (default: open)", "false"},
                                {"page", "integer", "Page number (default: 1)", "false"},
                                {"perPage", "integer", "Results per page (default: 30)", "false"}
                        })
                ),
                new McpToolDefinition(
                        "github_get_pull_request_changes",
                        "Get changed files and diff details for a pull request. Accepts a PR URL (https://github.com/{owner}/{repo}/pull/{number}) or explicit params.",
                        buildSchema(new String[][]{
                                {"url", "string", "GitHub PR URL — automatically extracts PR number", "false"},
                                {"owner", "string", "Repository owner (alternative to url)", "false"},
                                {"repo", "string", "Repository name (alternative to url)", "false"},
                                {"pullNumber", "integer", "Pull request number (alternative to url)", "false"}
                        })
                ),
                new McpToolDefinition(
                        "github_get_actions_workflows",
                        "List all GitHub Actions workflows in a repository.",
                        buildSchema(new String[][]{
                                {"url", "string", "GitHub repository URL", "false"},
                                {"owner", "string", "Repository owner (alternative to url)", "false"},
                                {"repo", "string", "Repository name (alternative to url)", "false"}
                        })
                ),
                new McpToolDefinition(
                        "github_get_workflow_runs",
                        "Get GitHub Actions workflow run history, optionally filtered by workflow ID and branch.",
                        buildSchema(new String[][]{
                                {"url", "string", "GitHub repository URL", "false"},
                                {"owner", "string", "Repository owner (alternative to url)", "false"},
                                {"repo", "string", "Repository name (alternative to url)", "false"},
                                {"workflowId", "string", "Workflow ID or filename (optional — omit to list all runs)", "false"},
                                {"branch", "string", "Filter runs by branch name (optional)", "false"}
                        })
                )
        );
    }

    @Override
    public McpToolResult callTool(String toolName, JsonNode arguments) {
        try {
            return switch (toolName) {
                case "github_get_repository"          -> githubApiClient.getRepository(arguments);
                case "github_get_file_tree"           -> githubApiClient.getFileTree(arguments);
                case "github_get_file_content"        -> githubApiClient.getFileContent(arguments);
                case "github_get_pull_requests"       -> githubApiClient.getPullRequests(arguments);
                case "github_get_pull_request_changes"-> githubApiClient.getPullRequestChanges(arguments);
                case "github_get_actions_workflows"   -> githubApiClient.getActionsWorkflows(arguments);
                case "github_get_workflow_runs"       -> githubApiClient.getWorkflowRuns(arguments);
                default                               -> McpToolResult.error("Unknown GitHub tool: " + toolName);
            };
        } catch (IllegalArgumentException e) {
            return McpToolResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("GitHub tool '{}' failed unexpectedly: {}", toolName, e.getMessage());
            return McpToolResult.error("Unexpected error: " + e.getMessage());
        }
    }

    private Map<String, Object> buildSchema(String[][] fields) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (String[] f : fields) {
            properties.put(f[0], Map.of("type", f[1], "description", f[2]));
        }
        return Map.of("type", "object", "properties", properties);
    }
}
