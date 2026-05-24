package com.connectsphere.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateNotificationRequest {
	@NotNull
	private int recipientId;

	private int actorId;

	@NotBlank
	private String type;

	@NotBlank
	private String message;

	private int targetId;

	private String targetType;

	private String deepLinkUrl;

	private String recipientEmail;
}