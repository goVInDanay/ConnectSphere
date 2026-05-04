package com.connectsphere.authservice.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connectsphere.authservice.dto.AuthResponse;
import com.connectsphere.authservice.dto.CreateNotificationRequest;
import com.connectsphere.authservice.dto.RegisterRequest;
import com.connectsphere.authservice.dto.UpdateProfileRequest;
import com.connectsphere.authservice.dto.UserSummary;
import com.connectsphere.authservice.entities.User;
import com.connectsphere.authservice.exceptions.DuplicateResourceException;
import com.connectsphere.authservice.exceptions.InvalidCredentialsException;
import com.connectsphere.authservice.exceptions.ResourceNotFoundException;
import com.connectsphere.authservice.messaging.NotificationProducer;
import com.connectsphere.authservice.repository.UserRepository;
import com.connectsphere.authservice.util.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	private final NotificationProducer notificationProducer;

	@Override
	public User register(RegisterRequest request) {
		log.info("Registering new User: {}", request.getEmail());

		if (userRepository.existsByEmail(request.getEmail())) {
			throw new DuplicateResourceException("Email already registered: " + request.getEmail());
		}

		if (userRepository.existsByEmail(request.getUsername())) {
			throw new DuplicateResourceException("Username already taken: " + request.getUsername());
		}

		User user = User.builder().username(request.getUsername()).email(request.getEmail())
				.passwordHash(passwordEncoder.encode(request.getPassword())).fullName(request.getFullName())
				.role("ROLE_USER").provider("local").isActive(true).build();
		User saved = userRepository.save(user);
		log.info("User registered successfully: userId={}", saved.getUserId());
		try {
			CreateNotificationRequest notification = CreateNotificationRequest.builder().recipientId(saved.getUserId())
					.actorId(saved.getUserId()).type("ACCOUNT_ACTION").message("Welcome to ConnectSphere 🎉")
					.targetId(saved.getUserId()).targetType("USER").deepLinkUrl("/profile/" + saved.getUserId())
					.build();

			notificationProducer.sendNotification(notification);

		} catch (Exception ex) {
			log.error("Failed to send welcome notification: {}", ex.getMessage());
		}
		return saved;
	}

	@Override
	@Transactional(readOnly = true)
	public AuthResponse login(String email, String password) {
		log.info("Login attempt for: {}", email);

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

		if (!user.isActive()) {
			throw new InvalidCredentialsException("Account is deactivated");
		}
		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			throw new InvalidCredentialsException("Invalid email or password");
		}

		String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole(), user.getUserId());
		String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

		UserSummary summary = mapToSummary(user);
		log.info("Login successful for userId={}", user.getUserId());
		return new AuthResponse(accessToken, refreshToken, summary);
	}

	@Override
	public void logout(String token) {
		jwtUtil.denylistToken(token);
		log.info("Token revoked (logout)");
	}

	@Override
	@Transactional(readOnly = true)
	public boolean validateToken(String token) {
		return jwtUtil.validateToken(token);
	}

	@Override
	@Transactional(readOnly = true)
	public String refreshToken(String refreshToken) {
		if (!jwtUtil.validateToken(refreshToken)) {
			throw new InvalidCredentialsException("Invalid or expired refresh token");
		}
		String email = jwtUtil.extractEmail(refreshToken);
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found for token"));

		return jwtUtil.generateAccessToken(user.getEmail(), user.getRole(), user.getUserId());
	}

	@Override
	@Transactional(readOnly = true)
	public User getUserByEmail(String email) {
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
	}

	@Override
	@Transactional(readOnly = true)
	public User getUserById(int userId) {
		return userRepository.findByUserId(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<User> getUsersByUsernames(List<String> usernames) {
		if (usernames == null || usernames.isEmpty()) {
			return List.of();
		}
		List<String> uniqueUsernames = usernames.stream().map(String::toLowerCase).distinct().toList();
		return userRepository.findByUsernameIn(uniqueUsernames);
	}

	@Override
	public User updateProfile(int userId, UpdateProfileRequest request) {
		User user = getUserById(userId);

		if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
			if (userRepository.existsByUsername(request.getUsername())) {
				throw new DuplicateResourceException("Username already taken: " + request.getUsername());
			}
			user.setUsername(request.getUsername());
		}
		if (request.getFullName() != null)
			user.setFullName(request.getFullName());
		if (request.getBio() != null)
			user.setBio(request.getBio());
		if (request.getProfilePicUrl() != null)
			user.setProfilePicUrl(request.getProfilePicUrl());

		User updated = userRepository.save(user);
		log.info("Profile updated for userId={}", userId);
		return updated;
	}

	@Override
	public void changePassword(int userId, String newPassword) {
		User user = getUserById(userId);
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		userRepository.save(user);
		log.info("Password changed for userId={}", userId);
	}

	@Override
	public void deactivateAccount(int userId) {
		User user = getUserById(userId);
		user.setActive(false);
		userRepository.save(user);
		log.info("Account deactivated: userId={}", userId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<User> searchUsers(String term) {
		return userRepository.searchUsers(term);
	}

	public User processOAuthLogin(String provider, String providerId, String email, String name) {
		return userRepository.findByProviderAndProviderId(provider, providerId).orElseGet(() -> {
			return userRepository.findByEmail(email).orElseGet(() -> {
				String baseUsername = email.split("@")[0];
				String uniqueUsername = ensureUniqueUsername(baseUsername);

				User newUser = User.builder().username(uniqueUsername).email(email).fullName(name).role("ROLE_USER")
						.provider(provider).providerId(providerId).isActive(true).build();
				return userRepository.save(newUser);
			});
		});
	}

	private UserSummary mapToSummary(User user) {
		UserSummary s = new UserSummary();
		s.setUserId(user.getUserId());
		s.setUsername(user.getUsername());
		s.setEmail(user.getEmail());
		s.setFullName(user.getFullName());
		s.setBio(user.getBio());
		s.setProfilePicUrl(user.getProfilePicUrl());
		s.setRole(user.getRole());
		s.setActive(user.isActive());
		return s;
	}

	private String ensureUniqueUsername(String base) {
		String candidate = base;
		int suffix = 1;
		while (userRepository.existsByUsername(candidate)) {
			candidate = base + suffix++;
		}
		return candidate;
	}
}
