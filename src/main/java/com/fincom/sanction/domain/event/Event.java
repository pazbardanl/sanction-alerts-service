package com.fincom.sanction.domain.event;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import com.fincom.sanction.domain.alert.AlertStatus;

public record Event(UUID alertId, String tenantId, String eventType, AlertStatus status, LocalDateTime timestamp) {

    @Override
    public String toString() {
        return "Event{" +
                "alertId=" + alertId +
                ", tenantId='" + tenantId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", status=" + status +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(alertId, event.alertId) && Objects.equals(tenantId, event.tenantId) && Objects.equals(eventType, event.eventType) && status == event.status && Objects.equals(timestamp, event.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alertId, tenantId, eventType, status, timestamp);
    }
}
