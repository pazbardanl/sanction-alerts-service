package com.fincom.sanction.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fincom.sanction.contract.AlertDTO;
import com.fincom.sanction.contract.CreateAlertRequestDTO;
import com.fincom.sanction.contract.EscalateAlertRequestDTO;
import com.fincom.sanction.contract.UpdateAlertDecisionRequestDTO;
import com.fincom.sanction.domain.Alert;
import com.fincom.sanction.domain.AlertStatus;
import com.fincom.sanction.domain.CreateAlertRequest;
import com.fincom.sanction.domain.EscalateAlertRequest;
import com.fincom.sanction.domain.UpdateAlertDecisionRequest;
import com.fincom.sanction.mapper.SanctionAlertsMapper;
import com.fincom.sanction.service.AlertsService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class SanctionsControllerTest {

	@Mock
	private AlertsService alertsService;

	@Mock
	private SanctionAlertsMapper mapper;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		SanctionsController controller = new SanctionsController(mapper, alertsService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).setValidator(validator).build();
	}

	@Nested
	class CreateAlert {

		@Test
		void happyPath_callsServiceWithMappedRequestAndReturns201() throws Exception {
			CreateAlertRequest domain = new CreateAlertRequest("tx-new", "ACME", 0.9f, "tenant-1");
			LocalDateTime t = LocalDateTime.parse("2026-01-20T10:00:00");
			UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
			Alert alert = new Alert(id, "tx-new", "ACME", 0.9f, AlertStatus.OPEN, null, "tenant-1", t, t, null);
			AlertDTO dto = new AlertDTO(
					id, "tx-new", "ACME", 0.9f, AlertStatus.OPEN, null, "tenant-1", t, t, null);
			when(mapper.toDomain(any(CreateAlertRequestDTO.class))).thenReturn(domain);
			when(alertsService.createAlert(domain)).thenReturn(alert);
			when(mapper.toDTO(alert)).thenReturn(dto);

			String body =
					"""
					{
					  "transactionId": "tx-new",
					  "matchedEntityName": "ACME",
					  "matchScore": 0.9,
					  "tenantId": "tenant-1"
					}
					""";

			mockMvc.perform(post("/sanctions/alerts").contentType(MediaType.APPLICATION_JSON).content(body))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.id").value(id.toString()))
					.andExpect(jsonPath("$.status").value("OPEN"))
					.andExpect(jsonPath("$.tenantId").value("tenant-1"));

			verify(alertsService).createAlert(domain);
		}

		@Test
		void blankTenant_returnsBadRequest() throws Exception {
			String body =
					"""
					{
					  "transactionId": "t",
					  "matchedEntityName": "e",
					  "matchScore": 0.5,
					  "tenantId": ""
					}
					""";
			mockMvc.perform(post("/sanctions/alerts").contentType(MediaType.APPLICATION_JSON).content(body))
					.andExpect(status().isBadRequest());
		}
	}

	@Nested
	class GetAlertsByFilter {

		@Test
		@SuppressWarnings("unchecked")
		void withTenantOnly_callsServiceAndReturnsArray() throws Exception {
			LocalDateTime t = LocalDateTime.parse("2026-01-20T10:00:00");
			UUID id = UUID.fromString("33333333-3333-3333-3333-333333333333");
			Alert a = new Alert(id, "tx-g", "X", 0.4f, AlertStatus.OPEN, null, "tenant-g", t, t, null);
			AlertDTO d = new AlertDTO(id, "tx-g", "X", 0.4f, AlertStatus.OPEN, null, "tenant-g", t, t, null);
			when(alertsService.getAlertsByFilter("tenant-g", null, null)).thenReturn(List.of(a));
			when(mapper.toDTO(any(List.class))).thenReturn(List.of(d));

			mockMvc.perform(get("/sanctions/alerts").param("tenantId", "tenant-g").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(1)))
					.andExpect(jsonPath("$[0].id").value(id.toString()))
					.andExpect(jsonPath("$[0].tenantId").value("tenant-g"));

			verify(alertsService).getAlertsByFilter("tenant-g", null, null);
		}

		@Test
		@SuppressWarnings("unchecked")
		void withStatusAndMinScore_forwardsToService() throws Exception {
			when(alertsService.getAlertsByFilter("tenant-h", AlertStatus.OPEN, 0.5f))
					.thenReturn(List.of());
			when(mapper.toDTO(any(List.class))).thenReturn(List.of());

			mockMvc.perform(
							get("/sanctions/alerts")
									.param("tenantId", "tenant-h")
									.param("status", "OPEN")
									.param("minScore", "0.5")
									.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(0)));

			verify(alertsService).getAlertsByFilter("tenant-h", AlertStatus.OPEN, 0.5f);
		}
	}

	@Nested
	class UpdateAlertDecision {

		@Test
		void happyPath_callsServiceWithMappedRequestAndReturnsBody() throws Exception {
			UUID id = UUID.fromString("44444444-4444-4444-4444-444444444444");
			UpdateAlertDecisionRequest domain =
					new UpdateAlertDecisionRequest(
							id, "tenant-d", AlertStatus.CLEARED, "noted");
			LocalDateTime t = LocalDateTime.parse("2026-01-20T10:00:00");
			Alert alert = new Alert(
					id, "tx-d", "E", 0.8f, AlertStatus.CLEARED, null, "tenant-d", t, t, "noted");
			AlertDTO dto =
					new AlertDTO(
							id, "tx-d", "E", 0.8f, AlertStatus.CLEARED, null, "tenant-d", t, t, "noted");
			when(mapper.toDomain(eq(id), any(UpdateAlertDecisionRequestDTO.class))).thenReturn(domain);
			when(alertsService.updateAlertDecision(domain)).thenReturn(alert);
			when(mapper.toDTO(alert)).thenReturn(dto);

			String body =
					"""
					{
					  "tenantId": "tenant-d",
					  "statusDecision": "CLEARED",
					  "decisionNote": "noted"
					}
					""";

			mockMvc.perform(
							patch("/sanctions/alerts/{id}/decision", id)
									.contentType(MediaType.APPLICATION_JSON)
									.content(body))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id").value(id.toString()))
					.andExpect(jsonPath("$.status").value("CLEARED"))
					.andExpect(jsonPath("$.decisionNote").value("noted"));

			verify(alertsService).updateAlertDecision(domain);
		}

		@Test
		void blankTenantId_returnsBadRequest() throws Exception {
			String body =
					"""
					{
					  "tenantId": "",
					  "statusDecision": "CLEARED",
					  "decisionNote": "n"
					}
					""";
			mockMvc.perform(
							patch("/sanctions/alerts/{id}/decision", UUID.randomUUID())
									.contentType(MediaType.APPLICATION_JSON)
									.content(body))
					.andExpect(status().isBadRequest());
		}
	}

	@Nested
	class EscalateAlert {

		@Test
		void happyPath_callsServiceWithMappedRequestAndReturnsBody() throws Exception {
			UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
			EscalateAlertRequest domain = new EscalateAlertRequest(id, "tenant-wmvc", "case-owner-9");
			LocalDateTime t = LocalDateTime.parse("2026-01-20T10:00:00");
			Alert alert =
					new Alert(
							id, "tx-w", "entity", 0.6f, AlertStatus.ESCALATED, "case-owner-9", "tenant-wmvc", t, t, null);
			AlertDTO dto =
					new AlertDTO(
							id,
							"tx-w",
							"entity",
							0.6f,
							AlertStatus.ESCALATED,
							"case-owner-9",
							"tenant-wmvc",
							t,
							t,
							null);
			when(mapper.toDomain(eq(id), any(EscalateAlertRequestDTO.class))).thenReturn(domain);
			when(alertsService.escalateAlert(domain)).thenReturn(alert);
			when(mapper.toDTO(alert)).thenReturn(dto);

			String body =
					"""
					{
					  "tenantId": "tenant-wmvc",
					  "assignedTo": "case-owner-9"
					}
					""";

			mockMvc.perform(
							patch("/sanctions/alerts/{id}/escalate", id)
									.contentType(MediaType.APPLICATION_JSON)
									.content(body))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id").value(id.toString()))
					.andExpect(jsonPath("$.status").value("ESCALATED"))
					.andExpect(jsonPath("$.assignedTo").value("case-owner-9"))
					.andExpect(jsonPath("$.tenantId").value("tenant-wmvc"))
					.andExpect(jsonPath("$.transactionId").value("tx-w"));

			verify(alertsService).escalateAlert(domain);
		}

		@Test
		void blankTenantId_returnsBadRequest() throws Exception {
			UUID id = UUID.randomUUID();
			String body =
					"""
					{
					  "tenantId": "",
					  "assignedTo": "a"
					}
					""";
			mockMvc.perform(
							patch("/sanctions/alerts/{id}/escalate", id)
									.contentType(MediaType.APPLICATION_JSON)
									.content(body))
					.andExpect(status().isBadRequest());
		}
	}
}
