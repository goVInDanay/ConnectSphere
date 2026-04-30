package com.connectsphere.search.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.connectsphere.search.entity.Hashtag;
import com.connectsphere.search.service.SearchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SearchController {

	private final SearchService searchService;

	@GetMapping("/api/search/posts")
	public ResponseEntity<List<Object>> searchPosts(@RequestParam("q") String term) {
		return ResponseEntity.ok(searchService.searchPosts(term));
	}

	@GetMapping("/api/search/users")
	public ResponseEntity<List<Object>> searchUsers(@RequestParam("q") String term) {
		return ResponseEntity.ok(searchService.searchUsers(term));
	}

	@GetMapping("/api/search/all")
	public ResponseEntity<Map<String, Object>> searchAll(@RequestParam("q") String term) {
		return ResponseEntity.ok(Map.of("posts", searchService.searchPosts(term), "users",
				searchService.searchUsers(term), "hashtags", searchService.searchHashtags(term)));
	}

	@PostMapping("/api/hashtags/index")
	public ResponseEntity<Map<String, String>> indexPost(@RequestBody Map<String, Object> body) {
		int postId = (Integer) body.get("postId");
		String content = (String) body.get("content");
		searchService.indexPost(postId, content);
		return ResponseEntity.ok(Map.of("message", "Post indexed"));
	}

	@DeleteMapping("/api/hashtags/index/{postId}")
	public ResponseEntity<Map<String, String>> removePostIndex(@PathVariable int postId) {
		searchService.removePostIndex(postId);
		return ResponseEntity.ok(Map.of("message", "Post index removed"));
	}

	@GetMapping("/api/hashtags/trending")
	public ResponseEntity<List<Hashtag>> getTrendingHashtags(@RequestParam(defaultValue = "20") int limit) {
		return ResponseEntity.ok(searchService.getTrendingHashtags(limit));
	}

	@GetMapping("/api/hashtags/{tag}/posts")
	public ResponseEntity<Map<String, Object>> getPostsByHashtag(@PathVariable String tag) {
		List<Integer> postIds = searchService.getPostsByHashtag(tag);
		return ResponseEntity.ok(Map.of("tag", tag, "postIds", postIds, "count", postIds.size()));
	}

	@GetMapping("/api/hashtags/{tag}/count")
	public ResponseEntity<Map<String, Object>> getHashtagCount(@PathVariable String tag) {
		return ResponseEntity.ok(Map.of("tag", tag, "postCount", searchService.getHashtagCount(tag)));
	}

	@GetMapping("/api/hashtags/post/{postId}")
	public ResponseEntity<List<Hashtag>> getHashtagsForPost(@PathVariable int postId) {
		return ResponseEntity.ok(searchService.getHashtagsForPost(postId));
	}

	@GetMapping("/api/hashtags/search")
	public ResponseEntity<List<Hashtag>> searchHashtags(@RequestParam("q") String term) {
		return ResponseEntity.ok(searchService.searchHashtags(term));
	}
}
