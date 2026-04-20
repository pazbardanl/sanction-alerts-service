package com.fincom.sanction.repository;

import java.util.List;
import java.util.UUID;

import com.fincom.sanction.domain.Alert;
import com.fincom.sanction.domain.AlertStatus;

public interface AlertsRepository {
    Alert storeAlert(Alert alert);
    Alert getAlert(String tenantId, UUID alertId);

    List<Alert> findAlertsByFilter(String tenantId, AlertStatus status, Float minScore);
}
