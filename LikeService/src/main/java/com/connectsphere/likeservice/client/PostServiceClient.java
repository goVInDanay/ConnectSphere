package com.connectsphere.likeservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "post-service")
public interface PostServiceClient {
	@PostMapping("/api/posts/{postId}/likes/inc")
	void incrementLikeCount(@PathVariable int postId);

	@PostMapping("/api/posts/{postId}/likes/dec")
	void decrementLikeCount(@PathVariable int postId);

	@GetMapping("/api/posts/{postId}/author")
	int getPostAuthor(@PathVariable("postId") int targetId);
}
