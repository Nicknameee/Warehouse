package io.store.ua.repository.cache;

import io.store.ua.entity.cache.CurrencyRate;
import org.springframework.data.keyvalue.repository.KeyValueRepository;

public interface CurrencyRateRepository extends KeyValueRepository<CurrencyRate, String> {
}
