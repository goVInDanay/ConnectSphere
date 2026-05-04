package com.connectsphere.postservice.clients;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.connectsphere.postservice.dto.UserDTO;

@FeignClient(name = "auth-service")
public interface UserClient {

	@GetMapping("/api/auth/username")
	List<UserDTO> getUsersByUsernames(@RequestParam List<String> usernames);
}