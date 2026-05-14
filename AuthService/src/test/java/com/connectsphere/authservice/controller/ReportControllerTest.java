package com.connectsphere.authservice.controller;

import com.connectsphere.authservice.entities.Report;
import com.connectsphere.authservice.entities.ReportReason;
import com.connectsphere.authservice.entities.ReportStatus;
import com.connectsphere.authservice.entities.ReportType;
import com.connectsphere.authservice.service.AuthService;
import com.connectsphere.authservice.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = { "jwt.secret=thisIsATestSecretKeyThatIsLongEnoughForHS512AlgorithmTest" })
@DisplayName("Auth ReportController Unit Tests")
class ReportControllerTest {

	@Autowired
	MockMvc mockMvc;
	@Autowired
	ObjectMapper objectMapper;

	@MockBean
	ReportService reportService;
	@MockBean
	AuthService authService;

	private Report reportPayload;

	@BeforeEach
	void setUp() {
		reportPayload = Report.builder().targetId(99).reason(ReportReason.SPAM)
				.description("This user is harassing me").build();
	}

	// ── POST /api/users/reports ──────────────────────────────────────

	@Test
	@WithMockUser
	@DisplayName("Create user report – authenticated → 200 OK")
	void createReport_authenticated_success() throws Exception {
		doNothing().when(reportService).createReport(any(Report.class));

		mockMvc.perform(post("/api/users/reports").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(reportPayload))).andExpect(status().isOk())
				.andExpect(content().string("Report submitted successfully"));

		verify(reportService).createReport(any(Report.class));
	}

	@Test
	@DisplayName("Create user report – null principal (unauthenticated) → 5xx")
	void createReport_nullPrincipal_throwsRuntime() throws Exception {
		// No @WithMockUser → @AuthenticationPrincipal resolves to null
		mockMvc.perform(post("/api/users/reports").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(reportPayload))).andExpect(status().is5xxServerError());

		verifyNoInteractions(reportService);
	}

	@Test
	@WithMockUser
	@DisplayName("Create user report – duplicate report → 5xx (service throws)")
	void createReport_duplicate_throws() throws Exception {
		doThrow(new RuntimeException("You already reported this")).when(reportService).createReport(any(Report.class));

		mockMvc.perform(post("/api/users/reports").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(reportPayload))).andExpect(status().is5xxServerError());
	}

	@Test
	@WithMockUser
	@DisplayName("Create user report – missing request body → 400 Bad Request")
	void createReport_missingBody_returns400() throws Exception {
		mockMvc.perform(post("/api/users/reports").with(csrf()).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser
	@DisplayName("Create user report – controller sets ReportType.USER automatically")
	void createReport_setsReportTypeUser() throws Exception {
		doNothing().when(reportService).createReport(any(Report.class));

		mockMvc.perform(post("/api/users/reports").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(reportPayload))).andExpect(status().isOk());

		verify(reportService).createReport(argThat(r -> r.getReportType() == ReportType.USER));
	}

	@Test
	@WithMockUser
	@DisplayName("Create user report – SPAM reason → 200 OK")
	void createReport_spamReason_success() throws Exception {
		reportPayload.setReason(ReportReason.SPAM);
		reportPayload.setDescription("Spam account");
		doNothing().when(reportService).createReport(any(Report.class));

		mockMvc.perform(post("/api/users/reports").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(reportPayload))).andExpect(status().isOk())
				.andExpect(content().string("Report submitted successfully"));
	}
}
