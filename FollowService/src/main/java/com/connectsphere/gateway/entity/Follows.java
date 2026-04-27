package com.connectsphere.gateway.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "follows", uniqueConstraints = @UniqueConstraint(name = "uc_follow_pair", columnNames = {"followerId", "followeeId"}), indexes = {
		@Index(name = "idx_follow_follower", columnList = "followerId"),
		@Index(name = "idx_follow_followee", columnList = "followeeId"),
		@Index(name = "idx_follow_status", columnList = "status"),
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "followId")
public class Follows {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int followId;
	
	@Column(nullable = false)
	private int followerId;
	
	@Column(nullable = false)
	private int followeeId;
	
	@Column(nullable = false, length = 10)
	@Builder.Default
	private String status = "ACTIVE";
	
	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;
	
	public 	static final String STATUS_ACTIVE = "ACTIVE";
	public static final String STATUS_PENDING = "PENDING";
	
	public boolean isActive() {
		return STATUS_ACTIVE.equals(status);
	}
	
	public boolean isPending() {
		return STATUS_PENDING.equals(status);
	}
}
