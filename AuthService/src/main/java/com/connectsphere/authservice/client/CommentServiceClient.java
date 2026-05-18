package com.connectsphere.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "comment-service")
public interface CommentServiceClient {
	@PutMapping("/api/admin/comments/{commentId}/approve")
	void approveComment(@PathVariable("commentId") int commentId);

	@PutMapping("/api/admin/comments/{commentId}/reject")
	void rejectComment(@PathVariable("commentId") int commentId);

	@PutMapping("/api/comments/{commentId}/report")
	void reportComment(@PathVariable("commentId") int commentId);
}
