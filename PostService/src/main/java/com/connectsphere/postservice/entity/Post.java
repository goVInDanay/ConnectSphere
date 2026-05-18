package com.connectsphere.postservice.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "posts", indexes = { @Index(name = "idx_post_author", columnList = "authorId"),
		@Index(name = "idx_post_visibility", columnList = "visibility"),
		@Index(name = "idx_post_created", columnList = "createdAt"),
		@Index(name = "idx_post_deleted", columnList = "isDeleted") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "postId")
public class Post {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int postId;

	@Column(nullable = false)
	private int authorId;

	@NotBlank
	@Size(max = 5000)
	@Column(nullable = false, length = 5000)
	private String content;

	@Type(JsonType.class)
	@Column(columnDefinition = "json")
	@Builder.Default
	private List<String> mediaUrls = new ArrayList<>();

	@Column(nullable = false, length = 10)
	@Builder.Default
	private String postType = "TEXT";

	@NotNull
	@Column(nullable = false, length = 15)
	@Builder.Default
	private String visibility = "PUBLIC";

	@Column(nullable = false)
	@Builder.Default
	private int likesCount = 0;

	@Column(nullable = false)
	@Builder.Default
	private int commentsCount = 0;

	@Column(nullable = false)
	@Builder.Default
	private int sharesCount = 0;

	@Column(nullable = false)
	@Builder.Default
	private boolean isDeleted = false;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	public void incrementLikes() {
		this.likesCount = Math.max(0, this.likesCount + 1);
	}

	public void decrementLikes() {
		this.likesCount = Math.max(0, this.likesCount - 1);
	}

	public void incrementComments() {
		this.commentsCount = Math.max(0, this.commentsCount + 1);
	}

	public void decrementComments() {
		this.commentsCount = Math.max(0, this.commentsCount - 1);
	}

	public void incrementShares() {
		this.sharesCount = Math.max(0, this.sharesCount + 1);
	}
}
