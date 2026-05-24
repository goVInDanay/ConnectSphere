package com.connectsphere.follow.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import com.connectsphere.follow.FollowServiceApplication;
import com.connectsphere.follow.controller.FollowController;
import com.connectsphere.follow.entity.Follows;
import com.connectsphere.follow.service.FollowService;
import com.connectsphere.follow.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(FollowController.class)
@ContextConfiguration(classes = FollowServiceApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
@DisplayName("FollowController Unit Tests")
class FollowControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockBean
	FollowService followService;

	@MockBean
	JwtUtil jwtUtil;

	private Follows sampleFollow;

	@BeforeEach
	void setUp() {
		sampleFollow = Follows.builder().followId(1).followerId(10).followeeId(20).status("ACTIVE")
				.createdAt(LocalDateTime.now()).build();
	}

	@Test
	@WithMockUser(username = "10")
	void follow_error() throws Exception {
		when(followService.follow(anyInt(), eq(20))).thenThrow(new RuntimeException());

		mockMvc.perform(post("/api/follows/20")).andExpect(status().is5xxServerError());
	}


	@Test
	@WithMockUser(username = "10")
	void unfollow_error() throws Exception {
		doThrow(new RuntimeException()).when(followService).unfollow(anyInt(), eq(20));

		mockMvc.perform(delete("/api/follows/20")).andExpect(status().is5xxServerError());
	}

	@Test
	void getFollowers() throws Exception {
		when(followService.getFollowers(20)).thenReturn(List.of(sampleFollow));

		mockMvc.perform(get("/api/follows/20/followers")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void getFollowing() throws Exception {
		when(followService.getFollowing(10)).thenReturn(List.of(sampleFollow));

		mockMvc.perform(get("/api/follows/10/following")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void followerCount() throws Exception {
		when(followService.getFollowerCount(20)).thenReturn(150L);

		mockMvc.perform(get("/api/follows/20/follower-count")).andExpect(status().isOk())
				.andExpect(jsonPath("$.followerCount").value(150));
	}

	@Test
	void followingCount() throws Exception {
		when(followService.getFollowingCount(10)).thenReturn(75L);

		mockMvc.perform(get("/api/follows/10/following-count")).andExpect(status().isOk())
				.andExpect(jsonPath("$.followingCount").value(75));
	}


	@Test
	void isFollowingInternal() throws Exception {
		when(followService.isFollowing(10, 20)).thenReturn(true);

		mockMvc.perform(get("/api/follows/is-following").param("followerId", "10").param("followeeId", "20"))
				.andExpect(status().isOk()).andExpect(content().string("true"));
	}
}