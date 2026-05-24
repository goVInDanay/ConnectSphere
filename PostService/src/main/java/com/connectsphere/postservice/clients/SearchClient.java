package com.connectsphere.postservice.clients;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.connectsphere.postservice.dto.HashtagDTO;

@FeignClient(name = "search-service")
public interface SearchClient {

	@GetMapping("/api/hashtags/trending/post-ids")
	List<Integer> getTrendingPostIds(@RequestParam(defaultValue = "20") int limit);
}