package com.fincom.sanction.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fincom.sanction.domain.Alert;
import com.fincom.sanction.domain.AlertStatus;
import com.fincom.sanction.domain.CreateAlertRequest;
import com.fincom.sanction.domain.UpdateAlertDecisionRequest;
import com.fincom.sanction.repository.AlertsRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertsServiceImplTest {

	@Mock
	private AlertsRepository alertsRepository;

	@InjectMocks
	private AlertsServiceImpl alertsService;

	@Test
	void createAlert_persistsNewOpenAlertAndReturnsStoredValue() {
		CreateAlertRequest request = new CreateAlertRequest("txn-1", "ACME", 0.95f, "tenant-a");
		when(alertsRepository.storeAlert(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Alert result = alertsService.createAlert(request);

		ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
		verify(alertsRepository).storeAlert(alertCaptor.capture());

		Alert passedToRepository = alertCaptor.getValue();
		assertThat(passedToRepository).isSameAs(result);
		assertThat(passedToRepository.id()).isNotNull();
		assertThat(passedToRepository.transactionId()).isEqualTo("txn-1");
		assertThat(passedToRepository.matchedEntityName()).isEqualTo("ACME");
		assertThat(passedToRepository.matchScore()).isEqualTo(0.95f);
		assertThat(passedToRepository.status()).isEqualTo(AlertStatus.OPEN);
		assertThat(passedToRepository.assignedTo()).isNull();
		assertThat(passedToRepository.tenantId()).isEqualTo("tenant-a");
		assertThat(passedToRepository.createdAt()).isNotNull();
		assertThat(passedToRepository.updatedAt()).isNotNull();
		assertThat(passedToRepository.createdAt()).isEqualTo(passedToRepository.updatedAt());
		assertThat(passedToRepository.decisionNote()).isNull();
	}

	@Test
	void createAlert_returnsWhateverRepositoryReturns() {
		CreateAlertRequest request = new CreateAlertRequest("t", "e", 1f, "tenant");
		Alert fromRepo = new Alert(
				UUID.randomUUID(),
				"t",
				"e",
				1f,
				AlertStatus.ESCALATED,
				"analyst",
				"tenant",
				null,
				null,
				"note");
		when(alertsRepository.storeAlert(any(Alert.class))).thenReturn(fromRepo);

		Alert result = alertsService.createAlert(request);

		assertThat(result).isSameAs(fromRepo);
		verify(alertsRepository).storeAlert(any(Alert.class));
	}

	@Test
	void updateAlertDecision_delegatesToRepositoryWhenAlertIsOpen() {
		UUID alertId = UUID.randomUUID();
		LocalDateTime t = LocalDateTime.of(2026, 4, 20, 12, 0);
		Alert existing =
				new Alert(
						alertId,
						"tx",
						"e",
						0.9f,
						AlertStatus.OPEN,
						null,
						"tenant-d",
						t,
						t,
						null);
		Alert updated =
				new Alert(
						alertId,
						"tx",
						"e",
						0.9f,
						AlertStatus.CLEARED,
						null,
						"tenant-d",
						t,
						t,
						"reviewed");
		UpdateAlertDecisionRequest req =
				new UpdateAlertDecisionRequest(alertId, "tenant-d", AlertStatus.CLEARED, "reviewed");
		when(alertsRepository.getAlert("tenant-d", alertId)).thenReturn(existing);
		when(alertsRepository.updateAlertStatusAndDecisionNote(
						eq("tenant-d"), eq(alertId), eq(AlertStatus.CLEARED), eq("reviewed")))
				.thenReturn(updated);

		Alert result = alertsService.updateAlertDecision(req);

		assertThat(result).isSameAs(updated);
		verify(alertsRepository)
				.updateAlertStatusAndDecisionNote(
						eq("tenant-d"), eq(alertId), eq(AlertStatus.CLEARED), eq("reviewed"));
	}

	@Test
	void updateAlertDecision_whenEscalated_stillAllowsDecisionUpdate() {
		UUID alertId = UUID.randomUUID();
		LocalDateTime t = LocalDateTime.of(2026, 1, 1, 0, 0);
		Alert existing =
				new Alert(
						alertId, "tx", "e", 1f, AlertStatus.ESCALATED, null, "tenant-e", t, t, null);
		Alert after =
				new Alert(
						alertId,
						"tx",
						"e",
						1f,
						AlertStatus.CONFIRMED_HIT,
						null,
						"tenant-e",
						t,
						t,
						"confirmed");
		UpdateAlertDecisionRequest req =
				new UpdateAlertDecisionRequest(
						alertId, "tenant-e", AlertStatus.CONFIRMED_HIT, "confirmed");
		when(alertsRepository.getAlert("tenant-e", alertId)).thenReturn(existing);
		when(alertsRepository.updateAlertStatusAndDecisionNote(
						eq("tenant-e"), eq(alertId), eq(AlertStatus.CONFIRMED_HIT), eq("confirmed")))
				.thenReturn(after);

		Alert result = alertsService.updateAlertDecision(req);

		assertThat(result).isSameAs(after);
	}

	@Test
	void updateAlertDecision_nullTenantId_throwsAndDoesNotUpdate() {
		UpdateAlertDecisionRequest req =
				new UpdateAlertDecisionRequest(UUID.randomUUID(), null, AlertStatus.CLEARED, "n");

		assertThatThrownBy(() -> alertsService.updateAlertDecision(req))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Tenant ID");

		verify(alertsRepository, never()).updateAlertStatusAndDecisionNote(any(), any(), any(), any());
	}

	@Test
	void updateAlertDecision_emptyTenantId_throwsAndDoesNotUpdate() {
		UpdateAlertDecisionRequest req =
				new UpdateAlertDecisionRequest(UUID.randomUUID(), "", AlertStatus.CLEARED, "n");

		assertThatThrownBy(() -> alertsService.updateAlertDecision(req))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Tenant ID");

		verify(alertsRepository, never()).updateAlertStatusAndDecisionNote(any(), any(), any(), any());
	}

	@Test
	void updateAlertDecision_nullAlertId_throwsAndDoesNotUpdate() {
		UpdateAlertDecisionRequest req =
				new UpdateAlertDecisionRequest(null, "tenant-x", AlertStatus.CLEARED, "n");

		assertThatThrownBy(() -> alertsService.updateAlertDecision(req))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Alert ID");

		verify(alertsRepository, never()).updateAlertStatusAndDecisionNote(any(), any(), any(), any());
	}

	@Test
	void updateAlertDecision_alertNotFound_throwsAndDoesNotUpdate() {
		UUID alertId = UUID.randomUUID();
		UpdateAlertDecisionRequest req =
				new UpdateAlertDecisionRequest(alertId, "tenant-m", AlertStatus.CLEARED, "n");
		when(alertsRepository.getAlert("tenant-m", alertId)).thenReturn(null);

		assertThatThrownBy(() -> alertsService.updateAlertDecision(req))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Alert not found");

		verify(alertsRepository, never()).updateAlertStatusAndDecisionNote(any(), any(), any(), any());
	}

	@Test
	void updateAlertDecision_alreadyConfirmed_throwsAndDoesNotUpdate() {
		UUID alertId = UUID.randomUUID();
		LocalDateTime t = LocalDateTime.of(2026, 3, 15, 9, 30);
		Alert decided =
				new Alert(
						alertId,
						"tx",
						"e",
						1f,
						AlertStatus.CONFIRMED_HIT,
						null,
						"tenant-f",
						t,
						t,
						"earlier");
		UpdateAlertDecisionRequest req =
				new UpdateAlertDecisionRequest(alertId, "tenant-f", AlertStatus.CLEARED, "late");
		when(alertsRepository.getAlert("tenant-f", alertId)).thenReturn(decided);

		assertThatThrownBy(() -> alertsService.updateAlertDecision(req))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("already been decided");

		verify(alertsRepository, never()).updateAlertStatusAndDecisionNote(any(), any(), any(), any());
	}

	@Test
	void updateAlertDecision_alreadyCleared_throwsAndDoesNotUpdate() {
		UUID alertId = UUID.randomUUID();
		LocalDateTime t = LocalDateTime.of(2026, 3, 16, 10, 0);
		Alert decided =
				new Alert(
						alertId, "tx", "e", 1f, AlertStatus.CLEARED, null, "tenant-g", t, t, "done");
		UpdateAlertDecisionRequest req =
				new UpdateAlertDecisionRequest(alertId, "tenant-g", AlertStatus.CONFIRMED_HIT, "x");
		when(alertsRepository.getAlert("tenant-g", alertId)).thenReturn(decided);

		assertThatThrownBy(() -> alertsService.updateAlertDecision(req))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("already been decided");

		verify(alertsRepository, never()).updateAlertStatusAndDecisionNote(any(), any(), any(), any());
	}
}
