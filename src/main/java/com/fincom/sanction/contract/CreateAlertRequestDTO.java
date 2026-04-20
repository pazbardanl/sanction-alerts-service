package com.fincom.sanction.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

public record CreateAlertRequestDTO(
    @NotBlank String transactionId,
    @NotBlank String matchedEntityName,
    @NotNull Float matchScore,
    @NotBlank String tenantId) {
    @Override
    public String toString() {
        return "CreateAlertRequestDTO{" +
                "transactionId='" + transactionId + '\'' +
                ", matchedEntityName='" + matchedEntityName + '\'' +
                ", matchScore=" + matchScore +
                ", tenantId='" + tenantId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateAlertRequestDTO that = (CreateAlertRequestDTO) o;
        return Objects.equals(transactionId, that.transactionId)
                && Objects.equals(matchedEntityName, that.matchedEntityName)
                && Objects.equals(matchScore, that.matchScore)
                && Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, matchedEntityName, matchScore, tenantId);
    }
}
