package com.connectsphere.authservice.dto;

import lombok.Data;

@Data
public class UserSummary {
	public int userId;
	public String username;
	public String email;
	public String fullName;
	public String bio;
	public String profilePicUrl;
	public String role;
	public boolean isActive;
}