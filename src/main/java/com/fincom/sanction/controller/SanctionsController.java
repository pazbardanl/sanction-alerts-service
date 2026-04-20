package com.fincom.sanction.controller;

import com.fincom.sanction.contract.AlertDTO;
import com.fincom.sanction.contract.CreateAlertRequestDTO;
import com.fincom.sanction.domain.Alert;
import com.fincom.sanction.domain.AlertStatus;
import com.fincom.sanction.mapper.SanctionAlertsMapper;
import com.fincom.sanction.service.AlertsService;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

	@GetMapping("/alerts")
	public List<AlertDTO> getAlertsByFilter(
			@RequestParam String tenantId,
			@RequestParam(required = false) AlertStatus status,
			@RequestParam(required = false) Float minScore) {
		log.debug("getAlertsByFilter: tenantId={}, status={}, minScore={}", tenantId, status, minScore);
		List<Alert> alerts = alertsService.getAlertsByFilter(tenantId, status, minScore);
		log.debug("getAlertsByFilter: alerts found={}", alerts.size());
		return createAlertRequestMapper.toDTO(alerts);
	}
}
