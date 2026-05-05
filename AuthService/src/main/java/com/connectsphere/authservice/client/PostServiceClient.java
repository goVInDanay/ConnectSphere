package com.connectsphere.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "post-service")
public interface PostServiceClient {
	@PutMapping("/api/admin/posts/{postId}/approve")
	void approvePost(@PathVariable("postId") int postId);

	@PutMapping("/api/admin/posts/{postId}/reject")
	void rejectPost(@PathVariable("postId") int postId);

	@PutMapping("/api/posts/{postId}/report")
	void reportPost(@PathVariable("postId") int postId);
}
