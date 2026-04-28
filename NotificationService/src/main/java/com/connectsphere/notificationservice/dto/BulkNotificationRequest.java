package com.connectsphere.notificationservice.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BulkNotificationRequest {

	@NotNull
	private List<Integer> recipientIds;

	@NotBlank
	private String type;

	@NotBlank
	private String message;
}