package com.connectsphere.media.entity;

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
@Table(name = "stories", indexes = {
		@Index(name = "idx_story_author", columnList = "authorId"),
		@Index(name = "idx_story_active", columnList = "activeStatus"),
		@Index(name = "idx_story_expires", columnList = "expiresAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "storyId")
public class Story {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int storyId;

	@Column(nullable = false)
	private int authorId;


	@Column(nullable = false, length = 1000)
	private String mediaUrl;

	@Column(length = 300)
	private String mediaPublicId;
	
	@Column(length = 500)
	private String caption;

	@Column(nullable = false, length = 10)
	@Builder.Default
	private String mediaType = Media.TYPE_IMAGE;

	@Column(nullable = false)
	@Builder.Default
	private int viewCount = 0;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	@Builder.Default
	private boolean activeStatus = true;

	public boolean isExpired() {
		return LocalDateTime.now().isAfter(expiresAt);
	}
}
