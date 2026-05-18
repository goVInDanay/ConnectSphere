package com.connectsphere.likeservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "comment-service")
public interface CommentServiceClient {
	@PostMapping("/api/comments/{commentId}/like")
	void incrementCommentCount(@PathVariable int commentId);

	@PostMapping("/api/comments/{commentId}/unlike")
	void decrementCommentCount(@PathVariable int commentId);
	
	@GetMapping("/api/comments/{commentId}/author")
	int getCommentAuthor(int commentId);
}
