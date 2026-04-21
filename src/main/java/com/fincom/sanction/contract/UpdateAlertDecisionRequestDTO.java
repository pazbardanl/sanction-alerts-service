package com.fincom.sanction.contract;

import java.util.UUID;
// TODO: see if we can refrain from depending on domain objects in the contract layer
import com.fincom.sanction.domain.AlertStatus;

public record UpdateAlertDecisionRequestDTO(
    UUID alertId,
    String tenantId,
    AlertStatus statusDecision,
    String decisionNote
) {

}
