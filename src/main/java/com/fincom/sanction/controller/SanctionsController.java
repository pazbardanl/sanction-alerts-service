package com.fincom.sanction.controller;

import com.fincom.sanction.contract.AlertDTO;
import com.fincom.sanction.contract.CreateAlertRequestDTO;
import com.fincom.sanction.contract.EscalateAlertRequestDTO;
import com.fincom.sanction.contract.UpdateAlertDecisionRequestDTO;
import com.fincom.sanction.domain.Alert;
import com.fincom.sanction.domain.AlertStatus;
import com.fincom.sanction.mapper.SanctionAlertsMapper;
import com.fincom.sanction.service.AlertsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/sanctions")
public class SanctionsController {

	private static final Logger log = LoggerFactory.getLogger(SanctionsController.class);

	private final SanctionAlertsMapper mapper;
	private final AlertsService alertsService;

	public SanctionsController(SanctionAlertsMapper mapper, AlertsService alertsService) {
		this.mapper = mapper;
		this.alertsService = alertsService;
	}

	@PostMapping("/alerts")
	@ResponseStatus(HttpStatus.CREATED)
	public AlertDTO createAlert(@Valid @RequestBody CreateAlertRequestDTO requestDto) {
		log.debug("createAlert: request={}", requestDto);
		Alert alert = alertsService.createAlert(mapper.toDomain(requestDto));
        log.debug("createAlert: alert stored={}", alert);
		AlertDTO alertDTO = mapper.toDTO(alert);
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
		return mapper.toDTO(alerts);
	}

	@PatchMapping("/alerts/{id}/decision")
	public AlertDTO updateAlertDecision(
			@PathVariable("id") @NotNull UUID id, @Valid @RequestBody UpdateAlertDecisionRequestDTO requestDto) {
		log.debug("updateAlertDecision: id={}, request={}", id, requestDto);
		Alert updatedAlert =
				alertsService.updateAlertDecision(mapper.toDomain(id, requestDto));
		log.debug("updateAlertDecision: updatedAlert={}", updatedAlert);
		return mapper.toDTO(updatedAlert);
	}

	@PatchMapping("/alerts/{id}/escalate")
	public AlertDTO escalateAlert(
			@PathVariable("id") @NotNull UUID id, @Valid @RequestBody EscalateAlertRequestDTO requestDto) {
		log.debug("escalateAlert: id={}, request={}", id, requestDto);
		Alert updatedAlert = alertsService.escalateAlert(mapper.toDomain(id, requestDto));
		log.debug("escalateAlert: updatedAlert={}", updatedAlert);
		return mapper.toDTO(updatedAlert);
	}
}
