package com.fincom.sanction.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fincom.sanction.domain.Alert;
import com.fincom.sanction.domain.AlertStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlertsRepositoryInMemoryImplTest {

	private AlertsRepositoryInMemoryImpl repository;

	@BeforeEach
	void setUp() {
		repository = new AlertsRepositoryInMemoryImpl();
	}

	@Test
	void storeAlert_thenGetAlert_returnsSameAlert() {
		UUID id = UUID.randomUUID();
		Alert alert = sampleAlert(id, "tenant-a");

		Alert stored = repository.storeAlert(alert);

		assertThat(stored).isSameAs(alert);
		assertThat(repository.getAlert("tenant-a", id)).isSameAs(alert);
	}

	@Test
	void getAlert_unknownTenant_returnsNull() {
		assertThat(repository.getAlert("missing-tenant", UUID.randomUUID())).isNull();
	}

	@Test
	void getAlert_knownTenant_unknownAlertId_returnsNull() {
		repository.storeAlert(sampleAlert(UUID.randomUUID(), "tenant-a"));

		assertThat(repository.getAlert("tenant-a", UUID.randomUUID())).isNull();
	}

	@Test
	void storeAlert_isolatesAlertsByTenant() {
		UUID id = UUID.randomUUID();
		Alert forTenantA = sampleAlert(id, "tenant-a");
		Alert forTenantB = sampleAlert(id, "tenant-b");

		repository.storeAlert(forTenantA);
		repository.storeAlert(forTenantB);

		assertThat(repository.getAlert("tenant-a", id)).isSameAs(forTenantA);
		assertThat(repository.getAlert("tenant-b", id)).isSameAs(forTenantB);
	}

	@Test
	void storeAlert_sameTenantSameId_overwrites() {
		UUID id = UUID.randomUUID();
		Alert first = sampleAlert(id, "tenant-a");
		Alert second = new Alert(
				id,
				"tx-2",
				"other",
				0.5f,
				AlertStatus.ESCALATED,
				null,
				"tenant-a",
				first.createdAt(),
				first.updatedAt(),
				null);

		repository.storeAlert(first);
		repository.storeAlert(second);

		assertThat(repository.getAlert("tenant-a", id)).isSameAs(second);
	}

	@Test
	void storeAlert_nullTenantId_throws() {
		Alert alert = sampleAlert(UUID.randomUUID(), null);

		assertThatThrownBy(() -> repository.storeAlert(alert))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Tenant ID");
	}

	@Test
	void storeAlert_emptyTenantId_throws() {
		Alert alert = sampleAlert(UUID.randomUUID(), "");

		assertThatThrownBy(() -> repository.storeAlert(alert))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Tenant ID");
	}

	@Test
	void storeAlert_nullAlertId_throws() {
		Alert alert = sampleAlert(null, "tenant-a");

		assertThatThrownBy(() -> repository.storeAlert(alert))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Alert ID");
	}

	private static Alert sampleAlert(UUID id, String tenantId) {
		LocalDateTime now = LocalDateTime.now();
		return new Alert(
				id,
				"txn-1",
				"entity",
				1f,
				AlertStatus.OPEN,
				null,
				tenantId,
				now,
				now,
				null);
	}
}
