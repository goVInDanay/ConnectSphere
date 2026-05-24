package com.connectsphere.commentservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.connectsphere.commentservice.entity.Report;
import com.connectsphere.commentservice.entity.ReportStatus;
import com.connectsphere.commentservice.entity.ReportType;

public interface ReportRepository extends JpaRepository<Report, Integer> {

	List<Report> findByStatus(ReportStatus status);

	List<Report> findByReportType(ReportType type);

	List<Report> findByStatusAndReportType(ReportStatus status, ReportType type);

	boolean existsByReporterIdAndTargetIdAndReportType(Integer reporterId, Integer targetId, ReportType reportType);
}