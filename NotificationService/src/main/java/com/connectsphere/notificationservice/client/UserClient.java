package com.connectsphere.notificationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.connectsphere.notificationservice.dto.EmailResponse;

@FeignClient(name = "auth-service")
public interface UserClient {

	@GetMapping("/api/auth/{id}/email")
	EmailResponse getUserEmail(@PathVariable("id") int userId);
}