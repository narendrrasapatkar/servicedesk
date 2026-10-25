package com.servicedesk.model;

public enum TicketStatus {
    NEW,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED;

    public boolean canTransitionTo(TicketStatus target) {
        if (this == target) return true;
        return switch (this) {
            case NEW -> target == ASSIGNED || target == CANCELLED;
            case ASSIGNED -> target == IN_PROGRESS || target == CANCELLED;
            case IN_PROGRESS -> target == RESOLVED || target == CANCELLED;
            case RESOLVED -> target == CLOSED || target == IN_PROGRESS;
            case CLOSED, CANCELLED -> false; // Terminal states
        };
    }
}