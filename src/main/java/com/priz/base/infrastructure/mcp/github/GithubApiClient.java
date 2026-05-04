package com.priz.base.infrastructure.mcp.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.priz.base.application.integration.github.GithubService;
import com.priz.base.application.integration.github.util.GithubUrlParser;
import com.priz.base.application.integration.github.util.GithubUrlParts;
import com.priz.base.application.mcp.provider.McpToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClientException;

@Slf4j
@RequiredArgsConstructor
public class GithubApiClient {

    private final GithubService githubService;

    public McpToolResult getRepository(JsonNode args) {
        try {
            GithubUrlParts parts = resolveParts(args);
            Object response = githubService.getRepository(parts.owner(), parts.repo());
            return McpToolResult.success(response);
        } catch (RestClientException | IllegalArgumentException e) {
            return McpToolResult.error("GitHub error: " + e.getMessage());
        }
    }

    public McpToolResult getFileTree(JsonNode args) {
        try {
            GithubUrlParts parts = resolveParts(args);
            String ref = firstNonBlank(parts.ref(), args.path("ref").asText(null));
            boolean recursive = args.path("recursive").asBoolean(false);
            Object response = githubService.getFileTree(parts.owner(), parts.repo(), ref, recursive);
            return McpToolResult.success(response);
        } catch (RestClientException | IllegalArgumentException e) {
            return McpToolResult.error("GitHub error: " + e.getMessage());
        }
    }

    public McpToolResult getFileContent(JsonNode args) {
        try {
            GithubUrlParts parts = resolveParts(args);
            String filePath = firstNonBlank(parts.path(), args.path("path").asText(null));
            if (filePath == null) {
                return McpToolResult.error("'path' is required");
            }
            String ref = firstNonBlank(parts.ref(), args.path("ref").asText(null));
            Object response = githubService.getFileContent(parts.owner(), parts.repo(), filePath, ref);
            return McpToolResult.success(response);
        } catch (RestClientException | IllegalArgumentException e) {
            return McpToolResult.error("GitHub error: " + e.getMessage());
        }
    }

    public McpToolResult getPullRequests(JsonNode args) {
        try {
            GithubUrlParts parts = resolveParts(args);
            String state = args.path("state").asText("open");
            int page = args.path("page").asInt(1);
            int perPage = args.path("perPage").asInt(30);
            Object response = githubService.getPullRequests(parts.owner(), parts.repo(), state, page, perPage);
            return McpToolResult.success(response);
        } catch (RestClientException | IllegalArgumentException e) {
            return McpToolResult.error("GitHub error: " + e.getMessage());
        }
    }

    public McpToolResult getPullRequestChanges(JsonNode args) {
        try {
            GithubUrlParts parts = resolveParts(args);
            Integer pullNumber = parts.pullNumber() != null
                    ? parts.pullNumber()
                    : (args.path("pullNumber").isMissingNode() ? null : args.path("pullNumber").asInt());
            if (pullNumber == null) {
                return McpToolResult.error("'pullNumber' is required");
            }
            Object response = githubService.getPullRequestChanges(parts.owner(), parts.repo(), pullNumber);
            return McpToolResult.success(response);
        } catch (RestClientException | IllegalArgumentException e) {
            return McpToolResult.error("GitHub error: " + e.getMessage());
        }
    }

    public McpToolResult getActionsWorkflows(JsonNode args) {
        try {
            GithubUrlParts parts = resolveParts(args);
            Object response = githubService.getActionsWorkflows(parts.owner(), parts.repo());
            return McpToolResult.success(response);
        } catch (RestClientException | IllegalArgumentException e) {
            return McpToolResult.error("GitHub error: " + e.getMessage());
        }
    }

    public McpToolResult getWorkflowRuns(JsonNode args) {
        try {
            GithubUrlParts parts = resolveParts(args);
            String branch = args.path("branch").asText(null);
            String workflowId = args.path("workflowId").asText(null);
            Object response = githubService.getWorkflowRuns(parts.owner(), parts.repo(), workflowId, branch);
            return McpToolResult.success(response);
        } catch (RestClientException | IllegalArgumentException e) {
            return McpToolResult.error("GitHub error: " + e.getMessage());
        }
    }

    private GithubUrlParts resolveParts(JsonNode args) {
        String url = args.path("url").asText(null);
        if (url != null && !url.isBlank()) {
            return GithubUrlParser.parse(url);
        }
        String owner = args.path("owner").asText(null);
        String repo = args.path("repo").asText(null);
        if (owner == null || owner.isBlank() || repo == null || repo.isBlank()) {
            throw new IllegalArgumentException("Either 'url' or ('owner' + 'repo') are required");
        }
        return new GithubUrlParts(owner, repo, null, null, null);
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
