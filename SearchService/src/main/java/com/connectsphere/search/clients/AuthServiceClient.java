package com.connectsphere.search.clients;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {
	@GetMapping("/api/auth/search")
	List<Object> searchUsers(@RequestParam("q") String term);
}
