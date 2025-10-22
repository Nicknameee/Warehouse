package io.store.ua.enums;

public enum Role {
    OPERATOR,
    SHIPPER,
    VENDOR,
    MANAGER,
    DEV_OPS,
    OWNER;

    public static final String ROLE_PATTERN = "OPERATOR|SHIPPER|VENDOR|MANAGER|DEV_OPS";
}
