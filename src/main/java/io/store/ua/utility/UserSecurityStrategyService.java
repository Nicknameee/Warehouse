package io.store.ua.utility;

public class UserSecurityStrategyService {
    public static final String USER_AUTHENTICATION_TYPE = "Bearer";

    static class CustomClaims {
        public static final String IP = "IP";
        public static final String USER_AGENT = "User-Agent";
    }
}
