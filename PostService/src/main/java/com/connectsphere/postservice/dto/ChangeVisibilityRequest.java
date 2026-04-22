package com.connectsphere.postservice.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangeVisibilityRequest {

	@Pattern(regexp = "PUBLIC|FOLLOWERS|PRIVATE", message = "visibility must be PUBLIC, FOLLOWERS, or PRIVATE")
	private String visibility;
}