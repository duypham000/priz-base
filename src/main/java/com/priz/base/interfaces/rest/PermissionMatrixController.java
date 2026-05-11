package com.priz.base.interfaces.rest;

import com.priz.base.application.features.permission.admin.PermissionMatrixService;
import com.priz.base.common.response.ApiResponse;
import com.priz.common.security.annotation.Secured;
import com.priz.common.security.permission.PermissionAction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Permission Matrix", description = "Admin API để quản lý bitmask permissions theo group")
@RestController
@RequestMapping("/api/v1/admin/permissions")
@RequiredArgsConstructor
public class PermissionMatrixController {

    private final PermissionMatrixService permissionMatrixService;

    @Operation(summary = "Lấy permission matrix của một group")
    @GetMapping("/matrix")
    @Secured(permissions = {PermissionAction.READ}, customKey = "permission_admin", isGlobal = true)
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> getMatrix(
            @RequestParam String groupCode) {
        return ResponseEntity.ok(ApiResponse.success(permissionMatrixService.getMatrix(groupCode)));
    }

    @Operation(summary = "Cập nhật permission matrix cho một group")
    @PostMapping("/matrix")
    @Secured(permissions = {PermissionAction.UPDATE}, customKey = "permission_admin", isGlobal = true)
    public ResponseEntity<ApiResponse<Void>> updateMatrix(
            @RequestParam String groupCode,
            @RequestBody Map<String, List<String>> matrix) {
        permissionMatrixService.updateMatrix(groupCode, matrix);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
