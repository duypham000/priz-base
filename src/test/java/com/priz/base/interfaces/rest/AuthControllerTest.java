package com.priz.base.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priz.base.application.features.auth.AuthService;
import com.priz.base.application.features.auth.dto.AuthResponse;
import com.priz.base.application.features.auth.dto.LoginRequest;
import com.priz.base.application.features.auth.dto.RegisterRequest;
import com.priz.common.exception.UnauthorizedException;
import com.priz.base.testutil.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @Test
    void register_should_return201_withAuthResponse() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(TestFixtures.TEST_ACCESS_TOKEN)
                .refreshToken(TestFixtures.TEST_REFRESH_TOKEN)
                .tokenType("Bearer")
                .expiresIn(3600)
                .user(AuthResponse.UserInfo.builder()
                        .id(TestFixtures.TEST_USER_ID)
                        .email(TestFixtures.TEST_EMAIL)
                        .username(TestFixtures.TEST_USERNAME)
                        .role("USER")
                        .build())
                .build();

        when(authService.register(any())).thenReturn(authResponse);

        RegisterRequest request = TestFixtures.createRegisterRequest();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.accessToken").value(TestFixtures.TEST_ACCESS_TOKEN))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void register_should_return400_whenValidationFails() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("")
                .email("invalid-email")
                .password("short")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_should_return200_withAuthResponse() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(TestFixtures.TEST_ACCESS_TOKEN)
                .refreshToken(TestFixtures.TEST_REFRESH_TOKEN)
                .tokenType("Bearer")
                .expiresIn(3600)
                .build();

        when(authService.login(any())).thenReturn(authResponse);

        LoginRequest request = TestFixtures.createLoginRequest();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value(TestFixtures.TEST_ACCESS_TOKEN));
    }

    @Test
    void login_should_return401_whenInvalidCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new UnauthorizedException("Invalid email or password"));

        LoginRequest request = TestFixtures.createLoginRequest();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgotPassword_should_return200_always() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If the email exists, a reset link has been sent"));
    }

    @Test
    void resetPassword_should_return200_onSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"valid-token\",\"newPassword\":\"newPassword123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));
    }
}
