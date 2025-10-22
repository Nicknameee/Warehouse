package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.cache.CurrencyRate;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.service.external.OpenExchangeRateAPIService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.store.ua.service.external.DataTransAPIService.Constants;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CurrencyRateServiceIT extends AbstractIT {
    @Autowired
    private CurrencyRateService service;
    @MockitoBean
    private OpenExchangeRateAPIService openExchangeRateAPIService;

    @Test
    void convert_success() {
        BigDecimal rate = new BigDecimal("0.95");
        BigDecimal targetRate = new BigDecimal("1");
        currencyRateRepository.saveAll(List.of(
                CurrencyRate.builder()
                        .currencyCode(Constants.Currency.EUR)
                        .baseCurrencyCode(Constants.Currency.USD)
                        .rate(rate)
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build(),
                CurrencyRate.builder()
                        .currencyCode(Constants.Currency.USD)
                        .baseCurrencyCode(Constants.Currency.USD)
                        .rate(targetRate)
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build()
        ));

        BigDecimal initial = new BigDecimal("100");
        BigDecimal out = service.convert(Constants.Currency.EUR, Constants.Currency.USD, initial);

        assertEquals(0, out.compareTo(initial.multiply(rate).divide(targetRate, 2, RoundingMode.HALF_EVEN)));
    }

    @Test
    void convert_success_whenInitialCurrencyEqualsTargetCurrency() {
        currencyRateRepository.saveAll(List.of(
                CurrencyRate.builder()
                        .currencyCode(Constants.Currency.USD)
                        .baseCurrencyCode(Constants.Currency.USD)
                        .rate(new BigDecimal("1"))
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build()
        ));

        BigDecimal initial = new BigDecimal("100.333");
        BigDecimal out = service.convert(Constants.Currency.USD, Constants.Currency.USD, initial);

        assertEquals(initial.setScale(2, RoundingMode.HALF_EVEN), out);
    }

    @Test
    void convert_fail_whenBaseCurrencyIsNotFound() {
        assertThrows(
                NotFoundException.class,
                () -> service.convert(Constants.Currency.USD, Constants.Currency.CHF, new BigDecimal("10"))
        );
    }

    @Test
    void convert_fail_whenTargetCurrencyIsNotFound() {
        currencyRateRepository.saveAll(List.of(
                CurrencyRate.builder()
                        .currencyCode(Constants.Currency.USD)
                        .baseCurrencyCode(Constants.Currency.USD)
                        .rate(new BigDecimal("1"))
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build()
        ));

        assertThrows(NotFoundException.class, () -> service.convert(Constants.Currency.USD, Constants.Currency.EUR, new BigDecimal("10")));
    }

    @Test
    void refreshCurrencyRates_replacesAllWithFreshRates() {
        currencyRateRepository.saveAll(List.of(
                CurrencyRate.builder()
                        .currencyCode(Constants.Currency.USD)
                        .baseCurrencyCode(Constants.Currency.USD)
                        .rate(new BigDecimal("1.00"))
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build(),
                CurrencyRate.builder()
                        .currencyCode(Constants.Currency.CHF)
                        .baseCurrencyCode(Constants.Currency.USD)
                        .rate(new BigDecimal("1.25"))
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build(),
                CurrencyRate.builder()
                        .currencyCode(Constants.Currency.EUR)
                        .baseCurrencyCode(Constants.Currency.USD)
                        .rate(new BigDecimal("0.91"))
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build()
        ));

        assertEquals(3, currencyRateRepository.count());

        var usdRate =
                CurrencyRate.builder()
                        .currencyCode(Constants.Currency.USD)
                        .baseCurrencyCode(Constants.Currency.USD)
                        .rate(new BigDecimal("1.00"))
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build();
        var chfRate =
                CurrencyRate.builder()
                        .currencyCode(Constants.Currency.CHF)
                        .baseCurrencyCode(Constants.Currency.USD)
                        .rate(new BigDecimal("1.25"))
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build();
        var eurRate =
                CurrencyRate.builder()
                        .currencyCode(Constants.Currency.EUR)
                        .baseCurrencyCode(Constants.Currency.USD)
                        .rate(new BigDecimal("0.91"))
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build();

        List<CurrencyRate> fresh = List.of(usdRate, chfRate, eurRate);

        when(openExchangeRateAPIService.refreshCurrencyRates()).thenReturn(fresh);

        service.refreshCurrencyRates();

        assertEquals(3, currencyRateRepository.count());

        var usd = currencyRateRepository.findById(Constants.Currency.USD).orElseThrow();
        var chf = currencyRateRepository.findById(Constants.Currency.CHF).orElseThrow();
        var eur = currencyRateRepository.findById(Constants.Currency.EUR).orElseThrow();

        assertEquals(usdRate.getCurrencyCode(), usd.getCurrencyCode());
        assertEquals(usdRate.getBaseCurrencyCode(), usd.getBaseCurrencyCode());
        assertEquals(0, usdRate.getRate().compareTo(usd.getRate()));
        assertEquals(usdRate.getExpiryTime(), usd.getExpiryTime());

        assertEquals(chfRate.getCurrencyCode(), chf.getCurrencyCode());
        assertEquals(chfRate.getBaseCurrencyCode(), chf.getBaseCurrencyCode());
        assertEquals(0, chfRate.getRate().compareTo(chf.getRate()));
        assertEquals(chfRate.getExpiryTime(), chf.getExpiryTime());

        assertEquals(eurRate.getCurrencyCode(), eur.getCurrencyCode());
        assertEquals(eurRate.getBaseCurrencyCode(), eur.getBaseCurrencyCode());
        assertEquals(0, eurRate.getRate().compareTo(eur.getRate()));
        assertEquals(eurRate.getExpiryTime(), eur.getExpiryTime());

        verify(openExchangeRateAPIService, times(1)).refreshCurrencyRates();
    }
}
