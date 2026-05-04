package com.priz.base.application.integration.github.util;

import java.net.URI;
import java.net.URISyntaxException;

public class GithubUrlParser {

    private GithubUrlParser() {}

    /**
     * Parses a GitHub URL into its structural parts.
     *
     * Supported formats:
     *   https://github.com/{owner}/{repo}
     *   https://github.com/{owner}/{repo}/tree/{branch}
     *   https://github.com/{owner}/{repo}/blob/{branch}/{path...}
     *   https://github.com/{owner}/{repo}/pull/{number}
     *   https://github.com/{owner}/{repo}/actions/workflows/{...}
     */
    public static GithubUrlParts parse(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("GitHub URL must not be blank");
        }
        try {
            URI uri = new URI(url.trim());
            String[] segments = uri.getPath().replaceAll("^/|/$", "").split("/", -1);
            // segments[0]=owner, segments[1]=repo, segments[2]=verb, ...

            if (segments.length < 2) {
                throw new IllegalArgumentException("Cannot extract owner/repo from GitHub URL: " + url);
            }

            String owner = segments[0];
            String repo = segments[1];
            String ref = null;
            String path = null;
            Integer pullNumber = null;

            if (segments.length >= 4) {
                String verb = segments[2];
                switch (verb) {
                    case "tree" -> ref = segments[3];
                    case "blob" -> {
                        ref = segments[3];
                        if (segments.length > 4) {
                            path = String.join("/", java.util.Arrays.copyOfRange(segments, 4, segments.length));
                        }
                    }
                    case "pull" -> {
                        try {
                            pullNumber = Integer.parseInt(segments[3]);
                        } catch (NumberFormatException ignored) {}
                    }
                    default -> {}
                }
            }

            return new GithubUrlParts(owner, repo, ref, path, pullNumber);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid GitHub URL: " + url, e);
        }
    }
}
