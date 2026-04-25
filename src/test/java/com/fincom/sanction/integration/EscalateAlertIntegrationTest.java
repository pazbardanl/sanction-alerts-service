package com.fincom.sanction.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fincom.sanction.domain.alert.AlertStatus;
import com.fincom.sanction.domain.event.Event;
import com.fincom.sanction.domain.event.EventType;
import com.fincom.sanction.repository.AlertsRepository;
import com.fincom.sanction.service.impl.StdoutEventPublisher;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class EscalateAlertIntegrationTest {

	private static final int PUBLISH_VERIFY_TIMEOUT_MS = 5_000;

	private static final String TENANT = "tenant-escalate-flow";

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private AlertsRepository alertsRepository;

	@MockitoBean
	private StdoutEventPublisher stdoutEventPublisher;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		clearInvocations(stdoutEventPublisher);
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	void escalateAlert_happyPath_returnsEscalatedWithAssignee() throws Exception {
		String createJson =
				"""
				{
				  "transactionId": "txn-esc-1",
				  "matchedEntityName": "ORION",
				  "matchScore": 0.75,
				  "tenantId": "%s"
				}
				"""
						.formatted(TENANT);

		String createResponse =
				mockMvc.perform(post("/sanctions/alerts").contentType(MediaType.APPLICATION_JSON).content(createJson))
						.andExpect(status().isCreated())
						.andReturn()
						.getResponse()
						.getContentAsString();

		UUID alertId = UUID.fromString(JsonPath.read(createResponse, "$.id"));

		String patchJson =
				"""
				{
				  "tenantId": "%s",
				  "assignedTo": "analyst-esc-42"
				}
				"""
						.formatted(TENANT);

		mockMvc.perform(
						patch("/sanctions/alerts/{id}/escalate", alertId)
								.contentType(MediaType.APPLICATION_JSON)
								.content(patchJson))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(alertId.toString()))
				.andExpect(jsonPath("$.status").value("ESCALATED"))
				.andExpect(jsonPath("$.assignedTo").value("analyst-esc-42"))
				.andExpect(jsonPath("$.tenantId").value(TENANT))
				.andExpect(jsonPath("$.transactionId").value("txn-esc-1"));

		assertThatStoreShowsEscalated(alertId, "analyst-esc-42");

		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		verify(stdoutEventPublisher, timeout(PUBLISH_VERIFY_TIMEOUT_MS).times(1)).publish(eventCaptor.capture());
		Event published = eventCaptor.getValue();
		assertThat(published.alertId()).isEqualTo(alertId);
		assertThat(published.tenantId()).isEqualTo(TENANT);
		assertThat(published.eventType()).isEqualTo(EventType.ALERT_ESCALATED.getName());
		assertThat(published.status()).isEqualTo(AlertStatus.ESCALATED);
		assertThat(published.timestamp()).isNotNull();
	}

	@Test
	void escalateAlert_blankTenantId_returnsBadRequest() throws Exception {
		String patchJson =
				"""
				{
				  "tenantId": "",
				  "assignedTo": "a"
				}
				""";
		mockMvc.perform(
						patch("/sanctions/alerts/{id}/escalate", UUID.randomUUID())
								.contentType(MediaType.APPLICATION_JSON)
								.content(patchJson))
				.andExpect(status().isBadRequest());

		verify(stdoutEventPublisher, never()).publish(any());
	}

	@Test
	void escalateAlert_malformedJson_returnsBadRequest() throws Exception {
		mockMvc.perform(
						patch("/sanctions/alerts/{id}/escalate", UUID.randomUUID())
								.contentType(MediaType.APPLICATION_JSON)
								.content("{ not json"))
				.andExpect(status().isBadRequest());

		verify(stdoutEventPublisher, never()).publish(any());
	}

	@Test
	void escalateAlert_unknownAlertId_returnsNotFound() throws Exception {
		UUID randomId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
		String patchJson =
				"""
				{
				  "tenantId": "tenant-escalate-unknown",
				  "assignedTo": "nobody"
				}
				""";
		mockMvc.perform(
						patch("/sanctions/alerts/{id}/escalate", randomId)
								.contentType(MediaType.APPLICATION_JSON)
								.content(patchJson))
				.andExpect(status().isNotFound());

		verify(stdoutEventPublisher, never()).publish(any());
	}

	@Test
	void escalateAlert_afterDecided_returnsConflict() throws Exception {
		String createJson =
				"""
				{
				  "transactionId": "txn-esc-2",
				  "matchedEntityName": "N",
				  "matchScore": 0.5,
				  "tenantId": "%s"
				}
				"""
						.formatted(TENANT + "-decided");

		String createResponse =
				mockMvc.perform(post("/sanctions/alerts").contentType(MediaType.APPLICATION_JSON).content(createJson))
						.andExpect(status().isCreated())
						.andReturn()
						.getResponse()
						.getContentAsString();

		UUID alertId = UUID.fromString(JsonPath.read(createResponse, "$.id"));
		String t = TENANT + "-decided";

		String decisionJson =
				"""
				{
				  "tenantId": "%s",
				  "statusDecision": "CLEARED",
				  "decisionNote": "done"
				}
				"""
						.formatted(t);

		mockMvc.perform(
						patch("/sanctions/alerts/{id}/decision", alertId)
								.contentType(MediaType.APPLICATION_JSON)
								.content(decisionJson))
				.andExpect(status().isOk());

		String escalateJson =
				"""
				{
				  "tenantId": "%s",
				  "assignedTo": "late-escalation"
				}
				"""
						.formatted(t);

		mockMvc.perform(
						patch("/sanctions/alerts/{id}/escalate", alertId)
								.contentType(MediaType.APPLICATION_JSON)
								.content(escalateJson))
				.andExpect(status().isConflict());

		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		verify(stdoutEventPublisher, timeout(PUBLISH_VERIFY_TIMEOUT_MS).times(1)).publish(eventCaptor.capture());
		Event published = eventCaptor.getValue();
		assertThat(published.eventType()).isEqualTo(EventType.ALERT_DECIDED.getName());
		assertThat(published.status()).isEqualTo(AlertStatus.CLEARED);
	}

	private void assertThatStoreShowsEscalated(UUID alertId, String assignee) {
		var a = alertsRepository.getAlert(TENANT, alertId);
		assertThat(a).isNotNull();
		assertThat(a.status()).isEqualTo(AlertStatus.ESCALATED);
		assertThat(a.assignedTo()).isEqualTo(assignee);
	}
}
