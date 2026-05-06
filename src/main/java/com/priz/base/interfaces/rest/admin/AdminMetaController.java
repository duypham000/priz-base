package com.priz.base.interfaces.rest.admin;

import com.priz.base.application.admin.core.registry.AdminResourceRegistry;
import com.priz.base.common.response.ApiResponse;
import com.priz.common.security.annotation.Secured;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminMetaController {

    private final AdminResourceRegistry registry;

    public AdminMetaController(AdminResourceRegistry registry) {
        this.registry = registry;
    }

    @Secured(roles = {"ADMIN"})
    @GetMapping("/resources")
    public ResponseEntity<ApiResponse<List<String>>> listResources() {
        return ResponseEntity.ok(ApiResponse.success(registry.listResources()));
    }
}
