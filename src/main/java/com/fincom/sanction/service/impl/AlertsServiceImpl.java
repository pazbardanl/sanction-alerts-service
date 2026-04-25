package com.fincom.sanction.service.impl;

import com.fincom.sanction.domain.Alert;
import com.fincom.sanction.domain.AlertStatus;
import com.fincom.sanction.domain.CreateAlertRequest;
import com.fincom.sanction.domain.EscalateAlertRequest;
import com.fincom.sanction.domain.UpdateAlertDecisionRequest;
import com.fincom.sanction.exception.AlertAlreadyDecidedException;
import com.fincom.sanction.exception.AlertNotFoundException;
import com.fincom.sanction.exception.InvalidTenantException;
import com.fincom.sanction.repository.AlertsRepository;
import com.fincom.sanction.service.AlertsService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AlertsServiceImpl implements AlertsService {

	private static final Logger log = LoggerFactory.getLogger(AlertsServiceImpl.class);
	private static final List<AlertStatus> DECISISON_STATUSES = List.of(AlertStatus.CONFIRMED_HIT, AlertStatus.CLEARED);

	private final AlertsRepository alertsRepository;

	public AlertsServiceImpl(AlertsRepository alertsRepository) {
		this.alertsRepository = alertsRepository;
	}

	@Override
	public Alert createAlert(CreateAlertRequest request) {
        LocalDateTime now = LocalDateTime.now();
		log.debug("createAlert: request={}", request);
        Alert alert = new Alert(
            UUID.randomUUID(), 
            request.transactionId(), 
            request.matchedEntityName(), 
            request.matchScore(), 
            AlertStatus.OPEN,
            null,
            request.tenantId(),
            now,
            now,
            null);
        Alert storedAlert = alertsRepository.storeAlert(alert);
		log.debug("createAlert: alert stored={}", storedAlert);
        return storedAlert;
	}

	@Override
	public List<Alert> getAlertsByFilter(String tenantId, AlertStatus status, Float minScore) {
		log.debug("getAlertsByFilter: tenantId={}, status={}, minScore={}", tenantId, status, minScore);
		validateTenantId(tenantId);
		List<Alert> alerts = alertsRepository.findAlertsByFilter(tenantId, status, minScore);
		log.debug("getAlertsByFilter: alerts found={}", alerts.size());
		return alerts;
	}

	@Override
	public Alert updateAlertDecision(UpdateAlertDecisionRequest request) {
		log.debug("updateAlertStatusAndDecisionNote: request={}", request);
		validateTenantId(request.tenantId());
		validateCanUpdateAlertDecision(request.tenantId(), request.alertId());
		Alert updatedAlert = alertsRepository.updateAlertStatusAndDecisionNote(request.tenantId(), request.alertId(), request.statusDecision(), request.decisionNote());
		log.debug("updateAlertStatusAndDecisionNote: updatedAlert={}", updatedAlert);
		return updatedAlert;
	}

	@Override
	public Alert escalateAlert(EscalateAlertRequest request) {
		validateTenantId(request.tenantId());
		LocalDateTime now = LocalDateTime.now();
		log.debug("escalateAlert: request={}", request);
		validateCanUpdateAlertDecision(request.tenantId(), request.alertId());
		Alert updatedAlert = alertsRepository.updateAlertStatusAndAssignedTo(
				request.tenantId(), request.alertId(), AlertStatus.ESCALATED, request.assignedTo(), now);
		log.debug("escalateAlert: updatedAlert={}", updatedAlert);
		return updatedAlert;
	}


	private void validateTenantId(String tenantId) {
		if (tenantId == null || tenantId.isEmpty()) {
			throw new InvalidTenantException("Tenant ID is required");
		}
	}

	private void validateCanUpdateAlertDecision(String tenantId, UUID alertId) {
		Alert alert = alertsRepository.getAlert(tenantId, alertId);
		if (alert == null) {
			throw new AlertNotFoundException("Alert not found");
		}
		if (DECISISON_STATUSES.contains(alert.status())) {
			throw new AlertAlreadyDecidedException(
					"Alert has already been decided: " + alert.status() + " at " + alert.updatedAt());
		}
	}
}	
