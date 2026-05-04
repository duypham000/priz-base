package com.priz.base.application.integration.github.impl;

import com.priz.base.application.integration.github.GithubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class GithubServiceImpl implements GithubService {

    private final RestClient restClient;

    @Override
    public Object getRepository(String owner, String repo) {
        log.debug("GitHub getRepository: {}/{}", owner, repo);
        return restClient.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .retrieve()
                .body(Object.class);
    }

    @Override
    public Object getFileTree(String owner, String repo, String ref, boolean recursive) {
        String finalRef = (ref == null || ref.isBlank()) ? "HEAD" : ref;
        log.debug("GitHub getFileTree: {}/{} ref={} recursive={}", owner, repo, finalRef, recursive);
        
        String uri = UriComponentsBuilder
                .fromPath("/repos/{owner}/{repo}/git/trees/{ref}")
                .queryParam("recursive", recursive ? "1" : "0")
                .buildAndExpand(owner, repo, finalRef)
                .toUriString();

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(Object.class);
    }

    @Override
    public Object getFileContent(String owner, String repo, String path, String ref) {
        log.debug("GitHub getFileContent: {}/{} path={} ref={}", owner, repo, path, ref);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromPath("/repos/{owner}/{repo}/contents/{path}")
                .uriVariables(Map.of(
                        "owner", owner,
                        "repo", repo,
                        "path", path
                ));
        if (ref != null && !ref.isBlank()) {
            uriBuilder.queryParam("ref", ref);
        }

        return restClient.get()
                .uri(uriBuilder.build().toUriString())
                .retrieve()
                .body(Object.class);
    }

    @Override
    public Object getPullRequests(String owner, String repo, String state, int page, int perPage) {
        String finalState = (state == null || state.isBlank()) ? "open" : state;
        log.debug("GitHub getPullRequests: {}/{} state={}", owner, repo, finalState);
        
        String uri = UriComponentsBuilder
                .fromPath("/repos/{owner}/{repo}/pulls")
                .queryParam("state", finalState)
                .queryParam("page", page)
                .queryParam("per_page", perPage)
                .buildAndExpand(owner, repo)
                .toUriString();

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(Object.class);
    }

    @Override
    public Object getPullRequestChanges(String owner, String repo, int pullNumber) {
        log.debug("GitHub getPullRequestChanges: {}/{} PR#{}", owner, repo, pullNumber);
        return restClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{number}/files", owner, repo, pullNumber)
                .retrieve()
                .body(Object.class);
    }

    @Override
    public Object getActionsWorkflows(String owner, String repo) {
        log.debug("GitHub getActionsWorkflows: {}/{}", owner, repo);
        return restClient.get()
                .uri("/repos/{owner}/{repo}/actions/workflows", owner, repo)
                .retrieve()
                .body(Object.class);
    }

    @Override
    public Object getWorkflowRuns(String owner, String repo, String workflowId, String branch) {
        log.debug("GitHub getWorkflowRuns: {}/{} workflowId={} branch={}", owner, repo, workflowId, branch);

        UriComponentsBuilder uriBuilder;
        if (workflowId != null && !workflowId.isBlank()) {
            uriBuilder = UriComponentsBuilder
                    .fromPath("/repos/{owner}/{repo}/actions/workflows/{workflowId}/runs")
                    .uriVariables(Map.of(
                            "owner", owner,
                            "repo", repo,
                            "workflowId", workflowId
                    ));
        } else {
            uriBuilder = UriComponentsBuilder
                    .fromPath("/repos/{owner}/{repo}/actions/runs")
                    .uriVariables(Map.of(
                            "owner", owner,
                            "repo", repo
                    ));
        }
        if (branch != null && !branch.isBlank()) {
            uriBuilder.queryParam("branch", branch);
        }

        return restClient.get()
                .uri(uriBuilder.build().toUriString())
                .retrieve()
                .body(Object.class);
    }
}
