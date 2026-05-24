package com.connectsphere.postservice.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.connectsphere.postservice.entity.Post;
import com.connectsphere.postservice.entity.Report;
import com.connectsphere.postservice.service.PostService;
import com.connectsphere.postservice.service.ReportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminController {
	private final PostService postService;
	private final ReportService reportService;

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<Post>> getAllPosts() {
		return ResponseEntity.ok(postService.getAllPosts());
	}

	@DeleteMapping("/{postId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> deletePost(@PathVariable int postId, @AuthenticationPrincipal Integer adminId) {
		Post post = postService.getPostById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
		postService.adminDeletePost(postId);
		log.warn("ADMIN {} deleted post {} of user {}", adminId, postId, post.getAuthorId());
		return ResponseEntity.ok("Post deleted successfully");
	}

	@GetMapping("/flagged")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<Post>> getFlaggedPosts() {
		return ResponseEntity.ok(postService.getFlaggedPosts());
	}

	@PutMapping("/{postId}/approve")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> approvePost(@PathVariable int postId) {
		postService.approvePost(postId);
		return ResponseEntity.ok("Post approved");
	}

	@PutMapping("/{postId}/reject")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> rejectPost(@PathVariable int postId) {
		postService.rejectPost(postId);
		return ResponseEntity.ok("Post rejected");
	}

	@GetMapping("/user/{userId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<Post>> getPostsByUser(@PathVariable int userId) {
		return ResponseEntity.ok(postService.getPostsByUser(userId));
	}

	@GetMapping("/reports/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Report> getReport(@PathVariable Integer id) {
		return ResponseEntity.ok(reportService.getReportById(id));
	}

	@PutMapping("/reports/{id}/resolve")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<String> resolveReport(@PathVariable Integer id) {
		reportService.approveReport(id);
		return ResponseEntity.ok("Report approved (content is safe)");
	}

	@PutMapping("/reports/{id}/invalidate")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<String> invalidateReport(@PathVariable Integer id) {
		reportService.rejectReport(id);
		return ResponseEntity.ok("Content removed");
	}
}
