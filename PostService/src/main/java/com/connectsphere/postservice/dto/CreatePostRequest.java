package com.connectsphere.postservice.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePostRequest {
	private int authorId;

	@NotBlank
	@Size(min = 1, max = 5000)
	private String content;

	private List<String> mediaUrls = new ArrayList<>();

	private String postType = "TEXT";

	@Pattern(regexp = "PUBLIC|FOLLOWERS|PRIVATE", message = "visibility must be PUBLIC, FOLLOWERS, or PRIVATE")
	private String visibility = "PUBLIC";
}