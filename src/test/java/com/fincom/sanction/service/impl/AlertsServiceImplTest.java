package com.fincom.sanction.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fincom.sanction.domain.Alert;
import com.fincom.sanction.domain.AlertStatus;
import com.fincom.sanction.domain.CreateAlertRequest;
import com.fincom.sanction.repository.AlertsRepository;
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
}
