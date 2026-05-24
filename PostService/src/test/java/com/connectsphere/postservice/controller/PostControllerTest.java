package com.connectsphere.postservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.connectsphere.postservice.controllers.PostController;
import com.connectsphere.postservice.dto.ChangeVisibilityRequest;
import com.connectsphere.postservice.dto.CreatePostRequest;
import com.connectsphere.postservice.dto.FeedRequest;
import com.connectsphere.postservice.dto.PostResponse;
import com.connectsphere.postservice.dto.UpdatePostRequest;
import com.connectsphere.postservice.entity.Post;
import com.connectsphere.postservice.service.PostService;
import com.connectsphere.postservice.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(PostController.class)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = { "jwt.secret=thisIsATestSecretKeyThatIsLongEnoughForHS512AlgorithmTest" })
class PostControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockBean
	PostService postService;

	@MockBean
	private JwtUtil jwtUtil;

	private Post samplePost;
	private CreatePostRequest createRequest;

	@BeforeEach
	void setUp() {
		samplePost = Post.builder().postId(1).authorId(42).content("Hello ConnectSphere!").visibility("PUBLIC")
				.createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

		createRequest = new CreatePostRequest();
		createRequest.setContent("Hello ConnectSphere!");
		createRequest.setVisibility("PUBLIC");
	}

	@Test
	@WithMockUser
	void createPost_blankContent_returns400() throws Exception {
		createRequest.setContent("");

		mockMvc.perform(post("/api/posts").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createRequest))).andExpect(status().isBadRequest());

		verifyNoInteractions(postService);
	}

	@Test
	@WithMockUser
	void createPost_invalidVisibility_returns400() throws Exception {
		createRequest.setVisibility("INVALID");

		mockMvc.perform(post("/api/posts").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createRequest))).andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser
	void getPostsByUser_success() throws Exception {
		when(postService.getVisiblePosts(anyInt(), any())).thenReturn(List.of(samplePost));

		mockMvc.perform(get("/api/posts/user/42")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	@WithMockUser
	void searchPosts_success() throws Exception {
		when(postService.searchPosts("hello")).thenReturn(List.of(samplePost));
		when(postService.canUserViewPost(any(), eq(samplePost))).thenReturn(true);

		mockMvc.perform(get("/api/posts/search").param("q", "hello")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	@WithMockUser
	void getPostCount_success() throws Exception {
		when(postService.getPostCount(42)).thenReturn(5);

		mockMvc.perform(get("/api/posts/user/42/count")).andExpect(status().isOk())
				.andExpect(jsonPath("$.postCount").value(5));
	}

	@Test
	@WithMockUser
	void incrementLikes_success() throws Exception {
		doNothing().when(postService).incrementLikes(1);

		mockMvc.perform(post("/api/posts/1/likes/inc").with(csrf())).andExpect(status().isNoContent());
	}

	@Test
	@WithMockUser
	void decrementLikes_success() throws Exception {
		doNothing().when(postService).decrementLikes(1);

		mockMvc.perform(post("/api/posts/1/likes/dec").with(csrf())).andExpect(status().isNoContent());
	}

	@Test
	@WithMockUser
	void incrementComments_success() throws Exception {
		doNothing().when(postService).incrementComments(1);

		mockMvc.perform(post("/api/posts/1/comments/inc").with(csrf())).andExpect(status().isNoContent());
	}

	@Test
	@WithMockUser
	void decrementComments_success() throws Exception {
		doNothing().when(postService).decrementComments(1);

		mockMvc.perform(post("/api/posts/1/comments/dec").with(csrf())).andExpect(status().isNoContent());
	}

	@Test
	@WithMockUser
	void getPostAuthor_success() throws Exception {
		when(postService.getPostById(1)).thenReturn(Optional.of(samplePost));
		when(postService.canUserViewPost(any(), eq(samplePost))).thenReturn(true);

		mockMvc.perform(get("/api/posts/1/author")).andExpect(status().isOk()).andExpect(content().string("42"));
	}
}