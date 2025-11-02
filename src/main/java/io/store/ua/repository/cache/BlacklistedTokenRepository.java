package io.store.ua.repository.cache;

import io.store.ua.entity.cache.BlacklistedToken;
import org.springframework.data.keyvalue.repository.KeyValueRepository;

public interface BlacklistedTokenRepository extends KeyValueRepository<BlacklistedToken, String> {
}
