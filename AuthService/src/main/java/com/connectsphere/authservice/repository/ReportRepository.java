package com.connectsphere.authservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.connectsphere.authservice.entities.Report;
import com.connectsphere.authservice.entities.ReportStatus;
import com.connectsphere.authservice.entities.ReportType;

public interface ReportRepository extends JpaRepository<Report, Integer> {

	List<Report> findByStatus(ReportStatus status);

	List<Report> findByReportType(ReportType type);

	List<Report> findByStatusAndReportType(ReportStatus status, ReportType type);

	boolean existsByReporterIdAndTargetIdAndReportType(Integer reporterId, Integer targetId, ReportType reportType);
}