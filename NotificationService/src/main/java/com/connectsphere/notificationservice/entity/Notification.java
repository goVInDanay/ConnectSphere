package com.connectsphere.notificationservice.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "notifications", indexes = { @Index(name = "idx_notification_recipient", columnList = "recipientId"),
		@Index(name = "idx_notification_recipient_unread", columnList = "recipientId, readStatus"),
		@Index(name = "idx_notification_actor_target", columnList = "actorId, targetId"),
		@Index(name = "idx_notification_type", columnList = "type"),
		@Index(name = "idx_notification_created", columnList = "createdAt")

})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "notificationId")
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int notificationId;

	@Column(nullable = false)
	private int recipientId;

	@Column(nullable = false)
	@Builder.Default
	private int actorId = 0;

	@Column(nullable = false, length = 20)
	private String type;

	@Column(length = 500)
	private String message;

	@Column
	@Builder.Default
	private int targetId = 0;

	@Column(length = 10)
	private String targetType;

	@Column(length = 500)
	private String deepLinkUrl;

	@Column(nullable = false)
	@Builder.Default
	private boolean readStatus = false;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public static final String TYPE_LIKE = "LIKE";
	public static final String TYPE_COMMENT = "COMMENT";
	public static final String TYPE_REPLY = "REPLY";
	public static final String TYPE_FOLLOW = "FOLLOW";
	public static final String TYPE_MENTION = "MENTION";
	public static final String TYPE_ACCOUNT_ACTION = "ACCOUNT_ACTION";
	public static final String TYPE_BROADCAST = "BROADCAST";

	public static final String TARGET_POST = "POST";
	public static final String TARGET_COMMENT = "COMMENT";
	public static final String TARGET_USER = "USER";

	public boolean isHighPriority() {
		return TYPE_FOLLOW.equals(type) || TYPE_ACCOUNT_ACTION.equals(type);
	}
}
