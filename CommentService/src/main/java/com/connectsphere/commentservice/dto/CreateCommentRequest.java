package com.connectsphere.commentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCommentRequest {

	private int authorId;

	private int postId;

	@NotBlank
	@Size(min = 1, max = 2000)
	private String content;
}