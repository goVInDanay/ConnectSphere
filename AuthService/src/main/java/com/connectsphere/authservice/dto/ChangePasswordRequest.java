package com.connectsphere.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
	@NotBlank
	public String currentPassword;

	@NotBlank
	@Size(min = 8, max = 72)
	public String newPassword;
}