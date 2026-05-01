package com.connectsphere.authservice.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.connectsphere.authservice.dto.AuthResponse;
import com.connectsphere.authservice.dto.ChangePasswordRequest;
import com.connectsphere.authservice.dto.LoginRequest;
import com.connectsphere.authservice.dto.RegisterRequest;
import com.connectsphere.authservice.dto.UpdateProfileRequest;
import com.connectsphere.authservice.entities.User;
import com.connectsphere.authservice.service.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthService authService;

	@Value("${jwt.cookie.access-token-name:cs_access_token}")
	private String accessCookieName;

	@Value("${jwt.cookie.refresh-token-name:cs_refresh_token}")
	private String refreshCookieName;

	@Value("${jwt.cookie.secure:false}")
	private boolean secureCookie;

	@Value("${jwt.access-token-expiry-ms:900000}")
	private int accessTokenExpiryMs;

	@Value("${jwt.refresh-token-expiry-ms:604800000}")
	private int refreshTokenExpiryMs;

	@PostMapping("/register")
	public ResponseEntity<User> register(@Valid @RequestBody RegisterRequest request) {
		User created = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
		AuthResponse auth = authService.login(request.getEmail(), request.getPassword());
		setAuthCookies(response, auth.getAccessToken(), auth.getRefreshToken());
		return ResponseEntity.ok(auth);
	}

	@PostMapping("/logout")
	public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response) {

		String token = extractTokenFromRequest(request);
		if (token != null) {
			authService.logout(token);
		}
		clearAuthCookies(response);

		log.info("Logout — cookies cleared");
		return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
	}

	@PostMapping("/refresh")
	public ResponseEntity<Map<String, String>> refresh(HttpServletRequest request, HttpServletResponse response,
			@RequestBody Map<String, String> body) {

		String refreshToken = extractCookie(request, refreshCookieName);
		if (refreshToken == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No refresh token provided"));
		}

		String newAccessToken = authService.refreshToken(refreshToken);
		Cookie accessCookie = buildCookie(accessCookieName, newAccessToken, accessTokenExpiryMs / 1000);
		response.addCookie(accessCookie);

		return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
	}

	@GetMapping("/profile/{userId}")
	public ResponseEntity<User> getProfileById(@PathVariable Integer userId) {
		User user = authService.getUserById(userId);
		return ResponseEntity.ok(user);
	}

	@PutMapping("/profile")
	public ResponseEntity<User> updateProfile(@AuthenticationPrincipal Integer userId,
			@Valid @RequestBody UpdateProfileRequest request) {

		User updated = authService.updateProfile(userId, request);
		return ResponseEntity.ok(updated);
	}

	@PutMapping("/password")
	public ResponseEntity<Map<String, String>> changePassword(@AuthenticationPrincipal Integer userId,
			@Valid @RequestBody ChangePasswordRequest request) {
		authService.changePassword(userId, request.getNewPassword());
		return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
	}

	@DeleteMapping("/deactivate")
	public ResponseEntity<Map<String, String>> deactivate(@AuthenticationPrincipal Integer userId,
			HttpServletResponse response) {
		authService.deactivateAccount(userId);
		clearAuthCookies(response);
		return ResponseEntity.ok(Map.of("message", "Account deactivated"));
	}

	@DeleteMapping("/admin/users/{userId}/deactivate")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Map<String, String>> adminDeactivate(@PathVariable Integer userId) {
		authService.deactivateAccount(userId);
		return ResponseEntity.ok(Map.of("message", "User " + userId + " deactivated"));
	}

	@GetMapping("/search")
	public ResponseEntity<List<User>> searchUsers(@RequestParam("q") String term) {
		List<User> results = authService.searchUsers(term);
		return ResponseEntity.ok(results);
	}

	@GetMapping("/{id}/email")
	public ResponseEntity<Map<String, String>> getEmail(@PathVariable int id) {
		User user = authService.getUserById(id);
		return ResponseEntity.ok(Map.of("email", user.getEmail()));
	}

	private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
		response.addCookie(buildCookie(accessCookieName, accessToken, accessTokenExpiryMs / 1000));
		response.addCookie(buildCookie(refreshCookieName, refreshToken, refreshTokenExpiryMs / 1000));
	}

	private void clearAuthCookies(HttpServletResponse response) {
		response.addCookie(buildCookie(accessCookieName, "", 0));
		response.addCookie(buildCookie(refreshCookieName, "", 0));
	}

	private Cookie buildCookie(String name, String value, int maxAgeSeconds) {
		Cookie cookie = new Cookie(name, value);
		cookie.setHttpOnly(true);
		cookie.setPath("/");
		cookie.setMaxAge(maxAgeSeconds);
		cookie.setSecure(secureCookie);
		return cookie;
	}

	private String extractTokenFromRequest(HttpServletRequest request) {
		String fromCookie = extractCookie(request, accessCookieName);
		if (fromCookie != null)
			return fromCookie;
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			return header.substring(7);
		}
		return null;
	}

	private String extractCookie(HttpServletRequest request, String cookieName) {
		if (request.getCookies() == null)
			return null;
		return Arrays.stream(request.getCookies()).filter(c -> cookieName.equals(c.getName())).map(Cookie::getValue)
				.findFirst().orElse(null);
	}
}
