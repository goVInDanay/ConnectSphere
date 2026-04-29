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
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "media", indexes = { @Index(name = "idx_media_uploader", columnList = "uploaderId"),
		@Index(name = "idx_media_post", columnList = "linkedPostId"),
		@Index(name = "idx_media_type", columnList = "mediaType"),
		@Index(name = "idx_media_deleted", columnList = "deleteStatus") })
@Getter
@Setter
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "mediaId")
public class Media {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int mediaId;

	@Column(nullable = false)
	private int uploaderId;

	@Column(nullable = false, length = 1000)
	private String url;

	@Column(nullable = false, length = 10)
	private String mediaType;

	@Column
	private long sizeKb;

	@Column(length = 10)
	private String mimeType;

	@Column
	@Builder.Default
	private int linkedPostId = 0;

	@Column(length = 500)
	private String filePath;

	@Column(length = 225)
	private String originalFileName;


	@Column(nullable = false)
	@Builder.Default
	private boolean deleteStatus = false;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime uploadedAt;

	public static final String TYPE_IMAGE = "IMAGE";
	public static final String TYPE_VIDEO = "VIDEO";

	public static final long MAX_IMAGE_SIZE_KB = 10_240L;
	public static final long MAX_VIDEO_SIZE_KB = 102_400L;

	public boolean isImage() {
		return TYPE_IMAGE.equals(mediaType);
	}

	public boolean isVideo() {
		return TYPE_VIDEO.equals(mediaType);
	}

}
