package com.connectsphere.media.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.connectsphere.media.dto.PostDTO;

@FeignClient(name = "POST-SERVICE")
public interface PostClient {

	@GetMapping("/api/posts/{postId}")
	PostDTO getPostById(@PathVariable int postId);
}
