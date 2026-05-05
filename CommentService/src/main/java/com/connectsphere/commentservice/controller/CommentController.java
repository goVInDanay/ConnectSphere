package com.connectsphere.commentservice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.connectsphere.commentservice.dto.CreateCommentRequest;
import com.connectsphere.commentservice.dto.UpdateCommentRequest;
import com.connectsphere.commentservice.entity.Comment;
import com.connectsphere.commentservice.service.CommentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

	private final CommentService commentService;

	@PostMapping
	public ResponseEntity<Comment> addComment(@AuthenticationPrincipal Integer userId,
			@Valid @RequestBody CreateCommentRequest request) {
		log.info("user id" + userId);
		request.setAuthorId(userId);
		Comment created = commentService.addComment(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PostMapping("/{commentId}/replies")
	public ResponseEntity<Comment> addReply(@AuthenticationPrincipal Integer userId, @PathVariable int commentId,
			@Valid @RequestBody CreateCommentRequest request) {

		request.setAuthorId(userId);
		Comment reply = commentService.addReply(commentId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(reply);
	}

	@GetMapping("/post/{postId}")
	public ResponseEntity<List<Comment>> getCommentsByPost(@PathVariable int postId) {
		return ResponseEntity.ok(commentService.getCommentsByPost(postId));
	}

	@GetMapping("/{commentId}")
	public ResponseEntity<Comment> getCommentById(@PathVariable int commentId) {
		return ResponseEntity.ok(commentService.getCommentById(commentId));
	}

	@GetMapping("/{commentId}/replies")
	public ResponseEntity<List<Comment>> getReplies(@PathVariable int commentId) {
		return ResponseEntity.ok(commentService.getReplies(commentId));
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<List<Comment>> getCommentsByUser(@PathVariable int userId) {
		return ResponseEntity.ok(commentService.getCommentsByUser(userId));
	}

	@PutMapping("/{commentId}")
	public ResponseEntity<Comment> updateComment(@AuthenticationPrincipal Integer userId, @PathVariable int commentId,
			@Valid @RequestBody UpdateCommentRequest request) {

		Comment existing = commentService.getCommentById(commentId);
		if (existing.getAuthorId() != userId) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		Comment updated = commentService.updateComment(commentId, request.getContent());
		return ResponseEntity.ok(updated);
	}

	@DeleteMapping("/{commentId}")
	public ResponseEntity<Map<String, String>> deleteComment(@AuthenticationPrincipal Integer userId,
			@RequestHeader(value = "X-User-Role", defaultValue = "ROLE_USER") String userRole,
			@PathVariable int commentId) {

		Comment existing = commentService.getCommentById(commentId);
		boolean isAdmin = "ROLE_ADMIN".equals(userRole);
		boolean isOwner = existing.getAuthorId() == userId;

		if (!isAdmin && !isOwner) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(Map.of("error", "You may only delete your own comments"));
		}

		commentService.deleteComment(commentId);
		return ResponseEntity.ok(Map.of("message", "Comment deleted successfully"));
	}

	@PostMapping("/{commentId}/like")
	public ResponseEntity<Void> likeComment(@PathVariable int commentId) {
		commentService.likeComment(commentId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{commentId}/unlike")
	public ResponseEntity<Void> unlikeComment(@PathVariable int commentId) {
		commentService.unlikeComment(commentId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/post/{postId}/count")
	public ResponseEntity<Map<String, Integer>> getCommentCount(@PathVariable int postId) {
		return ResponseEntity.ok(Map.of("commentCount", commentService.getCommentCount(postId)));
	}

	@GetMapping("/{commentId}/author")
	public ResponseEntity<Map<String, Integer>> getCommentAuthor(@PathVariable int commentId) {
		Comment comment = commentService.getCommentById(commentId);
		return ResponseEntity.ok(Map.of("authorId", comment.getAuthorId()));
	}
}