package com.fincom.sanction.domain.event;

public enum EventType {
    ALERT_DECIDED("alert.decided"),
    ALERT_ESCALATED("alert.escalated");

    private final String name;

    EventType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return "EventType{" +
                "name='" + name + '\'' +
                '}';
    }
}
