package io.store.ua.enums;

public enum UserRole {
    OPERATOR,
    MANAGER,
    OWNER;

    public static final String ROLE_PATTERN = "OPERATOR|MANAGER";
}
