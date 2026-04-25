package com.fincom.sanction.contract;

import com.fincom.sanction.domain.alert.AlertStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateAlertDecisionRequestDTO(
        @NotBlank String tenantId,
        @NotNull AlertStatus statusDecision,
        String decisionNote) {}
