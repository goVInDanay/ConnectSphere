package com.connectsphere.commentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "post-service")
public interface PostServiceClient {
	@PostMapping("/api/posts/{postId}/comments/inc")
	void incrementCommentCount(@PathVariable int postId);

	@PostMapping("/api/posts/{postId}/comments/dec")
	void decrementCommentCount(@PathVariable int postId);

	@GetMapping("/api/posts/{postId}/author")
	Integer getPostOwnerId(@PathVariable("postId") int postId);
}
