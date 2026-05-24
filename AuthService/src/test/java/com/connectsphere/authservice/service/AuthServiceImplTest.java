package com.connectsphere.authservice.service;

import com.connectsphere.authservice.dto.*;
import com.connectsphere.authservice.entities.User;
import com.connectsphere.authservice.exceptions.DuplicateResourceException;
import com.connectsphere.authservice.exceptions.InvalidCredentialsException;
import com.connectsphere.authservice.exceptions.ResourceNotFoundException;
import com.connectsphere.authservice.messaging.NotificationProducer;
import com.connectsphere.authservice.repository.UserRepository;
import com.connectsphere.authservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

	@Mock
	UserRepository userRepository;
	@Mock
	PasswordEncoder passwordEncoder;
	@Mock
	JwtUtil jwtUtil;
	@Mock
	NotificationProducer notificationProducer;

	@InjectMocks
	AuthServiceImpl authService;

	private User sampleUser;
	private RegisterRequest registerRequest;

	@BeforeEach
	void setUp() {
		sampleUser = User.builder().userId(1).username("johndoe").email("john@example.com")
				.passwordHash("$2a$10$hashedPassword").fullName("John Doe").role("ROLE_USER").provider("local")
				.isActive(true).build();

		registerRequest = new RegisterRequest();
		registerRequest.setUsername("johndoe");
		registerRequest.setEmail("john@example.com");
		registerRequest.setPassword("password123");
		registerRequest.setFullName("John Doe");
	}

	@Nested
	@DisplayName("register()")
	class RegisterTests {

		@Test
		@DisplayName("New email + new username → saves and returns user")
		void register_newUser_success() {
			when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
			when(userRepository.existsByUsername("johndoe")).thenReturn(false);
			when(passwordEncoder.encode("password123")).thenReturn("encoded");
			when(userRepository.save(any(User.class))).thenReturn(sampleUser);

			User result = authService.register(registerRequest);

			assertThat(result).isNotNull();
			assertThat(result.getEmail()).isEqualTo("john@example.com");
			verify(userRepository).save(any(User.class));
		}

		@Test
		@DisplayName("Duplicate email → throws DuplicateResourceException")
		void register_duplicateEmail_throws() {
			when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

			assertThatThrownBy(() -> authService.register(registerRequest))
					.isInstanceOf(DuplicateResourceException.class).hasMessageContaining("Email already registered");

			verify(userRepository, never()).save(any());
		}

		@Test
		@DisplayName("Duplicate username → throws DuplicateResourceException")
		void register_duplicateUsername_throws() {
			when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
			when(userRepository.existsByUsername("johndoe")).thenReturn(true);

			assertThatThrownBy(() -> authService.register(registerRequest))
					.isInstanceOf(DuplicateResourceException.class).hasMessageContaining("Username already taken");

			verify(userRepository, never()).save(any());
		}
	}

	@Nested
	@DisplayName("login()")
	class LoginTests {

		@Test
		@DisplayName("Valid credentials → returns AuthResponse with tokens")
		void login_validCredentials_returnsTokens() {
			when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
			when(passwordEncoder.matches("password123", "$2a$10$hashedPassword")).thenReturn(true);
			when(jwtUtil.generateAccessToken(sampleUser.getEmail(), sampleUser.getRole(), sampleUser.getUserId()))
					.thenReturn("access-token");
			when(jwtUtil.generateRefreshToken(sampleUser.getEmail())).thenReturn("refresh-token");

			AuthResponse response = authService.login("john@example.com", "password123");

			assertThat(response).isNotNull();
			assertThat(response.getAccessToken()).isEqualTo("access-token");
			assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
		}

		@Test
		@DisplayName("Email not found → throws InvalidCredentialsException")
		void login_emailNotFound_throws() {
			when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> authService.login("unknown@example.com", "password123"))
					.isInstanceOf(InvalidCredentialsException.class);
		}

		@Test
		@DisplayName("Wrong password → throws InvalidCredentialsException")
		void login_wrongPassword_throws() {
			when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
			when(passwordEncoder.matches("wrongpassword", "$2a$10$hashedPassword")).thenReturn(false);

			assertThatThrownBy(() -> authService.login("john@example.com", "wrongpassword"))
					.isInstanceOf(InvalidCredentialsException.class);
		}

		@Test
		@DisplayName("Inactive account → throws InvalidCredentialsException")
		void login_inactiveUser_throws() {
			sampleUser.setActive(false);
			when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));

			assertThatThrownBy(() -> authService.login("john@example.com", "password123"))
					.isInstanceOf(InvalidCredentialsException.class);
		}
	}

	@Nested
	@DisplayName("getUserById()")
	class GetUserByIdTests {

		@Test
		@DisplayName("Existing ID → returns user")
		void getUserById_found() {
			when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));

			User result = authService.getUserById(1);

			assertThat(result.getUserId()).isEqualTo(1);
		}

		@Test
		@DisplayName("Non-existing ID → throws ResourceNotFoundException")
		void getUserById_notFound_throws() {
			when(userRepository.findById(999)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> authService.getUserById(999)).isInstanceOf(ResourceNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("updateProfile()")
	class UpdateProfileTests {

		@Test
		@DisplayName("Valid update → saves and returns updated user")
		void updateProfile_success() {
			UpdateProfileRequest req = new UpdateProfileRequest();
			req.setFullName("Jane Doe");
			req.setBio("Developer");

			when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
			when(userRepository.save(any(User.class))).thenReturn(sampleUser);

			User result = authService.updateProfile(1, req);

			assertThat(result).isNotNull();
			verify(userRepository).save(any(User.class));
		}

		@Test
		@DisplayName("User not found → throws ResourceNotFoundException")
		void updateProfile_userNotFound_throws() {
			when(userRepository.findById(999)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> authService.updateProfile(999, new UpdateProfileRequest()))
					.isInstanceOf(ResourceNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("changePassword()")
	class ChangePasswordTests {

		@Test
		@DisplayName("Valid change → saves new hash")
		void changePassword_success() {
			when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
			when(passwordEncoder.encode("newPassword99")).thenReturn("newHash");
			when(userRepository.save(any(User.class))).thenReturn(sampleUser);

			assertThatCode(() -> authService.changePassword(1, "newPassword99")).doesNotThrowAnyException();

			verify(passwordEncoder).encode("newPassword99");
			verify(userRepository).save(any(User.class));
		}

		@Test
		@DisplayName("User not found → throws ResourceNotFoundException")
		void changePassword_notFound_throws() {
			when(userRepository.findById(999)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> authService.changePassword(999, "pass"))
					.isInstanceOf(ResourceNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("account activation / deactivation")
	class AccountStatusTests {

		@Test
		@DisplayName("deactivateAccount – sets isActive=false and saves")
		void deactivateAccount_setsInactive() {
			when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
			when(userRepository.save(any(User.class))).thenReturn(sampleUser);

			authService.deactivateAccount(1);

			verify(userRepository).save(argThat(u -> !u.isActive()));
		}

		@Test
		@DisplayName("activateAccount – sets isActive=true and saves")
		void activateAccount_setsActive() {
			sampleUser.setActive(false);
			when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
			when(userRepository.save(any(User.class))).thenReturn(sampleUser);

			authService.activateAccount(1);

			verify(userRepository).save(argThat(User::isActive));
		}

		@Test
		@DisplayName("deactivateAccount – user not found → throws")
		void deactivateAccount_notFound_throws() {
			when(userRepository.findById(999)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> authService.deactivateAccount(999)).isInstanceOf(ResourceNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("searchUsers()")
	class SearchTests {

		@Test
		@DisplayName("Matching query → returns list of users")
		void searchUsers_found() {
			when(userRepository.searchByUsername("john")).thenReturn(List.of(sampleUser));

			List<User> results = authService.searchUsers("john");

			assertThat(results).hasSize(1);
			assertThat(results.get(0).getUsername()).isEqualTo("johndoe");
		}

		@Test
		@DisplayName("No matches → returns empty list")
		void searchUsers_noMatch() {
			when(userRepository.searchByUsername("xyz")).thenReturn(List.of());

			List<User> results = authService.searchUsers("xyz");

			assertThat(results).isEmpty();
		}
	}

	@Nested
	@DisplayName("admin operations")
	class AdminTests {

		@Test
		@DisplayName("getAllUsers → returns all users from repository")
		void getAllUsers_returnsList() {
			when(userRepository.findAll()).thenReturn(List.of(sampleUser));

			List<User> results = authService.getAllUsers();

			assertThat(results).hasSize(1);
			verify(userRepository).findAll();
		}

		@Test
		@DisplayName("deleteAccount – existing user → deletes from repository")
		void deleteAccount_success() {
			when(userRepository.existsById(1)).thenReturn(true);

			assertThatCode(() -> authService.deleteAccount(1)).doesNotThrowAnyException();

			verify(userRepository).deleteById(1);
		}

		@Test
		@DisplayName("deleteAccount – user not found → throws ResourceNotFoundException")
		void deleteAccount_notFound_throws() {
			when(userRepository.existsById(999)).thenReturn(false);

			assertThatThrownBy(() -> authService.deleteAccount(999)).isInstanceOf(ResourceNotFoundException.class);
		}

		@Test
		@DisplayName("flagUser – existing user → sets flagged=true")
		void flagUser_success() {
			when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
			when(userRepository.save(any(User.class))).thenReturn(sampleUser);

			authService.flagUser(1);

			verify(userRepository).save(argThat(User::isFlagged));
		}

		@Test
		@DisplayName("unflagUser – existing user → sets flagged=false")
		void unflagUser_success() {
			sampleUser.setFlagged(true);
			when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
			when(userRepository.save(any(User.class))).thenReturn(sampleUser);

			authService.unflagUser(1);

			verify(userRepository).save(argThat(u -> !u.isFlagged()));
		}
	}
}
