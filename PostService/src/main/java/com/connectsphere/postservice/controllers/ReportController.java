package com.connectsphere.postservice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.connectsphere.postservice.entity.Report;
import com.connectsphere.postservice.entity.ReportType;
import com.connectsphere.postservice.service.ReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts/reports")
@RequiredArgsConstructor
public class ReportController {

	private final ReportService reportService;

	@PostMapping
	public ResponseEntity<?> createReport(@RequestBody Report report, @AuthenticationPrincipal Integer userId) {
		if (userId == null) {
			throw new RuntimeException("Unauthorized");
		}
		report.setReportType(ReportType.POST);
		report.setReporterId(userId);
		reportService.createReport(report);
		return ResponseEntity.ok("Report submitted successfully");
	}
}