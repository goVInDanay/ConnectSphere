package com.connectsphere.likeservice.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.connectsphere.likeservice.entity.Like;
import com.connectsphere.likeservice.service.LikeService;
import com.connectsphere.likeservice.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(LikeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
@DisplayName("LikeController Unit Tests")
class LikeControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockBean
	LikeService likeService;

	@MockBean
	JwtUtil jwtUtil;

	private Like sampleLike;

	@BeforeEach
	void setUp() {
		sampleLike = Like.builder().likeId(1L).userId(10).targetId(20).targetType("POST").reactionType("LIKE")
				.createdAt(LocalDateTime.now()).build();
	}

	@Test
	void getLikesByTarget_success() throws Exception {
		when(likeService.getLikesByTarget(20, "POST")).thenReturn(List.of(sampleLike));

		mockMvc.perform(get("/api/likes/target/POST/20")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void getLikeCount_success() throws Exception {
		when(likeService.getLikeCount(20, "POST")).thenReturn(5L);

		mockMvc.perform(get("/api/likes/target/POST/20/count")).andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(5));
	}

}