package com.fincom.sanction.repository.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.fincom.sanction.domain.alert.Alert;
import com.fincom.sanction.domain.alert.AlertStatus;
import com.fincom.sanction.exception.AlertNotFoundException;
import com.fincom.sanction.repository.AlertsRepository;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: clean code
@Repository
public class AlertsRepositoryInMemoryImpl implements AlertsRepository {

	private static final Logger log = LoggerFactory.getLogger(AlertsRepositoryInMemoryImpl.class);
	private final Map<String, Map<UUID, Alert>> tenantIdToAlertIdToAlert = new HashMap<>();

	/** Mutex per (tenantId, alertId) — only used by {@link #updateAlertStatusAndDecisionNote}. */
	private final ConcurrentHashMap<String, Object> statusAndDecisionNoteUpdateLocks = new ConcurrentHashMap<>();

	private Object lockForStatusAndDecisionNoteUpdate(String tenantId, UUID alertId) {
		return statusAndDecisionNoteUpdateLocks.computeIfAbsent(tenantId + "\0" + alertId, k -> new Object());
	}

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

	@Override
	public Alert updateAlertStatusAndDecisionNote(String tenantId, UUID alertId, AlertStatus status,
			String decisionNote) {
		log.debug("updateAlertStatusAndDecisionNote: tenantId={}, alertId={}, status={}, decisionNote={}", tenantId, alertId, status, decisionNote);
		Alert updatedAlert = synchronizedUpdateAlertStatusAndDecisionNote(tenantId, alertId, status, decisionNote);
		log.debug("updateAlertStatusAndDecisionNote: updatedAlert={}", updatedAlert);
		return updatedAlert;
	}

	private Alert synchronizedUpdateAlertStatusAndDecisionNote(
			String tenantId, UUID alertId, AlertStatus status, String decisionNote) {
		synchronized (lockForStatusAndDecisionNoteUpdate(tenantId, alertId)) {
			Alert alert = getAlertOrThrow(tenantId, alertId);
			Alert updatedAlert = Alert.builderFrom(alert)
					.status(status)
					.decisionNote(decisionNote)
					.build();
			tenantIdToAlertIdToAlert
					.computeIfAbsent(updatedAlert.tenantId(), k -> new HashMap<>())
					.put(updatedAlert.id(), updatedAlert);
			return updatedAlert;
		}
	}

	@Override
	public Alert updateAlertStatusAndAssignedTo(String tenantId, UUID alertId, AlertStatus status, String assignedTo, LocalDateTime updatedAt) {
		log.debug(
				"updateAlertStatusAndAssignedTo: tenantId={}, alertId={}, status={}, assignedTo={}, updatedAt={}",
				tenantId, alertId, status, assignedTo, updatedAt);
		Alert alert = getAlertOrThrow(tenantId, alertId);
		Alert updatedAlert = Alert.builderFrom(alert)
			.status(status)
			.assignedTo(assignedTo)
			.updatedAt(updatedAt)
			.build();
		return storeAlert(updatedAlert);
	}

	private void validateAlert(Alert alert) {
		if (alert.tenantId() == null || alert.tenantId().isEmpty() || alert.id() == null) {
			throw new IllegalArgumentException("Tenant ID and Alert ID are required");
		}
	}

	private Alert getAlertOrThrow(String tenantId, UUID alertId) {
		Alert alert = getAlert(tenantId, alertId);
		if (alert == null) {
			throw new AlertNotFoundException("Alert not found");
		}
		return alert;
	}
}
