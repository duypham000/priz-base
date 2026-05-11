package com.priz.base.interfaces.rest.admin;

import com.priz.base.application.features.apikey.ApiKeyService;
import com.priz.base.application.features.apikey.dto.CreateApiKeyRequest;
import com.priz.base.application.features.apikey.dto.CreateApiKeyResponse;
import com.priz.base.common.response.ApiResponse;
import com.priz.base.domain.mysql_priz_base.model.ApiKeyCredentialsModel;
import com.priz.common.security.annotation.Secured;
import com.priz.common.security.permission.PermissionAction;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/api-keys")
@RequiredArgsConstructor
public class ApiKeyAdminController {

    private final ApiKeyService apiKeyService;

    @Secured(permissions = {PermissionAction.CREATE}, customKey = "api_key")
    @PostMapping
    public ResponseEntity<ApiResponse<CreateApiKeyResponse>> create(
            @Valid @RequestBody CreateApiKeyRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(apiKeyService.create(request)));
    }

    @Secured(permissions = {PermissionAction.UPDATE}, customKey = "api_key")
    @PostMapping("/{id}/regenerate")
    public ResponseEntity<ApiResponse<CreateApiKeyResponse>> regenerate(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(apiKeyService.regenerate(id)));
    }

    @Secured(permissions = {PermissionAction.UPDATE}, customKey = "api_key")
    @PostMapping("/{id}/revoke")
    public ResponseEntity<ApiResponse<Void>> revoke(@PathVariable String id) {
        apiKeyService.revoke(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Secured(permissions = {PermissionAction.READ}, customKey = "api_key")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ApiKeyCredentialsModel>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(apiKeyService.getAll()));
    }

    @Secured(permissions = {PermissionAction.READ}, customKey = "api_key")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApiKeyCredentialsModel>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(apiKeyService.getById(id)));
    }

    @Secured(permissions = {PermissionAction.DELETE}, customKey = "api_key")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        apiKeyService.revoke(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
