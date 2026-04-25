package com.fincom.sanction.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fincom.sanction.domain.Alert;
import com.fincom.sanction.domain.AlertStatus;
import com.fincom.sanction.exception.AlertNotFoundException;
import com.fincom.sanction.repository.AlertsRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Exercises {@link AlertsRepository} update methods with the real Spring-managed in-memory
 * implementation (no HTTP layer — repository API is not exposed on the controller yet).
 */
@SpringBootTest
class AlertUpdateRepositoryIntegrationTest {

	@Autowired
	private AlertsRepository alertsRepository;

	@Test
	void updateAlertStatusAndDecisionNote_inSpringContext() {
		String tenant = "tenant-int-decision-note";
		UUID id = UUID.randomUUID();
		LocalDateTime now = LocalDateTime.now();
		alertsRepository.storeAlert(
				new Alert(
						id,
						"tx-int",
						"entity",
						0.8f,
						AlertStatus.OPEN,
						null,
						tenant,
						now,
						now,
						null));

		Alert updated =
				alertsRepository.updateAlertStatusAndDecisionNote(
						tenant, id, AlertStatus.CONFIRMED_HIT, "confirmed after review");

		assertThat(updated.status()).isEqualTo(AlertStatus.CONFIRMED_HIT);
		assertThat(updated.decisionNote()).isEqualTo("confirmed after review");
		assertThat(alertsRepository.getAlert(tenant, id)).isEqualTo(updated);
	}

	@Test
	void updateAlertStatusAndAssignedTo_inSpringContext() {
		String tenant = "tenant-int-assigned";
		UUID id = UUID.randomUUID();
		LocalDateTime now = LocalDateTime.now();
		alertsRepository.storeAlert(
				new Alert(
						id,
						"tx-int-2",
						"entity",
						0.5f,
						AlertStatus.OPEN,
						null,
						tenant,
						now,
						now,
						null));

		LocalDateTime updatedAt = now.plusDays(1);
		Alert updated =
				alertsRepository.updateAlertStatusAndAssignedTo(
						tenant, id, AlertStatus.ESCALATED, "case-owner-42", updatedAt);

		assertThat(updated.status()).isEqualTo(AlertStatus.ESCALATED);
		assertThat(updated.assignedTo()).isEqualTo("case-owner-42");
		assertThat(updated.updatedAt()).isEqualTo(updatedAt);
		assertThat(alertsRepository.getAlert(tenant, id)).isEqualTo(updated);
	}

	@Test
	void updateAlertStatusAndDecisionNote_notFound_inSpringContext() {
		assertThatThrownBy(
						() ->
								alertsRepository.updateAlertStatusAndDecisionNote(
										"no-tenant", UUID.randomUUID(), AlertStatus.CLEARED, "x"))
				.isInstanceOf(AlertNotFoundException.class)
				.hasMessageContaining("Alert not found");
	}

	@Test
	void updateAlertStatusAndAssignedTo_notFound_inSpringContext() {
		assertThatThrownBy(
						() ->
								alertsRepository.updateAlertStatusAndAssignedTo(
										"no-tenant-2",
										UUID.randomUUID(),
										AlertStatus.ESCALATED,
										"nobody",
										LocalDateTime.now()))
				.isInstanceOf(AlertNotFoundException.class)
				.hasMessageContaining("Alert not found");
	}
}
