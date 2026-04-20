package com.fincom.sanction.repository;

import java.util.UUID;

import com.fincom.sanction.domain.Alert;

public interface AlertsRepository {
    Alert storeAlert(Alert alert);
    Alert getAlert(String tenantId, UUID alertId);
}
