package com.priz.base.application.integration.github;

public interface GithubService {
    Object getRepository(String owner, String repo);
    Object getFileTree(String owner, String repo, String ref, boolean recursive);
    Object getFileContent(String owner, String repo, String path, String ref);
    Object getPullRequests(String owner, String repo, String state, int page, int perPage);
    Object getPullRequestChanges(String owner, String repo, int pullNumber);
    Object getActionsWorkflows(String owner, String repo);
    Object getWorkflowRuns(String owner, String repo, String workflowId, String branch);
}
