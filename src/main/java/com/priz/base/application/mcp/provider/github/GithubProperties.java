package com.priz.base.application.mcp.provider.github;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("mcp.github")
public class GithubProperties {
    private boolean enabled = false;
    private String token;
    private String baseUrl = "https://api.github.com";
}
