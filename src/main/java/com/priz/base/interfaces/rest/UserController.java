package com.priz.base.interfaces.rest;

import com.priz.base.application.features.users.UserService;
import com.priz.base.application.features.users.dto.ChangePasswordRequest;
import com.priz.base.application.features.users.dto.UpdateProfileRequest;
import com.priz.base.application.features.users.dto.UserDetailResponse;
import com.priz.base.common.response.ApiResponse;
import com.priz.common.security.annotation.Secured;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management endpoints")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Secured
    @GetMapping("/me")
    @Operation(summary = "Get current user detail")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getMyProfile() {
        UserDetailResponse response = userService.getUserDetail();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Secured
    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        UserDetailResponse response = userService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Secured
    @PutMapping("/me/password")
    @Operation(summary = "Change current user password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @Secured
    @DeleteMapping("/me")
    @Operation(summary = "Delete (deactivate) current user account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount() {
        userService.deleteAccount();
        return ResponseEntity.ok(ApiResponse.success("Account deleted successfully", null));
    }
}
