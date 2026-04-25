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
class UpdateAlertDecisionIntegrationTest {

	/** Publishes run on event worker threads; async verification needs a wait window. */
	private static final int PUBLISH_VERIFY_TIMEOUT_MS = 5_000;

	private static final String TENANT = "tenant-update-decision-flow";

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
	void updateAlertDecision_patchThroughControllerServiceAndRepository() throws Exception {
		String createJson =
				"""
				{
				  "transactionId": "txn-patch-flow",
				  "matchedEntityName": "ACME",
				  "matchScore": 0.88,
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
				  "statusDecision": "CLEARED",
				  "decisionNote": "cleared after manual review"
				}
				"""
						.formatted(TENANT);

		String patchResponse =
				mockMvc.perform(
								patch("/sanctions/alerts/{id}/decision", alertId)
										.contentType(MediaType.APPLICATION_JSON)
										.content(patchJson))
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.id").value(alertId.toString()))
						.andExpect(jsonPath("$.status").value("CLEARED"))
						.andExpect(jsonPath("$.decisionNote").value("cleared after manual review"))
						.andExpect(jsonPath("$.tenantId").value(TENANT))
						.andExpect(jsonPath("$.transactionId").value("txn-patch-flow"))
						.andReturn()
						.getResponse()
						.getContentAsString();

		assertThat(JsonPath.<String>read(patchResponse, "$.id")).isEqualTo(alertId.toString());

		assertThat(alertsRepository.getAlert(TENANT, alertId).status()).isEqualTo(AlertStatus.CLEARED);
		assertThat(alertsRepository.getAlert(TENANT, alertId).decisionNote())
				.isEqualTo("cleared after manual review");

		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		verify(stdoutEventPublisher, timeout(PUBLISH_VERIFY_TIMEOUT_MS).times(1)).publish(eventCaptor.capture());
		Event published = eventCaptor.getValue();
		assertThat(published.alertId()).isEqualTo(alertId);
		assertThat(published.tenantId()).isEqualTo(TENANT);
		assertThat(published.eventType()).isEqualTo(EventType.ALERT_DECIDED.getName());
		assertThat(published.status()).isEqualTo(AlertStatus.CLEARED);
		assertThat(published.timestamp()).isNotNull();
	}

	@Test
	void updateAlertDecision_whenAlreadyDecided_returnsConflict() throws Exception {
		String createJson =
				"""
				{
				  "transactionId": "txn-twice",
				  "matchedEntityName": "Co",
				  "matchScore": 0.9,
				  "tenantId": "%s"
				}
				"""
						.formatted(TENANT + "-twice");

		String createResponse =
				mockMvc.perform(post("/sanctions/alerts").contentType(MediaType.APPLICATION_JSON).content(createJson))
						.andExpect(status().isCreated())
						.andReturn()
						.getResponse()
						.getContentAsString();

		UUID alertId = UUID.fromString(JsonPath.read(createResponse, "$.id"));
		String tenantTwice = TENANT + "-twice";

		String firstPatch =
				"""
				{
				  "tenantId": "%s",
				  "statusDecision": "CLEARED",
				  "decisionNote": "first"
				}
				"""
						.formatted(tenantTwice);

		mockMvc.perform(
						patch("/sanctions/alerts/{id}/decision", alertId)
								.contentType(MediaType.APPLICATION_JSON)
								.content(firstPatch))
				.andExpect(status().isOk());

		String secondPatch =
				"""
				{
				  "tenantId": "%s",
				  "statusDecision": "CONFIRMED_HIT",
				  "decisionNote": "second"
				}
				"""
						.formatted(tenantTwice);

		mockMvc.perform(
						patch("/sanctions/alerts/{id}/decision", alertId)
								.contentType(MediaType.APPLICATION_JSON)
								.content(secondPatch))
				.andExpect(status().isConflict());

		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		verify(stdoutEventPublisher, timeout(PUBLISH_VERIFY_TIMEOUT_MS).times(1)).publish(eventCaptor.capture());
		Event published = eventCaptor.getValue();
		assertThat(published.eventType()).isEqualTo(EventType.ALERT_DECIDED.getName());
		assertThat(published.status()).isEqualTo(AlertStatus.CLEARED);
		assertThat(published.alertId()).isEqualTo(alertId);
		assertThat(published.tenantId()).isEqualTo(tenantTwice);
	}

	@Test
	void updateAlertDecision_blankTenantId_returnsBadRequest() throws Exception {
		UUID id = UUID.randomUUID();
		String patchJson =
				"""
				{
				  "tenantId": "",
				  "statusDecision": "CLEARED",
				  "decisionNote": "n"
				}
				""";
		mockMvc.perform(
						patch("/sanctions/alerts/{id}/decision", id)
								.contentType(MediaType.APPLICATION_JSON)
								.content(patchJson))
				.andExpect(status().isBadRequest());

		verify(stdoutEventPublisher, never()).publish(any());
	}

	@Test
	void updateAlertDecision_missingTenantId_returnsBadRequest() throws Exception {
		UUID id = UUID.randomUUID();
		String patchJson =
				"""
				{
				  "statusDecision": "CLEARED",
				  "decisionNote": "n"
				}
				""";
		mockMvc.perform(
						patch("/sanctions/alerts/{id}/decision", id)
								.contentType(MediaType.APPLICATION_JSON)
								.content(patchJson))
				.andExpect(status().isBadRequest());

		verify(stdoutEventPublisher, never()).publish(any());
	}

	@Test
	void updateAlertDecision_nullStatusDecision_returnsBadRequest() throws Exception {
		UUID id = UUID.randomUUID();
		String patchJson =
				"""
				{
				  "tenantId": "some-tenant",
				  "statusDecision": null,
				  "decisionNote": "n"
				}
				""";
		mockMvc.perform(
						patch("/sanctions/alerts/{id}/decision", id)
								.contentType(MediaType.APPLICATION_JSON)
								.content(patchJson))
				.andExpect(status().isBadRequest());

		verify(stdoutEventPublisher, never()).publish(any());
	}

	@Test
	void updateAlertDecision_malformedJson_returnsBadRequest() throws Exception {
		mockMvc.perform(
						patch("/sanctions/alerts/{id}/decision", UUID.randomUUID())
								.contentType(MediaType.APPLICATION_JSON)
								.content("{ not json"))
				.andExpect(status().isBadRequest());

		verify(stdoutEventPublisher, never()).publish(any());
	}

	@Test
	void updateAlertDecision_invalidStatusEnum_returnsBadRequest() throws Exception {
		UUID id = UUID.randomUUID();
		String patchJson =
				"""
				{
				  "tenantId": "t",
				  "statusDecision": "NOT_A_REAL_STATUS",
				  "decisionNote": "n"
				}
				""";
		mockMvc.perform(
						patch("/sanctions/alerts/{id}/decision", id)
								.contentType(MediaType.APPLICATION_JSON)
								.content(patchJson))
				.andExpect(status().isBadRequest());

		verify(stdoutEventPublisher, never()).publish(any());
	}

	@Test
	void updateAlertDecision_unknownAlertId_returnsNotFound() throws Exception {
		UUID randomId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
		String patchJson =
				"""
				{
				  "tenantId": "tenant-unknown-patch",
				  "statusDecision": "CLEARED",
				  "decisionNote": "n"
				}
				""";
		mockMvc.perform(
						patch("/sanctions/alerts/{id}/decision", randomId)
								.contentType(MediaType.APPLICATION_JSON)
								.content(patchJson))
				.andExpect(status().isNotFound());

		verify(stdoutEventPublisher, never()).publish(any());
	}

	@Test
	void updateAlertDecision_wrongTenantForAlert_returnsNotFound() throws Exception {
		String createJson =
				"""
				{
				  "transactionId": "txn-wrong-tenant",
				  "matchedEntityName": "X",
				  "matchScore": 0.5,
				  "tenantId": "tenant-real-owner"
				}
				""";

		String createResponse =
				mockMvc.perform(post("/sanctions/alerts").contentType(MediaType.APPLICATION_JSON).content(createJson))
						.andExpect(status().isCreated())
						.andReturn()
						.getResponse()
						.getContentAsString();

		UUID alertId = UUID.fromString(JsonPath.read(createResponse, "$.id"));

		String patchWrongTenant =
				"""
				{
				  "tenantId": "tenant-impostor",
				  "statusDecision": "CLEARED",
				  "decisionNote": "hijack"
				}
				""";

		mockMvc.perform(
						patch("/sanctions/alerts/{id}/decision", alertId)
								.contentType(MediaType.APPLICATION_JSON)
								.content(patchWrongTenant))
				.andExpect(status().isNotFound());

		verify(stdoutEventPublisher, never()).publish(any());
	}
}
