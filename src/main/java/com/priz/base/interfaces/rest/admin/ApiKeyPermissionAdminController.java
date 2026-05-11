package com.priz.base.interfaces.rest.admin;

import com.priz.base.application.features.apikey.ApiKeyPermissionService;
import com.priz.base.common.response.ApiResponse;
import com.priz.base.domain.mysql_priz_base.model.ApiKeyPermissionGroupCredentialsModel;
import com.priz.base.domain.mysql_priz_base.model.ApiKeyPermissionMappingModel;
import com.priz.common.security.annotation.Secured;
import com.priz.common.security.permission.PermissionAction;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/admin/api-keys/{apiKeyId}/permissions")
@RequiredArgsConstructor
public class ApiKeyPermissionAdminController {

    private final ApiKeyPermissionService apiKeyPermissionService;

    @Secured(permissions = {PermissionAction.READ}, customKey = "api_key_permission")
    @GetMapping("/effective")
    public ResponseEntity<ApiResponse<Set<String>>> getEffective(@PathVariable String apiKeyId) {
        return ResponseEntity.ok(
                ApiResponse.success(apiKeyPermissionService.getEffectivePermissions(apiKeyId)));
    }

    @Secured(permissions = {PermissionAction.UPDATE}, customKey = "api_key_permission")
    @PostMapping("/direct")
    public ResponseEntity<ApiResponse<ApiKeyPermissionMappingModel>> addDirect(
            @PathVariable String apiKeyId, @RequestParam String permissionCode) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created(
                        apiKeyPermissionService.addDirectPermission(apiKeyId, permissionCode)));
    }

    @Secured(permissions = {PermissionAction.UPDATE}, customKey = "api_key_permission")
    @DeleteMapping("/direct/{permissionCode}")
    public ResponseEntity<ApiResponse<Void>> removeDirect(
            @PathVariable String apiKeyId, @PathVariable String permissionCode) {
        apiKeyPermissionService.removeDirectPermission(apiKeyId, permissionCode);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Secured(permissions = {PermissionAction.UPDATE}, customKey = "api_key_permission")
    @PostMapping("/groups")
    public ResponseEntity<ApiResponse<ApiKeyPermissionGroupCredentialsModel>> addGroup(
            @PathVariable String apiKeyId, @RequestParam String groupCode) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created(
                        apiKeyPermissionService.addPermissionGroup(apiKeyId, groupCode)));
    }

    @Secured(permissions = {PermissionAction.UPDATE}, customKey = "api_key_permission")
    @DeleteMapping("/groups/{groupCode}")
    public ResponseEntity<ApiResponse<Void>> removeGroup(
            @PathVariable String apiKeyId, @PathVariable String groupCode) {
        apiKeyPermissionService.removePermissionGroup(apiKeyId, groupCode);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
