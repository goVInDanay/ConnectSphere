package com.connectsphere.authservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.connectsphere.authservice.entities.Report;
import com.connectsphere.authservice.entities.ReportType;
import com.connectsphere.authservice.service.ReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users/reports")
@RequiredArgsConstructor
public class ReportController {

	private final ReportService reportService;

	@PostMapping
	public ResponseEntity<?> createReport(@RequestBody Report report, @AuthenticationPrincipal Integer userId) {
		if (userId == null) {
			throw new RuntimeException("Unauthorized");
		}
		report.setReporterId(userId);
		report.setReportType(ReportType.USER);
		reportService.createReport(report);
		return ResponseEntity.ok("Report submitted successfully");
	}
}