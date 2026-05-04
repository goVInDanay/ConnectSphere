package com.connectsphere.authservice.service;

import java.util.List;

import com.connectsphere.authservice.dto.AuthResponse;
import com.connectsphere.authservice.dto.RegisterRequest;
import com.connectsphere.authservice.dto.UpdateProfileRequest;
import com.connectsphere.authservice.entities.User;

public interface AuthService {
	User register(RegisterRequest request);

	AuthResponse login(String email, String password);

	void logout(String token);

	boolean validateToken(String token);

	String refreshToken(String refreshToken);

	User getUserByEmail(String email);

	User getUserById(int userId);

	User updateProfile(int userId, UpdateProfileRequest request);

	void changePassword(int userId, String newPassword);

	void deactivateAccount(int userId);

	List<User> searchUsers(String term);
	
	List<User> getUsersByUsernames(List<String> usernames);
}
