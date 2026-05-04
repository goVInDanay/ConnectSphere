package com.connectsphere.postservice.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mentions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mention {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	private int postId;
	private int mentionedUserId;

	@CreationTimestamp
	private LocalDateTime createdAt;
}