package com.connectsphere.postservice.dto;

import java.util.List;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePostRequest {

	@Size(min = 1, max = 5000)
	private String content;

	private List<String> mediaUrls;
}