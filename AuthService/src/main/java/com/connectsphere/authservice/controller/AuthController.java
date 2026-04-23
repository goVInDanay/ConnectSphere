package com.connectsphere.authservice.controller;

import java.util.List;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RequestHeader;
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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthService authService;

	@PostMapping("/register")
	public ResponseEntity<User> register(@Valid @RequestBody RegisterRequest request) {
		User created = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		AuthResponse response = authService.login(request.getEmail(), request.getPassword());
		return ResponseEntity.ok(response);
	}

	@PostMapping("/logout")
	public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authHeader) {

		String token = authHeader.replace("Bearer ", "");
		authService.logout(token);
		return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
	}

	@PostMapping("/refresh")
	public ResponseEntity<Map<String, String>> refresh(@RequestBody Map<String, String> body) {

		String newToken = authService.refreshToken(body.get("refreshToken"));
		return ResponseEntity.ok(Map.of("accessToken", newToken));
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
	public ResponseEntity<Map<String, String>> deactivate(@AuthenticationPrincipal Integer userId) {
		authService.deactivateAccount(userId);
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
}
