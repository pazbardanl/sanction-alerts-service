package com.fincom.sanction.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fincom.sanction.domain.alert.Alert;
import com.fincom.sanction.domain.alert.AlertStatus;
import com.fincom.sanction.repository.AlertsRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class CreateAlertIntegrationTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private AlertsRepository alertsRepository;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	void createAlert_flowFromControllerThroughServiceToRepository() throws Exception {
		String requestJson =
				"""
				{
				  "transactionId": "txn-integration",
				  "matchedEntityName": "ACME Corp",
				  "matchScore": 0.91,
				  "tenantId": "tenant-integration"
				}
				""";

		MvcResult mvcResult = mockMvc
				.perform(post("/sanctions/alerts").contentType(MediaType.APPLICATION_JSON).content(requestJson))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.transactionId").value("txn-integration"))
				.andExpect(jsonPath("$.matchedEntityName").value("ACME Corp"))
				.andExpect(jsonPath("$.matchScore").value(0.91))
				.andExpect(jsonPath("$.tenantId").value("tenant-integration"))
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.assignedTo").doesNotExist())
				.andExpect(jsonPath("$.decisionNote").doesNotExist())
				.andReturn();

		String responseBody = mvcResult.getResponse().getContentAsString();
		String idStr = JsonPath.read(responseBody, "$.id");
		UUID alertId = UUID.fromString(idStr);

		Alert stored = alertsRepository.getAlert("tenant-integration", alertId);
		assertThat(stored).isNotNull();
		assertThat(stored.id()).isEqualTo(alertId);
		assertThat(stored.transactionId()).isEqualTo("txn-integration");
		assertThat(stored.matchedEntityName()).isEqualTo("ACME Corp");
		assertThat(stored.matchScore()).isEqualTo(0.91f);
		assertThat(stored.tenantId()).isEqualTo("tenant-integration");
		assertThat(stored.status()).isEqualTo(AlertStatus.OPEN);
		assertThat(stored.assignedTo()).isNull();
		assertThat(stored.decisionNote()).isNull();
	}

	@Test
	void createAlert_blankTransactionId_returnsBadRequest() throws Exception {
		String body =
				"""
				{
				  "transactionId": "",
				  "matchedEntityName": "ACME Corp",
				  "matchScore": 0.91,
				  "tenantId": "tenant-integration"
				}
				""";
		mockMvc.perform(post("/sanctions/alerts").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAlert_blankMatchedEntityName_returnsBadRequest() throws Exception {
		String body =
				"""
				{
				  "transactionId": "txn-1",
				  "matchedEntityName": "   ",
				  "matchScore": 0.91,
				  "tenantId": "tenant-integration"
				}
				""";
		mockMvc.perform(post("/sanctions/alerts").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAlert_nullMatchScore_returnsBadRequest() throws Exception {
		String body =
				"""
				{
				  "transactionId": "txn-1",
				  "matchedEntityName": "ACME",
				  "matchScore": null,
				  "tenantId": "tenant-integration"
				}
				""";
		mockMvc.perform(post("/sanctions/alerts").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAlert_blankTenantId_returnsBadRequest() throws Exception {
		String body =
				"""
				{
				  "transactionId": "txn-1",
				  "matchedEntityName": "ACME",
				  "matchScore": 0.91,
				  "tenantId": ""
				}
				""";
		mockMvc.perform(post("/sanctions/alerts").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAlert_missingTransactionId_returnsBadRequest() throws Exception {
		String body =
				"""
				{
				  "matchedEntityName": "ACME",
				  "matchScore": 0.91,
				  "tenantId": "tenant-integration"
				}
				""";
		mockMvc.perform(post("/sanctions/alerts").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAlert_malformedJson_returnsBadRequest() throws Exception {
		mockMvc.perform(post("/sanctions/alerts").contentType(MediaType.APPLICATION_JSON).content("{ not json"))
				.andExpect(status().isBadRequest());
	}
}
