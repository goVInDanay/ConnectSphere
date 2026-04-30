package com.connectsphere.search.entity;

import java.time.LocalDateTime;

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
@Table(name = "hashtags", uniqueConstraints = @UniqueConstraint(name = "uc_hashtag_tag", columnNames = "tag"), indexes = @Index(name = "idx_hashtag_count", columnList = "postCount DESC"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "hashtagId")
public class Hashtag {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int hashtagId;

	@Column(nullable = false, unique = true, length = 100)
	private String tag;

	@Column(nullable = false)
	@Builder.Default
	private int postCount = 0;

	@Column
	private LocalDateTime lastUsedAt;
}
