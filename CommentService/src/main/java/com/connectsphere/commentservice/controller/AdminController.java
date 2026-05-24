package com.connectsphere.commentservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.connectsphere.commentservice.entity.Comment;
import com.connectsphere.commentservice.entity.Report;
import com.connectsphere.commentservice.service.CommentService;
import com.connectsphere.commentservice.service.ReportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
public class AdminController {

	private final CommentService commentService;
	private final ReportService reportService;

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<Comment>> getAllComments() {
		return ResponseEntity.ok(commentService.getAllComments());
	}

	@DeleteMapping("/{commentId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> deleteComment(@PathVariable int commentId, @AuthenticationPrincipal Integer adminId) {

		Comment comment = commentService.getCommentById(commentId);

		commentService.adminDeleteComment(commentId);

		log.warn("ADMIN {} deleted comment {} of user {} on post {}", adminId, commentId, comment.getAuthorId(),
				comment.getPostId());

		return ResponseEntity.ok("Comment deleted successfully");
	}

	@GetMapping("/flagged")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<Comment>> getFlaggedComments() {
		return ResponseEntity.ok(commentService.getFlaggedComments());
	}

	@PutMapping("/{commentId}/approve")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> approveComment(@PathVariable int commentId) {
		commentService.approveComment(commentId);
		return ResponseEntity.ok("Comment approved");
	}

	@PutMapping("/{commentId}/reject")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> rejectComment(@PathVariable int commentId) {
		commentService.rejectComment(commentId);
		return ResponseEntity.ok("Comment rejected and removed");
	}

	@GetMapping("/user/{userId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<Comment>> getCommentsByUser(@PathVariable int userId) {
		return ResponseEntity.ok(commentService.getCommentsByUser(userId));
	}

	@GetMapping("/post/{postId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<Comment>> getCommentsByPost(@PathVariable int postId) {
		return ResponseEntity.ok(commentService.getCommentsByPost(postId));
	}

	@GetMapping("/{commentId}/replies")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<Comment>> getReplies(@PathVariable int commentId) {
		return ResponseEntity.ok(commentService.getReplies(commentId));
	}

	@GetMapping("/reports")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<Report>> getAllReports() {
		return ResponseEntity.ok(reportService.getAllReports());
	}

	@GetMapping("/status/{status}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<Report>> getByStatus(@PathVariable String status) {
		return ResponseEntity.ok(reportService.getReportsByStatus(status));
	}

	@GetMapping("/reports/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Report> getReport(@PathVariable Integer id) {
		return ResponseEntity.ok(reportService.getReportById(id));
	}

	@PutMapping("/reports/{id}/approve")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<String> approveReport(@PathVariable Integer id) {
		reportService.approveReport(id);
		return ResponseEntity.ok("Report approved (content is safe)");
	}

	@PutMapping("/reports/{id}/reject")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<String> rejectReport(@PathVariable Integer id) {
		reportService.rejectReport(id);
		return ResponseEntity.ok("Content removed");
	}
}
