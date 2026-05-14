package com.connectsphere.authservice.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.connectsphere.authservice.entities.Report;
import com.connectsphere.authservice.entities.User;
import com.connectsphere.authservice.service.AuthService;
import com.connectsphere.authservice.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AdminController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
		"jwt.secret=connectsphere-super-secret-key-replace-in-production-use-openssl-rand-hex-64" })
@DisplayName("Auth AdminController Unit Tests")
class AdminControllerTest {

	@Autowired
	MockMvc mockMvc;
	@Autowired
	ObjectMapper objectMapper;

	@MockBean
	AuthService authService;
	@MockBean
	ReportService reportService;

	private Report sampleReport;
	private User sampleUser;

	@BeforeEach
	void setUp() {
		sampleReport = new Report();
		sampleUser = User.builder().userId(2).username("alice").email("alice@test.com").build();
	}

	// ── GET /api/admin/users/reports ────────────────────────────────

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("Get all reports – success → 200 OK")
	void getAllReports_success() throws Exception {
		when(reportService.getAllReports()).thenReturn(List.of(sampleReport));

		mockMvc.perform(get("/api/admin/users/reports")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("Get all reports – empty → 200 OK with empty list")
	void getAllReports_empty() throws Exception {
		when(reportService.getAllReports()).thenReturn(List.of());

		mockMvc.perform(get("/api/admin/users/reports")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	// ── GET /api/admin/users/status/{status} ────────────────────────

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("Get reports by status – success → 200 OK")
	void getByStatus_success() throws Exception {
		when(reportService.getReportsByStatus("PENDING")).thenReturn(List.of(sampleReport));

		mockMvc.perform(get("/api/admin/users/status/PENDING")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	// ── GET /api/admin/users/{id} ───────────────────────────────────

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("Get report by ID – found → 200 OK")
	void getReport_found() throws Exception {
		when(reportService.getReportById(1)).thenReturn(sampleReport);

		mockMvc.perform(get("/api/admin/users/1")).andExpect(status().isOk());
	}

	// ── PUT /api/admin/users/{id}/approve ──────────────────────────

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("Approve report – success → 200 OK")
	void approveReport_success() throws Exception {
		doNothing().when(reportService).approveReport(1);

		mockMvc.perform(put("/api/admin/users/1/approve").with(csrf())).andExpect(status().isOk())
				.andExpect(content().string("Report approved (content is safe)"));
	}

	// ── PUT /api/admin/users/{id}/reject ───────────────────────────

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("Reject report – success → 200 OK")
	void rejectReport_success() throws Exception {
		doNothing().when(reportService).rejectReport(1);

		mockMvc.perform(put("/api/admin/users/1/reject").with(csrf())).andExpect(status().isOk())
				.andExpect(content().string("Content removed"));
	}

	// ── GET /api/admin/users/users ─────────────────────────────────

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("Get all users – success → 200 OK")
	void getAllUsers_success() throws Exception {
		when(authService.getAllUsers()).thenReturn(List.of(sampleUser));

		mockMvc.perform(get("/api/admin/users/users")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	// ── PUT /api/admin/users/{userId}/deactivate ────────────────────

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("Deactivate user – success → 200 OK")
	void deactivateUser_success() throws Exception {
		doNothing().when(authService).deactivateAccount(2);

		mockMvc.perform(put("/api/admin/users/2/deactivate").with(csrf())).andExpect(status().isOk());
	}

	// ── PUT /api/admin/users/{userId}/suspend ──────────────────────

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("Suspend user – success → 200 OK")
	void suspendUser_success() throws Exception {
		doNothing().when(authService).suspendUser(2);

		mockMvc.perform(put("/api/admin/users/2/suspend").with(csrf())).andExpect(status().isOk());
	}

	// ── PUT /api/admin/users/{userId}/activate ─────────────────────

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("Activate user – success → 200 OK")
	void activateUser_success() throws Exception {
		doNothing().when(authService).activateAccount(2);

		mockMvc.perform(put("/api/admin/users/2/activate").with(csrf())).andExpect(status().isOk());
	}

	// ── DELETE /api/admin/users/{userId} ───────────────────────────

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("Delete user – success → 200 OK")
	void deleteUser_success() throws Exception {
		doNothing().when(authService).deleteAccount(2);

		mockMvc.perform(delete("/api/admin/users/2").with(csrf())).andExpect(status().isOk());
	}

	// ── GET /api/admin/users/users/flagged ─────────────────────────

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("Get flagged users – success → list")
	void getFlaggedUsers_success() throws Exception {
		when(authService.getFlaggedUsers()).thenReturn(List.of(sampleUser));

		mockMvc.perform(get("/api/admin/users/users/flagged")).andExpect(status().isOk());
	}

	// ── POST /api/admin/users/users/{id}/flag ──────────────────────

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("Flag user – success → 200 OK")
	void flagUser_success() throws Exception {
		doNothing().when(authService).flagUser(2);

		mockMvc.perform(post("/api/admin/users/users/2/flag").with(csrf())).andExpect(status().isOk());
	}

	// ── POST /api/admin/users/users/{id}/unflag ────────────────────

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("Unflag user – success → 200 OK")
	void unflagUser_success() throws Exception {
		doNothing().when(authService).unflagUser(2);

		mockMvc.perform(post("/api/admin/users/users/2/unflag").with(csrf())).andExpect(status().isOk());
	}
}
