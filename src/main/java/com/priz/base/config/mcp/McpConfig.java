package com.priz.base.config.mcp;

import com.priz.base.application.integration.github.GithubService;
import com.priz.base.application.integration.github.impl.GithubServiceImpl;
import com.priz.base.application.mcp.provider.github.GithubProperties;
import com.priz.base.infrastructure.mcp.github.GithubApiClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Wires infrastructure API clients and services for each enabled MCP provider.
 * Auth headers (API keys, tokens) are set here.
 */
@Configuration
@EnableConfigurationProperties({McpProperties.class, GithubProperties.class})
public class McpConfig {

    @Bean
    @ConditionalOnProperty(prefix = "mcp.github", name = "enabled", havingValue = "true")
    public GithubService githubService(GithubProperties props) {
        RestClient restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getToken())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
        return new GithubServiceImpl(restClient);
    }

    @Bean
    @ConditionalOnProperty(prefix = "mcp.github", name = "enabled", havingValue = "true")
    public GithubApiClient githubApiClient(GithubService githubService) {
        return new GithubApiClient(githubService);
    }
}
