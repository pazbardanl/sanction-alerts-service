package com.fincom.sanction.contract;

import com.fincom.sanction.domain.AlertStatus;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record AlertDTO(
    UUID id,
    String transactionId,
    String matchedEntityName,
    float matchScore,
    AlertStatus status,
    String assignedTo,
    String tenantId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String decisionNote) {

        @Override
        public String toString() {
            return "AlertDTO(id=" + id + ", transactionId=" + transactionId + ", matchedEntityName=" + matchedEntityName + ", matchScore=" + matchScore + ", status=" + status + ", assignedTo=" + assignedTo + ", tenantId=" + tenantId + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + ", decisionNote=" + decisionNote + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AlertDTO alertDTO = (AlertDTO) o;
            return Float.compare(alertDTO.matchScore, matchScore) == 0 && Objects.equals(id, alertDTO.id) && Objects.equals(transactionId, alertDTO.transactionId) && Objects.equals(matchedEntityName, alertDTO.matchedEntityName) && status == alertDTO.status && Objects.equals(assignedTo, alertDTO.assignedTo) && Objects.equals(tenantId, alertDTO.tenantId) && Objects.equals(createdAt, alertDTO.createdAt) && Objects.equals(updatedAt, alertDTO.updatedAt) && Objects.equals(decisionNote, alertDTO.decisionNote);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, transactionId, matchedEntityName, matchScore, status, assignedTo, tenantId, createdAt, updatedAt, decisionNote);
        }   
}
