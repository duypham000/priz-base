package com.priz.base.interfaces.rest.admin;

import com.priz.base.application.features.permission.UserPermissionService;
import com.priz.base.common.response.ApiResponse;
import com.priz.base.domain.mysql_priz_base.model.UserPermissionGroupModel;
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

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/user-permissions")
@RequiredArgsConstructor
public class UserPermissionAdminController {

    private final UserPermissionService userPermissionService;

    @Secured(permissions = {PermissionAction.CREATE}, customKey = "user_permission")
    @PostMapping("/{userId}/groups")
    public ResponseEntity<ApiResponse<UserPermissionGroupModel>> assignGroup(
            @PathVariable String userId, @RequestParam String groupCode) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created(
                        userPermissionService.assignGroup(userId, groupCode)));
    }

    @Secured(permissions = {PermissionAction.DELETE}, customKey = "user_permission")
    @DeleteMapping("/{userId}/groups/{groupCode}")
    public ResponseEntity<ApiResponse<Void>> removeGroup(
            @PathVariable String userId, @PathVariable String groupCode) {
        userPermissionService.removeGroup(userId, groupCode);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Secured(permissions = {PermissionAction.READ}, customKey = "user_permission")
    @GetMapping("/{userId}/groups")
    public ResponseEntity<ApiResponse<List<String>>> getGroups(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(userPermissionService.getGroupCodes(userId)));
    }

    @Secured(permissions = {PermissionAction.READ}, customKey = "user_permission")
    @GetMapping("/{userId}/effective-permissions")
    public ResponseEntity<ApiResponse<Set<String>>> getEffectivePermissions(
            @PathVariable String userId) {
        return ResponseEntity.ok(
                ApiResponse.success(userPermissionService.getPermissionCodes(userId)));
    }
}
