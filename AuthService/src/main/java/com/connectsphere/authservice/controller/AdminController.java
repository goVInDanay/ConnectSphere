package com.connectsphere.authservice.controller;

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

import com.connectsphere.authservice.entities.Report;
import com.connectsphere.authservice.entities.User;
import com.connectsphere.authservice.service.AuthService;
import com.connectsphere.authservice.service.ReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminController {

	private final ReportService reportService;
	private final AuthService authService;

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

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Report> getReport(@PathVariable Integer id) {
		return ResponseEntity.ok(reportService.getReportById(id));
	}

	@PutMapping("/{id}/approve")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<String> approveReport(@PathVariable Integer id) {
		reportService.approveReport(id);
		return ResponseEntity.ok("Report approved (content is safe)");
	}

	@PutMapping("/{id}/reject")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<String> rejectReport(@PathVariable Integer id) {
		reportService.rejectReport(id);
		return ResponseEntity.ok("Content removed");
	}

	@PutMapping("/{userId}/deactivate")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> deactivateUser(@PathVariable int userId, @AuthenticationPrincipal int adminId) {
		if (userId == adminId) {
			throw new RuntimeException("Admin cannot deactivate himself");
		}
		authService.deactivateAccount(userId);
		return ResponseEntity.ok("User " + userId + " deactivated");
	}

	@GetMapping("/users")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<User>> getAllUsers() {
		return ResponseEntity.ok(authService.getAllUsers());
	}

	@PutMapping("/{userId}/suspend")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> suspendUser(@PathVariable int userId, @AuthenticationPrincipal int adminId) {
		if (userId == adminId) {
			throw new RuntimeException("Admin cannot suspend himself");
		}
		authService.suspendUser(userId);
		return ResponseEntity.ok("User " + userId + " suspended");
	}

	@PutMapping("/{userId}/activate")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> activateUser(@PathVariable int userId, @AuthenticationPrincipal int adminId) {
		if (userId == adminId) {
			throw new RuntimeException("Admin is already activated");
		}
		authService.activateAccount(userId);
		return ResponseEntity.ok("User " + userId + " activated");
	}

	@DeleteMapping("/{userId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> deleteUser(@PathVariable int userId, @AuthenticationPrincipal int adminId) {
		if (userId == adminId) {
			throw new RuntimeException("Admin cannot delete himself");
		}
		authService.deleteAccount(userId);
		return ResponseEntity.ok("User " + userId + " deleted");
	}

	@GetMapping("/users/flagged")
	@PreAuthorize("hasRole('ADMIN')")
	public List<User> getFlaggedUsers() {
		return authService.getFlaggedUsers();
	}

	@PostMapping("/users/{id}/flag")
	@PreAuthorize("hasRole('ADMIN')")
	public void flagUser(@PathVariable int id) {
		authService.flagUser(id);
	}

	@PostMapping("/users/{id}/unflag")
	@PreAuthorize("hasRole('ADMIN')")
	public void unflagUser(@PathVariable int id) {
		authService.unflagUser(id);
	}
}
