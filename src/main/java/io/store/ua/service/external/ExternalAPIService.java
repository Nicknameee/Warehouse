package io.store.ua.service.external;

import java.util.concurrent.atomic.AtomicBoolean;

public interface ExternalAPIService {
    AtomicBoolean IS_HEALTHY = new AtomicBoolean(true);

    default boolean isHealthy() {
        return IS_HEALTHY.get();
    }

    default void setHealth(boolean flag) {
        IS_HEALTHY.set(flag);
    }
}
