package com.fincom.sanction.domain.event;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import com.fincom.sanction.domain.alert.AlertStatus;

// TODO: this might be unused
public record PublishEventRequestDTO(
    UUID alertId,
    String tenantId,
    EventType eventType,
    PublishMethod publishMethod,
    AlertStatus status,
    LocalDateTime timestamp) {

    @Override
    public String toString() {
        return "PublishEventRequestDTO{" +
                "alertId=" + alertId +
                ", tenantId='" + tenantId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", publishMethod='" + publishMethod + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublishEventRequestDTO that = (PublishEventRequestDTO) o;
        return Objects.equals(alertId, that.alertId) && Objects.equals(tenantId, that.tenantId) && Objects.equals(eventType, that.eventType) && Objects.equals(publishMethod, that.publishMethod) && Objects.equals(status, that.status) && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alertId, tenantId, eventType, publishMethod, status, timestamp);
    }
}
