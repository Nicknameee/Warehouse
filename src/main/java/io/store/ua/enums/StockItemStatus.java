package io.store.ua.enums;

/**
 * Represents item state in the system
 */
public enum StockItemStatus {
    /**
     * All items shipped out
     */
    OUT_OF_STOCK,
    /**
     * Items available for shipment
     */
    AVAILABLE,
    /**
     * Item is off
     */
    OUT_OF_SERVICE
}