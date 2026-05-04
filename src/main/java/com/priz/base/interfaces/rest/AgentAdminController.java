package com.priz.base.interfaces.rest;

import com.priz.base.application.features.agent.AgentProxyService;
import com.priz.base.application.features.agent.dto.ModelDto;
import com.priz.base.application.features.agent.dto.ProviderDto;
import com.priz.base.application.features.agent.dto.SessionDto;
import com.priz.base.application.features.agent.dto.WindowUsageDto;
import com.priz.base.common.response.ApiResponse;
import com.priz.common.security.annotation.Secured;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
@RestController
@RequestMapping("/api/admin/agent")
@Secured(roles = "ADMIN")
@RequiredArgsConstructor
*/
public class AgentAdminController {
    /*
    private final AgentProxyService agentProxyService;

    @GetMapping("/models")
    @Operation(summary = "Danh sách model được agent hỗ trợ")
    public ResponseEntity<ApiResponse<List<ModelDto>>> listModels() {
        return ResponseEntity.ok(ApiResponse.success(agentProxyService.listModels()));
    }

    @GetMapping("/providers")
    @Operation(summary = "Danh sách LLM provider cấu hình trong agent")
    public ResponseEntity<ApiResponse<List<ProviderDto>>> listProviders() {
        return ResponseEntity.ok(ApiResponse.success(agentProxyService.listProviders()));
    }

    @GetMapping("/sessions")
    @Operation(summary = "Danh sách session của user — onlyActive=true để chỉ lấy session đang chạy")
    public ResponseEntity<ApiResponse<List<SessionDto>>> listSessions(
            @RequestParam String userId,
            @RequestParam(defaultValue = "false") boolean onlyActive) {
        return ResponseEntity.ok(ApiResponse.success(agentProxyService.listSessions(userId, onlyActive)));
    }

    @GetMapping("/users/{userId}/token-usage")
    @Operation(summary = "Token usage theo cửa sổ (daily/weekly/monthly) của một user")
    public ResponseEntity<ApiResponse<List<WindowUsageDto>>> getUserTokenUsage(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(agentProxyService.getTokenUsage("USER", userId)));
    }
    */
}
