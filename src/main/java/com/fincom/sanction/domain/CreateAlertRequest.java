package com.fincom.sanction.domain;

import java.util.Objects;

public record CreateAlertRequest(String transactionId, String matchedEntityName, float matchScore, String tenantId) {
    @Override
    public String toString() {
        return "CreateAlertRequest{" +
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
        CreateAlertRequest that = (CreateAlertRequest) o;
        return Float.compare(that.matchScore, matchScore) == 0 && Objects.equals(transactionId, that.transactionId) && Objects.equals(matchedEntityName, that.matchedEntityName) && Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, matchedEntityName, matchScore, tenantId);
    }
}
