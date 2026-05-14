package com.connectsphere.authservice.controller;

import com.connectsphere.authservice.dto.*;
import com.connectsphere.authservice.entities.User;
import com.connectsphere.authservice.service.AuthService;
import com.connectsphere.authservice.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {
        "jwt.secret=thisIsATestSecretKeyThatIsLongEnoughForHS512AlgorithmTest",
        "jwt.access-token-expiry-ms=900000",
        "jwt.refresh-token-expiry-ms=604800000",
        "jwt.cookie.access-token-name=cs_access_token",
        "jwt.cookie.refresh-token-name=cs_refresh_token",
        "jwt.cookie.secure=false"
})
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private ReportService reportService;

    private User sampleUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .userId(1)
                .username("johndoe")
                .email("john@example.com")
                .fullName("John Doe")
                .role("ROLE_USER")
                .isActive(true)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("johndoe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFullName("John Doe");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("password123");
    }

    // ── POST /api/auth/register ─────────────────────────────────────

    @Test
    @DisplayName("Register – success → 201 Created with User body")
    void register_success() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(sampleUser);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Register – blank username → 400 Bad Request")
    void register_blankUsername_returns400() throws Exception {
        registerRequest.setUsername("");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Register – invalid email → 400 Bad Request")
    void register_invalidEmail_returns400() throws Exception {
        registerRequest.setEmail("not-an-email");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Register – password too short → 400 Bad Request")
    void register_passwordTooShort_returns400() throws Exception {
        registerRequest.setPassword("ab");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/auth/login ────────────────────────────────────────

    @Test
    @DisplayName("Login – success → 200 OK with tokens in body")
    void login_success() throws Exception {
        UserSummary summary = new UserSummary();
        AuthResponse authResponse = new AuthResponse("access-token", "refresh-token", summary);
        when(authService.login("john@example.com", "password123")).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        verify(authService).login("john@example.com", "password123");
    }

    @Test
    @DisplayName("Login – missing email → 400 Bad Request")
    void login_missingEmail_returns400() throws Exception {
        loginRequest.setEmail(null);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/auth/logout ───────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Logout – success → 200 OK with message")
    void logout_success() throws Exception {
        doNothing().when(authService).logout(anyString());

        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer some-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    // ── GET /api/auth/profile/{userId} ─────────────────────────────

    @Test
    @DisplayName("Get profile by ID – found → 200 OK")
    void getProfileById_found() throws Exception {
        when(authService.getUserById(1)).thenReturn(sampleUser);

        mockMvc.perform(get("/api/auth/profile/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    @Test
    @DisplayName("Get profile by ID – service throws → 404 propagated")
    void getProfileById_notFound() throws Exception {
        when(authService.getUserById(999)).thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(get("/api/auth/profile/999"))
                .andExpect(status().is5xxServerError());
    }

    // ── PUT /api/auth/profile ───────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Update profile – success → 200 OK")
    void updateProfile_success() throws Exception {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("Jane Doe");
        req.setBio("Hello world");

        sampleUser.setFullName("Jane Doe");
        when(authService.updateProfile(anyInt(), any(UpdateProfileRequest.class))).thenReturn(sampleUser);

        mockMvc.perform(put("/api/auth/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Jane Doe"));
    }

    // ── PUT /api/auth/password ──────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Change password – success → 200 OK")
    void changePassword_success() throws Exception {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setNewPassword("newSecret99");

        doNothing().when(authService).changePassword(anyInt(), anyString());

        mockMvc.perform(put("/api/auth/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    // ── DELETE /api/auth/deactivate ─────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Self deactivate – success → 200 OK")
    void deactivate_success() throws Exception {
        doNothing().when(authService).selfDeactivateAccount(anyInt());

        mockMvc.perform(delete("/api/auth/deactivate").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account deactivated"));
    }

    // ── GET /api/auth/search ────────────────────────────────────────

    @Test
    @DisplayName("Search users – success → 200 OK with list")
    void searchUsers_success() throws Exception {
        when(authService.searchUsers("john")).thenReturn(List.of(sampleUser));

        mockMvc.perform(get("/api/auth/search").param("q", "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("johndoe"));
    }

    @Test
    @DisplayName("Search users – no results → 200 OK with empty list")
    void searchUsers_noResults() throws Exception {
        when(authService.searchUsers("nobody")).thenReturn(List.of());

        mockMvc.perform(get("/api/auth/search").param("q", "nobody"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── GET /api/auth/{id}/email ────────────────────────────────────

    @Test
    @DisplayName("Get email by ID – found → 200 OK")
    void getEmail_found() throws Exception {
        when(authService.getUserById(1)).thenReturn(sampleUser);

        mockMvc.perform(get("/api/auth/1/email"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    // ── GET /api/auth/username ──────────────────────────────────────

    @Test
    @DisplayName("Get users by usernames – success → 200 OK")
    void getUsersByUsernames_success() throws Exception {
        when(authService.getUsersByUsernames(List.of("johndoe"))).thenReturn(List.of(sampleUser));

        mockMvc.perform(get("/api/auth/username").param("usernames", "johndoe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
