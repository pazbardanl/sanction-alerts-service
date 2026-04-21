package com.fincom.sanction.domain;

import java.util.UUID;

public record UpdateAlertDecisionRequest(
    UUID alertId,
    String tenantId,
    AlertStatus statusDecision,
    String decisionNote
) {

}
