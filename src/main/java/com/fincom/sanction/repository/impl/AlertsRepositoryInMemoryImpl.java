package com.fincom.sanction.repository.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import com.fincom.sanction.domain.Alert;
import com.fincom.sanction.domain.AlertStatus;
import com.fincom.sanction.repository.AlertsRepository;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
public class AlertsRepositoryInMemoryImpl implements AlertsRepository {

	private static final Logger log = LoggerFactory.getLogger(AlertsRepositoryInMemoryImpl.class);
	private final Map<String, Map<UUID, Alert>> tenantIdToAlertIdToAlert = new HashMap<>();


	@Override
	public Alert storeAlert(Alert alert) {
		log.debug("storeAlert: alert={}", alert);
		validateAlert(alert);
		tenantIdToAlertIdToAlert.computeIfAbsent(alert.tenantId(), k -> new HashMap<>()).put(alert.id(), alert);
		log.debug("storeAlert: alert stored={}", alert);
		return alert;
	}

	@Override
	public Alert getAlert(String tenantId, UUID alertId) {
		log.debug("getAlert: tenantId={}, alertId={}", tenantId, alertId);
		return tenantIdToAlertIdToAlert.getOrDefault(tenantId, new HashMap<>()).get(alertId);
	}

	@Override
	public List<Alert> findAlertsByFilter(String tenantId, AlertStatus status, Float minScore) {
		log.debug("findAlertsByFilter: tenantId={}, status={}, minScore={}", tenantId, status, minScore);
		if (tenantId == null || tenantId.isEmpty()) {
			throw new IllegalArgumentException("tenantId is required");
		}
		Map<UUID, Alert> forTenant = tenantIdToAlertIdToAlert.getOrDefault(tenantId, Map.of());
		Stream<Alert> stream = forTenant.values().stream();
		if (status != null) {
			stream = stream.filter(a -> a.status() == status);
		}
		if (minScore != null) {
			stream = stream.filter(a -> a.matchScore() >= minScore);
		}
		return stream.toList();
	}

	private void validateAlert(Alert alert) {
		if (alert.tenantId() == null || alert.tenantId().isEmpty() || alert.id() == null) {
			throw new IllegalArgumentException("Tenant ID and Alert ID are required");
		}
	}
}
