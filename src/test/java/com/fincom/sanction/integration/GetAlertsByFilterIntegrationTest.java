package com.fincom.sanction.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
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
class GetAlertsByFilterIntegrationTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	void getAlertsByFilter_tenantOnly_returnsAllAlertsForTenant() throws Exception {
		String tenant = "tenant-filter-only";
		postAlert(tenant, "txn-a", 0.5f);
		postAlert(tenant, "txn-b", 0.95f);

		String body = getJson("/sanctions/alerts?tenantId=" + tenant);
		assertThat(JsonPath.<List<?>>read(body, "$")).hasSize(2);
	}

	@Test
	void getAlertsByFilter_tenantAndMinScore_filtersByMinimumScore() throws Exception {
		String tenant = "tenant-filter-minscore";
		postAlert(tenant, "txn-low", 0.5f);
		postAlert(tenant, "txn-high", 0.95f);

		String body = getJson("/sanctions/alerts?tenantId=" + tenant + "&minScore=0.7");
		assertThat(JsonPath.<List<?>>read(body, "$")).hasSize(1);
		assertThat(((Number) JsonPath.read(body, "$[0].matchScore")).floatValue()).isEqualTo(0.95f);
		assertThat(JsonPath.<String>read(body, "$[0].transactionId")).isEqualTo("txn-high");
	}

	@Test
	void getAlertsByFilter_tenantAndStatus_filtersByStatus() throws Exception {
		String tenant = "tenant-filter-status";
		postAlert(tenant, "txn-1", 0.5f);
		postAlert(tenant, "txn-2", 0.9f);

		String openBody = getJson("/sanctions/alerts?tenantId=" + tenant + "&status=OPEN");
		assertThat(JsonPath.<List<?>>read(openBody, "$")).hasSize(2);

		String escalatedBody = getJson("/sanctions/alerts?tenantId=" + tenant + "&status=ESCALATED");
		assertThat(JsonPath.<List<?>>read(escalatedBody, "$")).isEmpty();
	}

	@Test
	void getAlertsByFilter_allParameters_appliesTenantStatusAndMinScore() throws Exception {
		String tenant = "tenant-filter-all";
		postAlert(tenant, "txn-low", 0.5f);
		postAlert(tenant, "txn-high", 0.95f);

		String body = getJson("/sanctions/alerts?tenantId=" + tenant + "&status=OPEN&minScore=0.7");
		assertThat(JsonPath.<List<?>>read(body, "$")).hasSize(1);
		assertThat(JsonPath.<String>read(body, "$[0].transactionId")).isEqualTo("txn-high");
		assertThat(((Number) JsonPath.read(body, "$[0].matchScore")).floatValue()).isEqualTo(0.95f);
	}

	@Test
	void getAlertsByFilter_unknownTenant_returnsEmptyArray() throws Exception {
		String body = getJson("/sanctions/alerts?tenantId=unknown-tenant-xyz");
		assertThat(JsonPath.<List<?>>read(body, "$")).isEmpty();
	}

	@Test
	void getAlertsByFilter_missingTenantId_returnsBadRequest() throws Exception {
		mockMvc.perform(get("/sanctions/alerts").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

	private void postAlert(String tenantId, String transactionId, float matchScore) throws Exception {
		String json =
				"""
				{
				  "transactionId": "%s",
				  "matchedEntityName": "Entity",
				  "matchScore": %s,
				  "tenantId": "%s"
				}
				"""
						.formatted(transactionId, matchScore, tenantId);
		mockMvc.perform(post("/sanctions/alerts").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated());
	}

	private String getJson(String uri) throws Exception {
		MvcResult result =
				mockMvc.perform(get(uri).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();
		return result.getResponse().getContentAsString();
	}
}
