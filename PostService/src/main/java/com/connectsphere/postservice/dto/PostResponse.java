package com.connectsphere.postservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostResponse {
	private int postId;
	private int authorId;
	private String content;
}