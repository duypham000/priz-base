package com.priz.base.interfaces.rest.admin;

import com.priz.base.application.features.permission.PermissionService;
import com.priz.base.application.features.permission.dto.CreatePermissionRequest;
import com.priz.base.common.response.ApiResponse;
import com.priz.base.domain.mysql_priz_base.model.PermissionModel;
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
@RequestMapping("/api/admin/permissions")
@RequiredArgsConstructor
public class PermissionAdminController {

    private final PermissionService permissionService;

    @Secured(permissions = {PermissionAction.CREATE}, customKey = "permission")
    @PostMapping
    public ResponseEntity<ApiResponse<PermissionModel>> create(
            @Valid @RequestBody CreatePermissionRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(permissionService.create(request)));
    }

    @Secured(permissions = {PermissionAction.READ}, customKey = "permission")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PermissionModel>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(permissionService.getAll()));
    }

    @Secured(permissions = {PermissionAction.READ}, customKey = "permission")
    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<PermissionModel>> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(permissionService.getByCode(code)));
    }

    @Secured(permissions = {PermissionAction.DELETE}, customKey = "permission")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        permissionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
