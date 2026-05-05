package com.connectsphere.commentservice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.connectsphere.commentservice.entity.Report;
import com.connectsphere.commentservice.entity.ReportStatus;
import com.connectsphere.commentservice.repository.ReportRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {

	private final ReportRepository reportRepository;
	private final CommentService commentService;

	public void createReport(Report report) {
		if (reportRepository.existsByReporterIdAndTargetIdAndReportType(report.getReporterId(), report.getTargetId(),
				report.getReportType())) {
			throw new RuntimeException("You already reported this");
		}
		report.setStatus(ReportStatus.PENDING);
		commentService.reportComment(report.getTargetId());
		reportRepository.save(report);
	}

	public Report getReportById(Integer id) {
		return reportRepository.findById(id).orElseThrow(() -> new RuntimeException("Report not found"));
	}

	@Transactional
	public void approveReport(Integer reportId) {
		Report report = getReportById(reportId);
		commentService.approveComment(report.getTargetId());
		report.setStatus(ReportStatus.RESOLVED);
		reportRepository.save(report);
	}

	@Transactional
	public void rejectReport(Integer reportId) {
		Report report = getReportById(reportId);
		commentService.rejectComment(report.getTargetId());
		report.setStatus(ReportStatus.REJECTED);
		reportRepository.save(report);
	}

	public List<Report> getAllReports() {
		return reportRepository.findAll();
	}

	public List<Report> getReportsByStatus(String status) {
		try {
			ReportStatus reportStatus = ReportStatus.valueOf(status.toUpperCase());
			return reportRepository.findByStatus(reportStatus);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Invalid report status: " + status);
		}
	}
}