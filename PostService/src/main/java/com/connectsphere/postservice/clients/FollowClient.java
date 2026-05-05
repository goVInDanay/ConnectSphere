package com.connectsphere.postservice.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "follow-service")
public interface FollowClient {

	@GetMapping("/is-following")
	Boolean isFollowing(@RequestParam int userId, @RequestParam int followeeId);
}