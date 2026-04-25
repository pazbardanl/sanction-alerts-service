package com.fincom.sanction.contract;

import jakarta.validation.constraints.NotBlank;
import java.util.Objects;

public record EscalateAlertRequestDTO(
    @NotBlank String tenantId,
    @NotBlank String assignedTo
) {
    @Override
    public String toString() {
        return "EscalateAlertRequestDTO{" +
                "tenantId='" + tenantId + '\'' +
                ", assignedTo='" + assignedTo + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EscalateAlertRequestDTO that = (EscalateAlertRequestDTO) o;
        return Objects.equals(tenantId, that.tenantId) && Objects.equals(assignedTo, that.assignedTo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, assignedTo);
    }
}
