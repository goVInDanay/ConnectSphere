package com.connectsphere.commentservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.connectsphere.commentservice.dto.CreateCommentRequest;
import com.connectsphere.commentservice.dto.UpdateCommentRequest;
import com.connectsphere.commentservice.entity.Comment;
import com.connectsphere.commentservice.service.CommentService;
import com.connectsphere.commentservice.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(CommentController.class)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = { "jwt.secret=thisIsATestSecretKeyThatIsLongEnoughForHS512AlgorithmTest" })
@DisplayName("CommentController Unit Tests")
class CommentControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockBean
	private JwtUtil jwtUtil;

	@MockBean
	CommentService commentService;

	private Comment sampleComment;
	private CreateCommentRequest createRequest;

	@BeforeEach
	void setUp() {
		sampleComment = Comment.builder().commentId(1).postId(10).authorId(42).content("Great post!").likesCount(0)
				.build();

		createRequest = new CreateCommentRequest();
		createRequest.setPostId(10);
		createRequest.setContent("Great post!");
	}

	@Test
	void getCommentsByPost_success() throws Exception {
		when(commentService.getCommentsByPost(10)).thenReturn(List.of(sampleComment));

		mockMvc.perform(get("/api/comments/post/10")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void getCommentById_found() throws Exception {
		when(commentService.getCommentById(1)).thenReturn(sampleComment);

		mockMvc.perform(get("/api/comments/1")).andExpect(status().isOk()).andExpect(jsonPath("$.commentId").value(1));
	}

	@Test
	void getReplies_success() throws Exception {
		Comment reply = Comment.builder().commentId(2).parentCommentId(1).postId(10).authorId(5).content("ok").build();

		when(commentService.getReplies(1)).thenReturn(List.of(reply));

		mockMvc.perform(get("/api/comments/1/replies")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void getCommentsByUser_success() throws Exception {
		when(commentService.getCommentsByUser(42)).thenReturn(List.of(sampleComment));

		mockMvc.perform(get("/api/comments/user/42")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	@WithMockUser
	void likeComment_success() throws Exception {
		doNothing().when(commentService).likeComment(1);

		mockMvc.perform(post("/api/comments/1/like").with(csrf())).andExpect(status().isNoContent());
	}

	@Test
	@WithMockUser
	void unlikeComment_success() throws Exception {
		doNothing().when(commentService).unlikeComment(1);

		mockMvc.perform(post("/api/comments/1/unlike").with(csrf())).andExpect(status().isNoContent());
	}

	@Test
	void getCommentCount_success() throws Exception {
		when(commentService.getCommentCount(10)).thenReturn(5);

		mockMvc.perform(get("/api/comments/post/10/count")).andExpect(status().isOk())
				.andExpect(jsonPath("$.commentCount").value(5));
	}

	@Test
	void getCommentAuthor_success() throws Exception {
		when(commentService.getCommentById(1)).thenReturn(sampleComment);

		mockMvc.perform(get("/api/comments/1/author")).andExpect(status().isOk())
				.andExpect(jsonPath("$.authorId").value(42));
	}
}