package com.connectsphere.authservice.entities;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "users", uniqueConstraints = { @UniqueConstraint(columnNames = "email"),
		@UniqueConstraint(columnNames = "username") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "passwordHash")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int userId;

	@NotBlank
	@Size(min = 3, max = 50)
	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@NotBlank
	@Email
	@Column(nullable = false, unique = true, length = 100)
	private String email;

	@Column(length = 255)
	private String passwordHash;

	@Size(max = 100)
	@Column(length = 100)
	private String fullName;

	@Size(max = 500)
	@Column(length = 500)
	private String bio;

	@Column(length = 500)
	private String profilePicUrl;

	@Column(nullable = false, length = 20)
	@Builder.Default
	private String role = "ROLE_USER";

	@Column(length = 20)
	@Builder.Default
	private String provider = "local";

	@Column(length = 255)
	private String providerId;

	@Column(nullable = false)
	@Builder.Default
	private boolean isActive = true;

	@Column(nullable = false)
	@Builder.Default
	private boolean isSuspended = false;

	@Column(nullable = false)
	@Builder.Default
	private boolean isDeleted = false;

	@Column(nullable = false)
	@Builder.Default
	private int reportCount = 0;

	@Column(nullable = false)
	@Builder.Default
	private boolean isFlagged = false;

	@Column(nullable = false)
	@Builder.Default
	private boolean deactivatedByAdmin = false;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public User(String username, String email, String passwordHash) {
		this.username = username;
		this.email = email;
		this.passwordHash = passwordHash;
	}

}
