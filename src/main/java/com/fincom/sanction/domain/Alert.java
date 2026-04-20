package com.fincom.sanction.domain;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;

public record Alert(
    UUID id, 
    String transactionId, 
    String matchedEntityName, 
    float matchScore, 
    AlertStatus status,
    String assignedTo,
    String tenantId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String decisionNote
) {
    @Override
    public String toString() {
        return "Alert(id=" + id + ", transactionId=" + transactionId + ", matchedEntityName=" + matchedEntityName + ", matchScore=" + matchScore + ", status=" + status + ", assignedTo=" + assignedTo + ", tenantId=" + tenantId + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + ", decisionNote=" + decisionNote + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Alert alert = (Alert) o;
        return Float.compare(alert.matchScore, matchScore) == 0 && Objects.equals(id, alert.id) && Objects.equals(transactionId, alert.transactionId) && Objects.equals(matchedEntityName, alert.matchedEntityName) && status == alert.status && Objects.equals(assignedTo, alert.assignedTo) && Objects.equals(tenantId, alert.tenantId) && Objects.equals(createdAt, alert.createdAt) && Objects.equals(updatedAt, alert.updatedAt) && Objects.equals(decisionNote, alert.decisionNote);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, transactionId, matchedEntityName, matchScore, status, assignedTo, tenantId, createdAt, updatedAt, decisionNote);
    }

    public static Builder builderFrom(Alert base) {
        return new Builder(base);
    }

    public static final class Builder {
        private UUID id;
        private String transactionId;
        private String matchedEntityName;
        private float matchScore;
        private AlertStatus status;
        private String assignedTo;
        private String tenantId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String decisionNote;

        private Builder(Alert base) {
            this.id = base.id();
            this.transactionId = base.transactionId();
            this.matchedEntityName = base.matchedEntityName();
            this.matchScore = base.matchScore();
            this.status = base.status();
            this.assignedTo = base.assignedTo();
            this.tenantId = base.tenantId();
            this.createdAt = base.createdAt();
            this.updatedAt = base.updatedAt();
            this.decisionNote = base.decisionNote();
        }

        public Builder status(AlertStatus status) {
            this.status = status;
            return this;
        }

        public Builder decisionNote(String decisionNote) {
            this.decisionNote = decisionNote;
            return this;
        }

        public Builder assignedTo(String assignedTo) {
            this.assignedTo = assignedTo;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Alert build() {
            return new Alert(
                    id,
                    transactionId,
                    matchedEntityName,
                    matchScore,
                    status,
                    assignedTo,
                    tenantId,
                    createdAt,
                    updatedAt,
                    decisionNote);
        }
    }
}
