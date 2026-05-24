package com.connectsphere.postservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNotificationRequest {
	private int recipientId;
	private int actorId;
	private String type;
	private String message;
	private int targetId;
	private String targetType;
	private String deepLinkUrl;
	private String recipientEmail;
}