package com.fincom.sanction.service;

import com.fincom.sanction.domain.alert.Alert;
import com.fincom.sanction.domain.alert.AlertStatus;
import com.fincom.sanction.domain.alert.CreateAlertRequest;
import com.fincom.sanction.domain.alert.EscalateAlertRequest;
import com.fincom.sanction.domain.alert.UpdateAlertDecisionRequest;

import java.util.List;

public interface AlertsService {
	Alert createAlert(CreateAlertRequest request);
	List<Alert> getAlertsByFilter(String tenantId, AlertStatus status, Float minScore);
	Alert updateAlertDecision(UpdateAlertDecisionRequest request);
	Alert escalateAlert(EscalateAlertRequest request);
}
