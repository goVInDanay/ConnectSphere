package com.connectsphere.authservice.dto;

import lombok.Data;

@Data
public class AuthResponse {
	public String accessToken;
	public String refreshToken;
	public String tokenType = "Bearer";
	public UserSummary user;

	public AuthResponse(String accessToken, String refreshToken, UserSummary user) {
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.user = user;
	}
}
