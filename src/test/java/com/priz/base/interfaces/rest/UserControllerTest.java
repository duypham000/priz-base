package com.priz.base.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priz.base.application.features.users.UserService;
import com.priz.base.application.features.users.dto.ChangePasswordRequest;
import com.priz.base.application.features.users.dto.UpdateProfileRequest;
import com.priz.base.application.features.users.dto.UserDetailResponse;
import com.priz.base.testutil.TestFixtures;
import com.priz.common.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserService userService;

    @Test
    void getMyProfile_should_return200_withUserDetail() throws Exception {
        UserDetailResponse detail = UserDetailResponse.builder()
                .id(TestFixtures.TEST_USER_ID)
                .username(TestFixtures.TEST_USERNAME)
                .email(TestFixtures.TEST_EMAIL)
                .fullName(TestFixtures.TEST_FULL_NAME)
                .role("USER")
                .isActive(true)
                .createdAt(Instant.now())
                .build();

        when(userService.getUserDetail()).thenReturn(detail);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TestFixtures.TEST_USER_ID))
                .andExpect(jsonPath("$.data.email").value(TestFixtures.TEST_EMAIL));
    }

    @Test
    void updateProfile_should_return200_withUpdatedDetail() throws Exception {
        UserDetailResponse detail = UserDetailResponse.builder()
                .id(TestFixtures.TEST_USER_ID)
                .username(TestFixtures.TEST_USERNAME)
                .email(TestFixtures.TEST_EMAIL)
                .fullName("New Name")
                .role("USER")
                .isActive(true)
                .build();

        when(userService.updateProfile(any())).thenReturn(detail);

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("New Name")
                .build();

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("New Name"));
    }

    @Test
    void changePassword_should_return200_onSuccess() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPassword123")
                .newPassword("newPassword123")
                .build();

        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    void deleteAccount_should_return200_onSuccess() throws Exception {
        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account deleted successfully"));
    }
}
