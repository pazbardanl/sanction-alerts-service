package com.fincom.sanction.service;

import com.fincom.sanction.domain.Alert;
import com.fincom.sanction.domain.AlertStatus;
import com.fincom.sanction.domain.CreateAlertRequest;
import java.util.List;

public interface AlertsService {

	Alert createAlert(CreateAlertRequest request);

	List<Alert> getAlertsByFilter(String tenantId, AlertStatus status, Float minScore);
}
