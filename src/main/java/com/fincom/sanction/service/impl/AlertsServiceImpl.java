package com.fincom.sanction.service.impl;

import com.fincom.sanction.domain.Alert;
import com.fincom.sanction.domain.AlertStatus;
import com.fincom.sanction.domain.CreateAlertRequest;
import com.fincom.sanction.repository.AlertsRepository;
import com.fincom.sanction.service.AlertsService;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AlertsServiceImpl implements AlertsService {

	private static final Logger log = LoggerFactory.getLogger(AlertsServiceImpl.class);

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

}
