package com.priz.base.application.integration.github.util;

public record GithubUrlParts(
        String owner,
        String repo,
        String ref,
        String path,
        Integer pullNumber
) {}
