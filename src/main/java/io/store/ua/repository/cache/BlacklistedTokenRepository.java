package io.store.ua.repository.cache;

import io.store.ua.entity.cache.BlacklistedToken;
import org.springframework.context.annotation.Profile;
import org.springframework.data.keyvalue.repository.KeyValueRepository;

@Profile({"redis", "users"})
public interface BlacklistedTokenRepository extends KeyValueRepository<BlacklistedToken, String> {
}
