package com.connectsphere.commentservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.connectsphere.commentservice.entity.Report;
import com.connectsphere.commentservice.entity.ReportType;
import com.connectsphere.commentservice.service.ReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/comments/")
@RequiredArgsConstructor
public class ReportController {

	private final ReportService reportService;

	@PostMapping("/{commentId}/report")
	public ResponseEntity<?> reportComment(@PathVariable int commentId, @RequestBody Report report,
			@AuthenticationPrincipal Integer userId) {
		if (userId == null) {
			throw new RuntimeException("Unauthorized");
		}
		report.setReportType(ReportType.COMMENT);
		report.setReporterId(userId);
		report.setTargetId(commentId);
		reportService.createReport(report);
		return ResponseEntity.ok("Comment reported successfully");
	}
}