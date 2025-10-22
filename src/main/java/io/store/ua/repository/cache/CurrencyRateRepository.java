package io.store.ua.repository.cache;

import io.store.ua.entity.cache.CurrencyRate;
import org.springframework.context.annotation.Profile;
import org.springframework.data.keyvalue.repository.KeyValueRepository;

@Profile({"redis", "external"})
public interface CurrencyRateRepository extends KeyValueRepository<CurrencyRate, String> {
}
