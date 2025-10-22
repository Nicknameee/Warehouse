package io.store.ua.service.external;

public interface ExternalAPIService {
    default boolean isHealthy() {
        return true;
    }
}
