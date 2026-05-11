package com.priz.base.interfaces.rest.admin;

import com.priz.base.application.features.permission.PermissionGroupService;
import com.priz.base.application.features.permission.dto.CreatePermissionGroupRequest;
import com.priz.base.common.response.ApiResponse;
import com.priz.base.domain.mysql_priz_base.model.PermissionGroupMappingModel;
import com.priz.base.domain.mysql_priz_base.model.PermissionGroupModel;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/permission-groups")
@RequiredArgsConstructor
public class PermissionGroupAdminController {

    private final PermissionGroupService permissionGroupService;

    @Secured(permissions = {PermissionAction.CREATE}, customKey = "permission_group")
    @PostMapping
    public ResponseEntity<ApiResponse<PermissionGroupModel>> create(
            @Valid @RequestBody CreatePermissionGroupRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created(permissionGroupService.create(request)));
    }

    @Secured(permissions = {PermissionAction.READ}, customKey = "permission_group")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PermissionGroupModel>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(permissionGroupService.getAll()));
    }

    @Secured(permissions = {PermissionAction.READ}, customKey = "permission_group")
    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<PermissionGroupModel>> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(permissionGroupService.getByCode(code)));
    }

    @Secured(permissions = {PermissionAction.READ}, customKey = "permission_group")
    @GetMapping("/{code}/permissions")
    public ResponseEntity<ApiResponse<List<String>>> getPermissions(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(permissionGroupService.getPermissionCodes(code)));
    }

    @Secured(permissions = {PermissionAction.UPDATE}, customKey = "permission_group")
    @PostMapping("/{code}/permissions")
    public ResponseEntity<ApiResponse<PermissionGroupMappingModel>> addPermission(
            @PathVariable String code, @RequestParam String permissionCode) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created(
                        permissionGroupService.addPermission(code, permissionCode)));
    }

    @Secured(permissions = {PermissionAction.UPDATE}, customKey = "permission_group")
    @DeleteMapping("/{code}/permissions/{permissionCode}")
    public ResponseEntity<ApiResponse<Void>> removePermission(
            @PathVariable String code, @PathVariable String permissionCode) {
        permissionGroupService.removePermission(code, permissionCode);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Secured(permissions = {PermissionAction.DELETE}, customKey = "permission_group")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        permissionGroupService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
