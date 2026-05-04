package com.priz.base.application.mcp;

import com.priz.base.application.mcp.dto.McpRequest;
import com.priz.base.application.mcp.dto.McpResponse;

public interface McpDispatcher {
    McpResponse dispatch(McpRequest request);
}
