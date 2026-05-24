package com.connectsphere.search.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.connectsphere.search.entity.Hashtag;
import com.connectsphere.search.service.SearchService;
import com.connectsphere.search.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(SearchController.class)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {
		"jwt.secret=connectsphere-super-secret-key-replace-in-production-use-openssl-rand-hex-64",
		"jwt.access-token-expiry-ms=900000", "jwt.refresh-token-expiry-ms=604800000",
		"jwt.cookie.access-token-name=cs_access_token", "jwt.cookie.refresh-token-name=cs_refresh_token",
		"jwt.cookie.secure=false" })
@DisplayName("SearchController Unit Tests")
class SearchControllerTest {

	@Autowired
	MockMvc mockMvc;
	@Autowired
	ObjectMapper objectMapper;

	@MockBean
	SearchService searchService;

	@MockBean
	private JwtUtil jwtUtil;

	private Hashtag sampleHashtag;

	@BeforeEach
	void setUp() {
		sampleHashtag = Hashtag.builder().tag("spring").postCount(42).build();
	}

	// GET /api/search/posts

	@Test
	@DisplayName("Search posts – found → 200 OK")
	void searchPosts_found() throws Exception {
		when(searchService.searchPosts("spring")).thenReturn(List.of(Map.of("postId", 1, "content", "Spring tips")));

		mockMvc.perform(get("/api/search/posts").param("q", "spring")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	@DisplayName("Search posts – no results → 200 OK with empty list")
	void searchPosts_noResults() throws Exception {
		when(searchService.searchPosts("unknownterm")).thenReturn(List.of());

		mockMvc.perform(get("/api/search/posts").param("q", "unknownterm")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	// GET /api/search/users

	@Test
	@DisplayName("Search users – found → 200 OK")
	void searchUsers_found() throws Exception {
		when(searchService.searchUsers("john")).thenReturn(List.of(Map.of("userId", 1, "username", "johndoe")));

		mockMvc.perform(get("/api/search/users").param("q", "john")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	@DisplayName("Search users – no results → 200 OK with empty list")
	void searchUsers_noResults() throws Exception {
		when(searchService.searchUsers("xyz")).thenReturn(List.of());

		mockMvc.perform(get("/api/search/users").param("q", "xyz")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	// GET /api/search/all

	@Test
	@DisplayName("Search all – returns posts, users, hashtags → 200 OK")
	void searchAll_success() throws Exception {
		when(searchService.searchPosts("java")).thenReturn(List.of(Map.of("postId", 1)));
		when(searchService.searchUsers("java")).thenReturn(List.of(Map.of("userId", 2)));
		when(searchService.searchHashtags("java")).thenReturn(List.of(sampleHashtag));

		mockMvc.perform(get("/api/search/all").param("q", "java")).andExpect(status().isOk())
				.andExpect(jsonPath("$.posts").isArray()).andExpect(jsonPath("$.users").isArray())
				.andExpect(jsonPath("$.hashtags").isArray());
	}

	@Test
	@DisplayName("Search all – empty results → 200 OK with empty collections")
	void searchAll_empty() throws Exception {
		when(searchService.searchPosts(anyString())).thenReturn(List.of());
		when(searchService.searchUsers(anyString())).thenReturn(List.of());
		when(searchService.searchHashtags(anyString())).thenReturn(List.of());

		mockMvc.perform(get("/api/search/all").param("q", "noresult")).andExpect(status().isOk())
				.andExpect(jsonPath("$.posts.length()").value(0)).andExpect(jsonPath("$.users.length()").value(0))
				.andExpect(jsonPath("$.hashtags.length()").value(0));
	}

	// POST /api/hashtags/index

	@Test
	@DisplayName("Index post – success → 200 OK")
	void indexPost_success() throws Exception {
		doNothing().when(searchService).indexPost(eq(1), anyString());

		Map<String, Object> body = Map.of("postId", 1, "content", "Hello #spring world");

		mockMvc.perform(post("/api/hashtags/index").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body))).andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Post indexed"));

		verify(searchService).indexPost(1, "Hello #spring world");
	}

	// ── DELETE /api/hashtags/index/{postId} ─────────────────────────

	@Test
	@DisplayName("Remove post index – success → 200 OK")
	void removePostIndex_success() throws Exception {
		doNothing().when(searchService).removePostIndex(1);

		mockMvc.perform(delete("/api/hashtags/index/1").with(csrf())).andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Post index removed"));

		verify(searchService).removePostIndex(1);
	}

	// GET /api/hashtags/trending

	@Test
	@DisplayName("Get trending hashtags – default limit → 200 OK")
	void getTrendingHashtags_defaultLimit() throws Exception {
		when(searchService.getTrendingHashtags(20)).thenReturn(List.of(sampleHashtag));

		mockMvc.perform(get("/api/hashtags/trending")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].tag").value("spring"))
				.andExpect(jsonPath("$[0].postCount").value(42));
	}

	@Test
	@DisplayName("Get trending hashtags – custom limit → 200 OK")
	void getTrendingHashtags_customLimit() throws Exception {
		when(searchService.getTrendingHashtags(5)).thenReturn(List.of(sampleHashtag));

		mockMvc.perform(get("/api/hashtags/trending").param("limit", "5")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	@DisplayName("Get trending hashtags – no trending → 200 OK with empty list")
	void getTrendingHashtags_empty() throws Exception {
		when(searchService.getTrendingHashtags(20)).thenReturn(List.of());

		mockMvc.perform(get("/api/hashtags/trending")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	// GET /api/hashtags/{tag}/posts

	@Test
	@DisplayName("Get posts by hashtag – found → 200 OK")
	void getPostsByHashtag_found() throws Exception {
		when(searchService.getPostsByHashtag("spring")).thenReturn(List.of(1, 2, 3));

		mockMvc.perform(get("/api/hashtags/spring/posts")).andExpect(status().isOk())
				.andExpect(jsonPath("$.tag").value("spring")).andExpect(jsonPath("$.count").value(3))
				.andExpect(jsonPath("$.postIds.length()").value(3));
	}

	@Test
	@DisplayName("Get posts by hashtag – no posts → 200 OK with count 0")
	void getPostsByHashtag_empty() throws Exception {
		when(searchService.getPostsByHashtag("rare")).thenReturn(List.of());

		mockMvc.perform(get("/api/hashtags/rare/posts")).andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(0));
	}

	// GET /api/hashtags/{tag}/count

	@Test
	@DisplayName("Get hashtag count – found → 200 OK")
	void getHashtagCount_success() throws Exception {
		when(searchService.getHashtagCount("spring")).thenReturn(42);

		mockMvc.perform(get("/api/hashtags/spring/count")).andExpect(status().isOk())
				.andExpect(jsonPath("$.tag").value("spring")).andExpect(jsonPath("$.postCount").value(42));
	}

	// GET /api/hashtags/post/{postId}

	@Test
	@DisplayName("Get hashtags for post – found → 200 OK")
	void getHashtagsForPost_found() throws Exception {
		when(searchService.getHashtagsForPost(1)).thenReturn(List.of(sampleHashtag));

		mockMvc.perform(get("/api/hashtags/post/1")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].tag").value("spring"));
	}

	@Test
	@DisplayName("Get hashtags for post – no hashtags → 200 OK with empty list")
	void getHashtagsForPost_empty() throws Exception {
		when(searchService.getHashtagsForPost(999)).thenReturn(List.of());

		mockMvc.perform(get("/api/hashtags/post/999")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	// ── GET /api/hashtags/search ───────────────────────────────────

	@Test
	@DisplayName("Search hashtags – found → 200 OK")
	void searchHashtags_found() throws Exception {
		when(searchService.searchHashtags("spr")).thenReturn(List.of(sampleHashtag));

		mockMvc.perform(get("/api/hashtags/search").param("q", "spr")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	// GET /api/hashtags/trending/post-ids

	@Test
	@DisplayName("Get trending post IDs – success → 200 OK")
	void getTrendingPostIds_success() throws Exception {
		when(searchService.getTrendingPostIds(20)).thenReturn(List.of(5, 10, 15));

		mockMvc.perform(get("/api/hashtags/trending/post-ids")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3));
	}

	@Test
	@DisplayName("Get trending post IDs – custom limit → 200 OK")
	void getTrendingPostIds_customLimit() throws Exception {
		when(searchService.getTrendingPostIds(5)).thenReturn(List.of(1, 2, 3, 4, 5));

		mockMvc.perform(get("/api/hashtags/trending/post-ids").param("limit", "5")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(5));
	}
}
