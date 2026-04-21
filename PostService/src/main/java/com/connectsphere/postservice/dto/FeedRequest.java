package com.connectsphere.postservice.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class FeedRequest {
	private List<Integer> followeeIds = new ArrayList<>();
}