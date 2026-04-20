package com.connectsphere.authservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
	@Size(min = 3, max = 50)
	public String username;

	@Size(max = 100)
	public String fullName;

	@Size(max = 500)
	public String bio;

	public String profilePicUrl;
}