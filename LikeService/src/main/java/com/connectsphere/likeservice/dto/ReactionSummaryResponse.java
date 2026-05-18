package com.connectsphere.likeservice.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReactionSummaryResponse {
	private int targetId;
	private String targetType;
//	private int totalCount;
	private Map<String, Long> reactions;
}
