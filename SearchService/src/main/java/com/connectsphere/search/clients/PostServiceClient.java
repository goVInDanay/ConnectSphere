package com.connectsphere.search.clients;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "post-service")
public interface PostServiceClient {
	@GetMapping("/api/posts/search")
	List<Object> searchPosts(@RequestParam("q") String term);
}
