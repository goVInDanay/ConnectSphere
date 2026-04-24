package com.connectsphere.likeservice.entity;

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
@Table(name = "likes", uniqueConstraints = { @UniqueConstraint(name = "like_user_target", columnNames = { "userId",
		"targetId", "targetType" }) }, indexes = {
				@Index(name = "idx_like_target", columnList = "targetId, targetType"),
				@Index(name = "idx_like_user", columnList = "userId"),
				@Index(name = "idx_like_reaction", columnList = "targetId, reactionType") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "likeId")
public class Like {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long likeId;

	@Column(nullable = false)
	private int userId;

	@Column(nullable = false)
	private int targetId;

	@Column(nullable = false, length = 19)
	private String targetType;
	
	@Column(nullable = false)
	@Builder.Default
	private String reactionType = "LIKE";

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public static final String TARGET_POST = "POST";
	public static final String TARGET_COMMENT = "COMMENT";

	public static final String REACTION_LIKE = "LIKE";
	public static final String REACTION_LOVE = "LOVE";
	public static final String REACTION_HAHA = "HAHA";
	public static final String REACTION_WOW = "WOW";
	public static final String REACTION_SAD = "SAD";
	public static final String REACTION_ANGRY = "ANGRY";

	public boolean targetsPost() {
		return TARGET_POST.equals(targetType);
	}

	public boolean targetsComment() {
		return TARGET_COMMENT.equals(targetType);
	}
}
