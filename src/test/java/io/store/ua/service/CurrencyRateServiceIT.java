package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.cache.CurrencyRate;
import io.store.ua.enums.Currency;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrencyRateServiceIT extends AbstractIT {
    @Autowired
    private CurrencyRateService currencyRateService;

    private void upsertRates(BigDecimal usdRate, BigDecimal eurRate, BigDecimal chfRate, BigDecimal uahRate) {
        currencyRateRepository.deleteAll();
        currencyRateRepository.saveAll(List.of(CurrencyRate.builder()
                        .currencyCode(Currency.USD.name())
                        .baseCurrencyCode(Currency.USD.name())
                        .rate(usdRate)
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build(),
                CurrencyRate.builder()
                        .currencyCode(Currency.EUR.name())
                        .baseCurrencyCode(Currency.USD.name())
                        .rate(eurRate)
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build(),
                CurrencyRate.builder()
                        .currencyCode(Currency.CHF.name())
                        .baseCurrencyCode(Currency.USD.name())
                        .rate(chfRate)
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build(),
                CurrencyRate.builder()
                        .currencyCode(Currency.UAH.name())
                        .baseCurrencyCode(Currency.USD.name())
                        .rate(uahRate)
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build()
        ));
    }

    private BigInteger expectedCents(String base, String target, BigInteger amountCents) {
        BigDecimal baseRate = currencyRateRepository.findById(base).orElseThrow().getRate();
        BigDecimal targetRate = currencyRateRepository.findById(target).orElseThrow().getRate();

        return new BigDecimal(amountCents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .multiply(targetRate)
                .divide(baseRate, 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toBigIntegerExact();
    }

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {
        @Test
        @DisplayName("findAll_success_returnsAllPersistedRates")
        void findAll_success_returnsAllPersistedRates() {
            upsertRates(
                    new BigDecimal("1.0000"),
                    new BigDecimal("0.9300"),
                    new BigDecimal("0.9800"),
                    new BigDecimal("41.0000")
            );

            List<CurrencyRate> currencyRates = currencyRateService.findAll();

            assertThat(currencyRates)
                    .hasSize(4);
            assertThat(currencyRates)
                    .extracting(CurrencyRate::getCurrencyCode)
                    .containsExactlyInAnyOrder(
                            Currency.USD.name(),
                            Currency.EUR.name(),
                            Currency.CHF.name(),
                            Currency.UAH.name()
                    );
        }

        @Test
        @DisplayName("findAll_success_returnsEmptyListWhenNoRates")
        void findAll_success_returnsEmptyListWhenNoRates() {
            currencyRateRepository.deleteAll();

            List<CurrencyRate> currencyRates = currencyRateService.findAll();

            assertThat(currencyRates)
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("convert(base: String, target: String, amount: BigInteger)")
    class ConvertTests {
        @Test
        @DisplayName("convert_success_whenUsdToEurExactCents")
        void convert_success_whenUsdToEurExactCents() {
            upsertRates(new BigDecimal("1.0000"),
                    new BigDecimal("0.9300"),
                    new BigDecimal("0.9800"),
                    new BigDecimal("41.0000"));

            BigInteger amount = new BigInteger("12345");
            BigInteger actual = currencyRateService.convert("USD", "EUR", amount);
            BigInteger expected = expectedCents("USD", "EUR", amount);

            assertEquals(expected, actual);
        }

        @Test
        @DisplayName("convert_success_whenEurToChfFractionBoundary")
        void convert_success_whenEurToChfFractionBoundary() {
            upsertRates(new BigDecimal("1.0000"),
                    new BigDecimal("0.9550"),
                    new BigDecimal("0.9555"),
                    new BigDecimal("40.5000"));

            BigInteger amount = new BigInteger("101");
            BigInteger actual = currencyRateService.convert("EUR", "CHF", amount);
            BigInteger expected = expectedCents("EUR", "CHF", amount);

            assertEquals(expected, actual);
        }

        @Test
        @DisplayName("convert_success_whenUahToUsdLargeAmount")
        void convert_success_whenUahToUsdLargeAmount() {
            upsertRates(new BigDecimal("1.0000"),
                    new BigDecimal("0.9200"),
                    new BigDecimal("0.9800"),
                    new BigDecimal("41.2500"));

            BigInteger amount = new BigInteger("9876543210");
            BigInteger actual = currencyRateService.convert("UAH", "USD", amount);
            BigInteger expected = expectedCents("UAH", "USD", amount);

            assertEquals(expected, actual);
        }

        @Test
        @DisplayName("convert_success_whenSameCurrencyReturnsSameAmount")
        void convert_success_whenSameCurrencyReturnsSameAmount() {
            upsertRates(new BigDecimal("1.0000"),
                    new BigDecimal("0.9300"),
                    new BigDecimal("0.9800"),
                    new BigDecimal("41.0000"));

            BigInteger amount = new BigInteger("777");
            BigInteger actual = currencyRateService.convert("USD", "USD", amount);

            assertEquals(amount, actual);
        }

        @Test
        @DisplayName("convert_success_whenHalfUpBoundaryReached")
        void convert_success_whenHalfUpBoundaryReached() {
            upsertRates(new BigDecimal("1.0000"),
                    new BigDecimal("0.9950"),
                    new BigDecimal("0.9900"),
                    new BigDecimal("40.0000"));

            BigInteger amount = new BigInteger("105");
            BigInteger actual = currencyRateService.convert("USD", "EUR", amount);
            BigInteger expected = expectedCents("USD", "EUR", amount);

            assertEquals(expected, actual);
        }

        @Test
        @DisplayName("convert_success_whenVerySmallAmountRoundsToZero")
        void convert_success_whenVerySmallAmountRoundsToZero() {
            upsertRates(new BigDecimal("1.0000"),
                    new BigDecimal("100.0000"),
                    new BigDecimal("120.0000"),
                    new BigDecimal("41.0000"));

            BigInteger amount = BigInteger.ONE;
            BigInteger actual = currencyRateService.convert("USD", "EUR", amount);

            assertEquals(expectedCents("USD", "EUR", amount), actual);
        }

        @Test
        @DisplayName("convert_fail_whenBaseCurrencyBlank")
        void convert_fail_whenBaseCurrencyBlank() {
            assertThatThrownBy(() -> currencyRateService.convert(" ", "USD", BigInteger.ONE))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        @DisplayName("convert_fail_whenTargetCurrencyBlank")
        void convert_fail_whenTargetCurrencyBlank() {
            assertThatThrownBy(() -> currencyRateService.convert("USD", "", BigInteger.ONE))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        @DisplayName("convert_fail_whenAmountNull")
        void convert_fail_whenAmountNull() {
            assertThatThrownBy(() -> currencyRateService.convert("USD", "EUR", null))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        @DisplayName("convert_fail_whenAmountLessThanOne")
        void convert_fail_whenAmountLessThanOne() {
            assertThatThrownBy(() -> currencyRateService.convert("USD", "EUR", BigInteger.ZERO))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        @DisplayName("convert_fail_whenBaseCurrencyNotFound")
        void convert_fail_whenBaseCurrencyNotFound() {
            upsertRates(new BigDecimal("1.0000"),
                    new BigDecimal("0.9300"),
                    new BigDecimal("0.9800"),
                    new BigDecimal("41.0000"));

            assertThatThrownBy(() -> currencyRateService.convert("ZZZ", "USD", BigInteger.TEN))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("convert_fail_whenTargetCurrencyNotFound")
        void convert_fail_whenTargetCurrencyNotFound() {
            upsertRates(new BigDecimal("1.0000"),
                    new BigDecimal("0.9300"),
                    new BigDecimal("0.9800"),
                    new BigDecimal("41.0000"));

            assertThatThrownBy(() -> currencyRateService.convert("USD", "ZZZ", BigInteger.TEN))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("convertFromCentsToCurrencyUnit(base: String, target: String, amount: BigInteger)")
    class ConvertFromCentsToCurrencyUnitTests {
        @Test
        @DisplayName("convertFromCentsToCurrencyUnit_success_whenMatchesConvertThenMoveLeft")
        void convertFromCentsToCurrencyUnit_success_whenMatchesConvertThenMoveLeft() {
            upsertRates(new BigDecimal("1.0000"),
                    new BigDecimal("0.9400"),
                    new BigDecimal("0.9600"),
                    new BigDecimal("40.7500"));

            BigInteger amount = new BigInteger("8888");
            BigInteger converted = currencyRateService.convert("USD", "EUR", amount);
            BigDecimal expected = new BigDecimal(converted).movePointLeft(2);
            BigDecimal actual = currencyRateService.convertFromCentsToCurrencyUnit("USD", "EUR", amount);

            assertEquals(0, expected.compareTo(actual));
        }

        @Test
        @DisplayName("convertFromCentsToCurrencyUnit_success_whenSameCurrencyDividesBy100")
        void convertFromCentsToCurrencyUnit_success_whenSameCurrencyDividesBy100() {
            upsertRates(new BigDecimal("1.0000"),
                    new BigDecimal("0.9400"),
                    new BigDecimal("0.9600"),
                    new BigDecimal("40.7500"));
            BigInteger amount = new BigInteger("901");

            BigDecimal actual = currencyRateService.convertFromCentsToCurrencyUnit("USD", "USD", amount);

            assertEquals(0, actual.compareTo(new BigDecimal("9.01")));
        }

        @Test
        @DisplayName("convertFromCentsToCurrencyUnit_fail_whenInvalidArgs")
        void convertFromCentsToCurrencyUnit_fail_whenInvalidArgs() {
            assertThatThrownBy(() -> currencyRateService.convertFromCentsToCurrencyUnit(" ", "USD", BigInteger.ONE))
                    .isInstanceOf(ConstraintViolationException.class);
            assertThatThrownBy(() -> currencyRateService.convertFromCentsToCurrencyUnit("USD", "", BigInteger.ONE))
                    .isInstanceOf(ConstraintViolationException.class);
            assertThatThrownBy(() -> currencyRateService.convertFromCentsToCurrencyUnit("USD", "EUR", null))
                    .isInstanceOf(ConstraintViolationException.class);
            assertThatThrownBy(() -> currencyRateService.convertFromCentsToCurrencyUnit("USD", "EUR", BigInteger.ZERO))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }
}
