package com.fincom.sanction.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fincom.sanction.domain.Alert;
import com.fincom.sanction.repository.AlertsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Uses a mocked {@link AlertsRepository} to assert behavior when persistence fails. Uses a real
 * HTTP connector ({@link SpringBootTest.WebEnvironment#RANDOM_PORT}) so errors from the service
 * layer are turned into HTTP 500 responses; {@code MockMvc} alone can surface those failures as
 * servlet exceptions instead of a status code.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CreateAlertRepositoryFailureIntegrationTest {

	private static final String VALID_BODY =
			"""
			{
			  "transactionId": "txn-1",
			  "matchedEntityName": "Entity",
			  "matchScore": 0.5,
			  "tenantId": "tenant-1"
			}
			""";

	@LocalServerPort
	private int port;

	@MockitoBean
	private AlertsRepository alertsRepository;

	@Test
	void createAlert_repositoryThrowsIllegalArgument_returnsInternalServerError() {
		when(alertsRepository.storeAlert(any(Alert.class)))
				.thenThrow(new IllegalArgumentException("Tenant ID and Alert ID are required"));

		assertThatThrownBy(this::postValidCreateAlert)
				.isInstanceOf(HttpServerErrorException.class)
				.satisfies(ex -> assertThat(((HttpServerErrorException) ex).getStatusCode().value()).isEqualTo(500));
	}

	@Test
	void createAlert_repositoryThrowsRuntimeException_returnsInternalServerError() {
		when(alertsRepository.storeAlert(any(Alert.class))).thenThrow(new RuntimeException("database unavailable"));

		assertThatThrownBy(this::postValidCreateAlert)
				.isInstanceOf(HttpServerErrorException.class)
				.satisfies(ex -> assertThat(((HttpServerErrorException) ex).getStatusCode().value()).isEqualTo(500));
	}

	private void postValidCreateAlert() {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<>(VALID_BODY, headers);
		restTemplate.postForEntity("http://localhost:" + port + "/sanctions/alerts", entity, String.class);
	}
}
