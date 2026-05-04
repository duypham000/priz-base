package com.priz.base.config.mcp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("mcp")
public class McpProperties {
    private boolean enabled = true;
}
