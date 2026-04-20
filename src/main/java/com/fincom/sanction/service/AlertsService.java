package com.fincom.sanction.service;

import com.fincom.sanction.domain.Alert;
import com.fincom.sanction.domain.CreateAlertRequest;

public interface AlertsService {

	Alert createAlert(CreateAlertRequest request);

}
