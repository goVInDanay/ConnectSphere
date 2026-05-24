package com.connectsphere.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
	@NotBlank
	@Size(min = 3, max = 50)
	public String username;

	@NotBlank
	@Email
	public String email;

	@NotBlank
	@Size(min = 3, max = 50)
	public String password;

	public String fullName;
}
