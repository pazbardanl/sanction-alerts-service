package com.fincom.sanction.controller;

import com.fincom.sanction.contract.AlertDTO;
import com.fincom.sanction.contract.CreateAlertRequestDTO;
import com.fincom.sanction.domain.Alert;
import com.fincom.sanction.mapper.SanctionAlertsMapper;
import com.fincom.sanction.service.AlertsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sanctions")
public class SanctionsController {

	private static final Logger log = LoggerFactory.getLogger(SanctionsController.class);

	private final SanctionAlertsMapper createAlertRequestMapper;
	private final AlertsService alertsService;

	public SanctionsController(SanctionAlertsMapper createAlertRequestMapper, AlertsService alertsService) {
		this.createAlertRequestMapper = createAlertRequestMapper;
		this.alertsService = alertsService;
	}

	@PostMapping("/alerts")
	@ResponseStatus(HttpStatus.CREATED)
	public AlertDTO createAlert(@Valid @RequestBody CreateAlertRequestDTO requestDto) {
		log.debug("createAlert: request={}", requestDto);
		Alert alert = alertsService.createAlert(createAlertRequestMapper.toDomain(requestDto));
        log.debug("createAlert: alert stored={}", alert);
		AlertDTO alertDTO = createAlertRequestMapper.toDTO(alert);
        log.debug("createAlert: alertDTO created={}", alertDTO);
        return alertDTO;
	}
}
