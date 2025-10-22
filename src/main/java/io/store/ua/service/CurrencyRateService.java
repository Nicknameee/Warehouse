package io.store.ua.service;

import io.store.ua.entity.cache.CurrencyRate;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.repository.cache.CurrencyRateRepository;
import io.store.ua.service.external.OpenExchangeRateAPIService;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CurrencyRateService {
    private final CurrencyRateRepository currencyRateRepository;
    private final OpenExchangeRateAPIService openExchangeRateAPIService;

    @Retryable(
            maxAttempts = 10,
            backoff = @Backoff(delay = 10_000, multiplier = 1.0),
            retryFor = Exception.class
    )
    @Scheduled(fixedRate = 15, timeUnit = TimeUnit.MINUTES, initialDelay = 0)
    @Transactional
    public void refreshCurrencyRates() {
        var freshRates = openExchangeRateAPIService.refreshCurrencyRates();

        clearAll();
        saveAll(freshRates);
    }

    public List<CurrencyRate> findAll() {
        return currencyRateRepository.findAll();
    }

    public CurrencyRate findByCurrency(String currency) {
        return currencyRateRepository.findById(currency)
                .orElseThrow(() -> new NotFoundException("Currency rate for currency '%s' was not found".formatted(currency)));
    }

    public List<CurrencyRate> saveAll(List<CurrencyRate> currencyRates) {
        return currencyRateRepository.saveAll(currencyRates);
    }

    public void clearAll(List<CurrencyRate> currencyRates) {
        currencyRateRepository.deleteAll(currencyRates);
    }

    public void clearAll() {
        currencyRateRepository.deleteAll();
    }

    public BigDecimal convert(String baseCurrency, String targetCurrency, BigDecimal amount) {
        BigDecimal targetRate = currencyRateRepository.findById(targetCurrency)
                .map(CurrencyRate::getRate)
                .orElseThrow(() -> new NotFoundException("Unknown target currency: %s".formatted(targetCurrency)));

        BigDecimal baseRate = currencyRateRepository.findById(baseCurrency)
                .map(CurrencyRate::getRate)
                .orElseThrow(() -> new NotFoundException("Unknown base currency: %s".formatted(baseCurrency)));

        return amount.multiply(baseRate).divide(targetRate, 2, RoundingMode.HALF_EVEN);
    }
}
