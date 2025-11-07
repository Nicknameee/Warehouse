package io.store.ua.enums;

import java.util.Map;
import java.util.Set;

public enum ShipmentStatus {
    PLANNED,
    INITIATED,
    SENT,
    DELIVERED,
    ROLLBACK;

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> transitions = Map.of(
            PLANNED, Set.of(INITIATED, SENT, ROLLBACK),
            INITIATED, Set.of(SENT, DELIVERED, ROLLBACK),
            SENT, Set.of(DELIVERED, ROLLBACK),
            DELIVERED, Set.of(ROLLBACK),
            ROLLBACK, Set.of());

    public static boolean canTransitionTo(ShipmentStatus current, ShipmentStatus target) {
        return transitions.getOrDefault(current, Set.of()).contains(target);
    }
}