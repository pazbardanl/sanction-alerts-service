package com.fincom.sanction.contract;

// TODO: see if we can refrain from depending on domain objects in the contract layer
import com.fincom.sanction.domain.AlertStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateAlertDecisionRequestDTO(
        @NotBlank String tenantId,
        @NotNull AlertStatus statusDecision,
        String decisionNote) {}
