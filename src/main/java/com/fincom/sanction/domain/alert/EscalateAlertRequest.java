package com.fincom.sanction.domain.alert;

import java.util.Objects;
import java.util.UUID;

public record EscalateAlertRequest(
    UUID alertId,
    String tenantId,
    String assignedTo
) {
    @Override
    public String toString() {
        return "EscalateAlertRequest{" +
                "alertId=" + alertId +
                "tenantId='" + tenantId + '\'' +
                ", assignedTo='" + assignedTo + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EscalateAlertRequest that = (EscalateAlertRequest) o;
        return Objects.equals(alertId, that.alertId) && Objects.equals(tenantId, that.tenantId) && Objects.equals(assignedTo, that.assignedTo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alertId, tenantId, assignedTo);
    }
}
