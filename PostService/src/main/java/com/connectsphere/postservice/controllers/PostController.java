package com.connectsphere.postservice.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.connectsphere.postservice.dto.ChangeVisibilityRequest;
import com.connectsphere.postservice.dto.CreatePostRequest;
import com.connectsphere.postservice.dto.FeedRequest;
import com.connectsphere.postservice.dto.PostResponse;
import com.connectsphere.postservice.dto.UpdatePostRequest;
import com.connectsphere.postservice.entity.Post;
import com.connectsphere.postservice.service.PostService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;

	@PostMapping
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Post> createPost(@AuthenticationPrincipal Integer userId,
			@Valid @RequestBody CreatePostRequest request) {

		request.setAuthorId(userId);
		Post created = postService.createPost(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@GetMapping("/{postId}")
	public ResponseEntity<PostResponse> getPostById(@PathVariable int postId) {
		Post post = postService.getPostById(postId).orElseThrow(() -> new RuntimeException("Post not found"));

		return ResponseEntity.ok(new PostResponse(post.getPostId(), post.getAuthorId()));
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<List<Post>> getPostsByUser(@PathVariable int userId) {
		List<Post> posts = postService.getPostsByUser(userId);
		return ResponseEntity.ok(posts);
	}

	@PostMapping("/feed")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<List<Post>> getFeed(@AuthenticationPrincipal Integer userId,
			@RequestBody FeedRequest feedRequest) {

		List<Post> feed = postService.getFeedForUser(userId, feedRequest.getFolloweeIds());
		return ResponseEntity.ok(feed);
	}

	@GetMapping("/search")
	public ResponseEntity<List<Post>> searchPosts(@RequestParam("q") String term) {
		log.info("Search api hit");
		List<Post> results = postService.searchPosts(term);
		return ResponseEntity.ok(results);
	}

	@GetMapping("/user/{userId}/count")
	public ResponseEntity<Map<String, Integer>> getPostCount(@PathVariable int userId) {
		int count = postService.getPostCount(userId);
		return ResponseEntity.ok(Map.of("postCount", count));
	}

	@PutMapping("/{postId}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Post> updatePost(@PathVariable int postId, @Valid @RequestBody UpdatePostRequest request) {

		Post updated = postService.updatePost(postId, request);
		return ResponseEntity.ok(updated);
	}

	@PutMapping("/{postId}/visibility")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Map<String, String>> changeVisibility(@PathVariable int postId,
			@Valid @RequestBody ChangeVisibilityRequest request) {

		postService.changeVisibility(postId, request.getVisibility());
		return ResponseEntity.ok(Map.of("postId", String.valueOf(postId), "visibility", request.getVisibility(),
				"message", "Visibility updated successfully"));
	}

	@DeleteMapping("/{postId}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Map<String, String>> deletePost(@PathVariable int postId) {
		postService.deletePost(postId);
		return ResponseEntity.ok(Map.of("message", "Post deleted successfully"));
	}

	@PostMapping("/{postId}/likes/inc")
	public ResponseEntity<Void> incrementLikes(@PathVariable int postId) {
		postService.incrementLikes(postId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{postId}/likes/dec")
	public ResponseEntity<Void> decrementLikes(@PathVariable int postId) {
		postService.decrementLikes(postId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{postId}/comments/inc")
	public ResponseEntity<Void> incrementComments(@PathVariable int postId) {
		postService.incrementComments(postId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{postId}/comments/dec")
	public ResponseEntity<Void> decrementComments(@PathVariable int postId) {
		postService.decrementComments(postId);
		return ResponseEntity.noContent().build();
	}
}