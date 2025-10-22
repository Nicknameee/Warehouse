package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.RegularUser;
import io.store.ua.entity.cache.CurrencyRate;
import io.store.ua.enums.Role;
import io.store.ua.enums.Status;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.service.external.OpenExchangeRateAPIService;
import jakarta.validation.ValidationException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.store.ua.service.external.DataTransAPIService.Constants;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrencyRateServiceIT extends AbstractIT {
    @Autowired
    private CurrencyRateService currencyRateService;
    @MockitoBean
    private OpenExchangeRateAPIService openExchangeRateAPIService;

    @BeforeEach
    void setUp() {
        var user = RegularUser.builder()
                .username(RandomStringUtils.secure().nextAlphanumeric(333))
                .role(Role.OWNER)
                .status(Status.ACTIVE)
                .build();

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    @DisplayName("refreshCurrencyRates()")
    class RefreshCurrencyRatesTests {
        @Test
        @DisplayName("refreshCurrencyRates_success: replaces all with fresh rates and calls external API once")
        void refreshCurrencyRates_success() {
            currencyRateRepository.saveAll(List.of(
                    CurrencyRate.builder().currencyCode(Constants.Currency.USD).baseCurrencyCode(Constants.Currency.USD).rate(new BigDecimal("1.11")).expiryTime(TimeUnit.DAYS.toSeconds(1)).build(),
                    CurrencyRate.builder().currencyCode(Constants.Currency.EUR).baseCurrencyCode(Constants.Currency.USD).rate(new BigDecimal("0.88")).expiryTime(TimeUnit.DAYS.toSeconds(1)).build()
            ));

            CurrencyRate usdRate = CurrencyRate.builder().currencyCode(Constants.Currency.USD).baseCurrencyCode(Constants.Currency.USD).rate(new BigDecimal("1.00")).expiryTime(TimeUnit.DAYS.toSeconds(1)).build();
            CurrencyRate chfRate = CurrencyRate.builder().currencyCode(Constants.Currency.CHF).baseCurrencyCode(Constants.Currency.USD).rate(new BigDecimal("1.25")).expiryTime(TimeUnit.DAYS.toSeconds(1)).build();
            CurrencyRate eurRate = CurrencyRate.builder().currencyCode(Constants.Currency.EUR).baseCurrencyCode(Constants.Currency.USD).rate(new BigDecimal("0.91")).expiryTime(TimeUnit.DAYS.toSeconds(1)).build();

            when(openExchangeRateAPIService.refreshCurrencyRates()).thenReturn(List.of(usdRate, chfRate, eurRate));

            currencyRateService.refreshCurrencyRates();

            assertEquals(3, currencyRateRepository.count());
            CurrencyRate usd = currencyRateRepository.findById(Constants.Currency.USD).orElseThrow();
            CurrencyRate chf = currencyRateRepository.findById(Constants.Currency.CHF).orElseThrow();
            CurrencyRate eur = currencyRateRepository.findById(Constants.Currency.EUR).orElseThrow();

            assertEquals(0, usd.getRate().compareTo(usdRate.getRate()));
            assertEquals(0, chf.getRate().compareTo(chfRate.getRate()));
            assertEquals(0, eur.getRate().compareTo(eurRate.getRate()));
            verify(openExchangeRateAPIService, times(1)).refreshCurrencyRates();
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAllRatesTests {
        @Test
        @DisplayName("findAll_success: returns all saved rates")
        void findAll_success() {
            currencyRateRepository.saveAll(List.of(
                    CurrencyRate.builder().currencyCode(Constants.Currency.USD).baseCurrencyCode(Constants.Currency.USD).rate(new BigDecimal("1.00")).expiryTime(TimeUnit.DAYS.toSeconds(1)).build(),
                    CurrencyRate.builder().currencyCode(Constants.Currency.EUR).baseCurrencyCode(Constants.Currency.USD).rate(new BigDecimal("0.92")).expiryTime(TimeUnit.DAYS.toSeconds(1)).build()
            ));

            List<CurrencyRate> allRates = currencyRateService.findAll();

            assertThat(allRates).hasSize(2);
            assertThat(allRates).extracting(CurrencyRate::getCurrencyCode).containsExactlyInAnyOrder(Constants.Currency.USD, Constants.Currency.EUR);
        }
    }

    @Nested
    @DisplayName("saveAll(currencyRates: List<CurrencyRate>)")
    class SaveAllTests {
        @Test
        @DisplayName("saveAll_success: persists all provided rates")
        void saveAll_success() {
            List<CurrencyRate> input = List.of(
                    CurrencyRate.builder().currencyCode(Constants.Currency.USD).baseCurrencyCode(Constants.Currency.USD).rate(new BigDecimal("1.00")).expiryTime(TimeUnit.DAYS.toSeconds(1)).build(),
                    CurrencyRate.builder().currencyCode(Constants.Currency.EUR).baseCurrencyCode(Constants.Currency.USD).rate(new BigDecimal("0.90")).expiryTime(TimeUnit.DAYS.toSeconds(1)).build()
            );

            List<CurrencyRate> saved = currencyRateService.saveAll(input);

            assertThat(saved).hasSize(2);
            assertTrue(currencyRateRepository.findById(Constants.Currency.USD).isPresent());
            assertTrue(currencyRateRepository.findById(Constants.Currency.EUR).isPresent());
        }


        @Test
        @DisplayName("saveAll_fail_emptyList: triggers ValidationException for @NotEmpty")
        void saveAll_fail_nullList() {
            assertThrows(ValidationException.class, () -> currencyRateService.saveAll(null));
        }

        @Test
        @DisplayName("saveAll_fail_emptyList: triggers ValidationException for @NotEmpty")
        void saveAll_fail_emptyList() {
            assertThrows(ValidationException.class, () -> currencyRateService.saveAll(List.of()));
        }

        @Test
        @DisplayName("saveAll_fail_containsNull: triggers ValidationException for element @NotNull")
        void saveAll_fail_containsNull() {
            assertThrows(ValidationException.class, () -> currencyRateService.saveAll(new ArrayList<>() {{
                add(null);
                add(CurrencyRate.builder()
                        .currencyCode(Constants.Currency.USD)
                        .baseCurrencyCode(Constants.Currency.USD)
                        .rate(new BigDecimal("1.00"))
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build());
            }}));
        }
    }

    @Nested
    @DisplayName("findByCurrency(currency: String)")
    class FindByCurrencyTests {
        @Test
        @DisplayName("findByCurrency_success: returns rate when currency exists")
        void findByCurrency_success() {
            currencyRateRepository.save(CurrencyRate.builder()
                    .currencyCode(Constants.Currency.CHF)
                    .baseCurrencyCode(Constants.Currency.USD)
                    .rate(new BigDecimal("1.25"))
                    .expiryTime(TimeUnit.DAYS.toSeconds(1))
                    .build());

            CurrencyRate found = currencyRateService.findByCurrency(Constants.Currency.CHF);

            assertEquals(Constants.Currency.CHF, found.getCurrencyCode());
        }

        @Test
        @DisplayName("findByCurrency_fail_notFound: throws NotFoundException when rate missing")
        void findByCurrency_fail_notFound() {
            assertThrows(NotFoundException.class, () -> currencyRateService.findByCurrency(Constants.Currency.EUR));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("findByCurrency_fail_null: @NotBlank triggers ValidationException")
        void findByCurrency_fail_blankCurrency(String currency) {
            assertThrows(ValidationException.class, () -> currencyRateService.findByCurrency(currency));
        }
    }

    @Nested
    @DisplayName("clearAll(currencyRates: List<CurrencyRate>)")
    class ClearAllListValidationTests {
        @Test
        @DisplayName("clearAll_success_subset: deletes only provided rates")
        void clearAll_success_subset() {
            CurrencyRate usd = currencyRateRepository.save(CurrencyRate.builder()
                    .currencyCode(Constants.Currency.USD)
                    .baseCurrencyCode(Constants.Currency.USD)
                    .rate(new BigDecimal("1.00"))
                    .expiryTime(TimeUnit.DAYS.toSeconds(1))
                    .build());
            CurrencyRate eur = currencyRateRepository.save(CurrencyRate.builder()
                    .currencyCode(Constants.Currency.EUR)
                    .baseCurrencyCode(Constants.Currency.USD)
                    .rate(new BigDecimal("0.90"))
                    .expiryTime(TimeUnit.DAYS.toSeconds(1))
                    .build());
            CurrencyRate chf = currencyRateRepository.save(CurrencyRate.builder()
                    .currencyCode(Constants.Currency.CHF)
                    .baseCurrencyCode(Constants.Currency.USD)
                    .rate(new BigDecimal("1.25"))
                    .expiryTime(TimeUnit.DAYS.toSeconds(1))
                    .build());

            currencyRateService.clearAll(java.util.Arrays.asList(usd, eur));

            assertFalse(currencyRateRepository.findById(Constants.Currency.USD).isPresent());
            assertFalse(currencyRateRepository.findById(Constants.Currency.EUR).isPresent());
            assertTrue(currencyRateRepository.findById(Constants.Currency.CHF).isPresent());
        }

        @Test
        @DisplayName("clearAll_fail_nullList: @NotEmpty triggers ValidationException")
        void clearAll_fail_nullList() {
            assertThrows(ValidationException.class, () -> currencyRateService.clearAll(null));
        }

        @Test
        @DisplayName("clearAll_fail_emptyList: @NotEmpty triggers ValidationException")
        void clearAll_fail_emptyList() {
            assertThrows(ValidationException.class, () -> currencyRateService.clearAll(new java.util.ArrayList<>()));
        }

        @Test
        @DisplayName("clearAll_fail_containsNullElement: element @NotNull triggers ValidationException")
        void clearAll_fail_containsNullElement() {
            CurrencyRate validRate = CurrencyRate.builder()
                    .currencyCode(Constants.Currency.USD)
                    .baseCurrencyCode(Constants.Currency.USD)
                    .rate(new BigDecimal("1.00"))
                    .expiryTime(TimeUnit.DAYS.toSeconds(1))
                    .build();

            java.util.List<CurrencyRate> listWithNull = new java.util.ArrayList<>();
            listWithNull.add(validRate);
            listWithNull.add(null);

            assertThrows(ValidationException.class, () -> currencyRateService.clearAll(listWithNull));
        }
    }

    @Nested
    @DisplayName("clearAll()")
    class ClearAllTests {
        @Test
        @DisplayName("clearAll_success: removes all rates")
        void clearAll_success() {
            currencyRateRepository.saveAll(List.of(
                    CurrencyRate.builder()
                            .currencyCode(Constants.Currency.USD)
                            .baseCurrencyCode(Constants.Currency.USD)
                            .rate(new BigDecimal("1.00")).expiryTime(TimeUnit.DAYS.toSeconds(1))
                            .build(),
                    CurrencyRate.builder()
                            .currencyCode(Constants.Currency.EUR)
                            .baseCurrencyCode(Constants.Currency.USD)
                            .rate(new BigDecimal("0.90")).expiryTime(TimeUnit.DAYS.toSeconds(1))
                            .build()
            ));

            currencyRateService.clearAll();

            assertEquals(0, currencyRateRepository.count());
        }
    }

    @Nested
    @DisplayName("convert(base: String, target: String, amount: BigDecimal)")
    class ConvertTests {
        @Test
        @DisplayName("convert_success: computes amount * baseRate / targetRate with HALF_EVEN scale 2")
        void convert_success() {
            BigDecimal eurRate = new BigDecimal("0.95");
            BigDecimal usdRate = new BigDecimal("1.00");

            currencyRateRepository.saveAll(List.of(
                    CurrencyRate.builder()
                            .currencyCode(Constants.Currency.EUR)
                            .baseCurrencyCode(Constants.Currency.USD)
                            .rate(eurRate)
                            .expiryTime(TimeUnit.DAYS.toSeconds(1))
                            .build(),
                    CurrencyRate.builder()
                            .currencyCode(Constants.Currency.USD)
                            .baseCurrencyCode(Constants.Currency.USD)
                            .rate(usdRate)
                            .expiryTime(TimeUnit.DAYS.toSeconds(1))
                            .build()
            ));

            BigDecimal amount = new BigDecimal("100");
            BigDecimal converted = currencyRateService.convert(Constants.Currency.EUR, Constants.Currency.USD, amount);

            assertEquals(0, converted.compareTo(amount.multiply(eurRate).divide(usdRate, 2, RoundingMode.HALF_EVEN)));
        }

        @Test
        @DisplayName("convert_success_sameCurrency: returns amount scaled to 2")
        void convert_success_sameCurrency() {
            currencyRateRepository.save(CurrencyRate.builder()
                    .currencyCode(Constants.Currency.USD)
                    .baseCurrencyCode(Constants.Currency.USD)
                    .rate(new BigDecimal("1"))
                    .expiryTime(TimeUnit.DAYS.toSeconds(1))
                    .build());

            BigDecimal amount = new BigDecimal("100.333");
            BigDecimal converted = currencyRateService.convert(Constants.Currency.USD, Constants.Currency.USD, amount);

            assertEquals(amount.setScale(2, RoundingMode.HALF_EVEN), converted);
        }

        @Test
        @DisplayName("convert_fail_baseNotFound: throws NotFoundException")
        void convert_fail_baseNotFound() {
            assertThrows(NotFoundException.class, () -> currencyRateService.convert(Constants.Currency.USD, Constants.Currency.CHF, new BigDecimal("10")));
        }

        @Test
        @DisplayName("convert_fail_targetNotFound: throws NotFoundException")
        void convert_fail_targetNotFound() {
            currencyRateRepository.save(CurrencyRate.builder()
                    .currencyCode(Constants.Currency.USD)
                    .baseCurrencyCode(Constants.Currency.USD)
                    .rate(new BigDecimal("1"))
                    .expiryTime(TimeUnit.DAYS.toSeconds(1))
                    .build());

            assertThrows(NotFoundException.class, () -> currencyRateService.convert(Constants.Currency.USD, Constants.Currency.EUR, new BigDecimal("10")));
        }

        @Test
        @DisplayName("convert_fail_baseBlank: @NotBlank triggers ValidationException")
        void convert_fail_baseBlank() {
            assertThrows(ValidationException.class, () -> currencyRateService.convert("   ", Constants.Currency.USD, new BigDecimal("10")));
        }

        @Test
        @DisplayName("convert_fail_targetBlank: @NotBlank triggers ValidationException")
        void convert_fail_targetBlank() {
            assertThrows(ValidationException.class, () -> currencyRateService.convert(Constants.Currency.USD, "", new BigDecimal("10")));
        }

        @Test
        @DisplayName("convert_fail_baseNull: @NotBlank triggers ValidationException")
        void convert_fail_baseNull() {
            assertThrows(ValidationException.class, () -> currencyRateService.convert(null, Constants.Currency.USD, new BigDecimal("10")));
        }

        @Test
        @DisplayName("convert_fail_targetNull: @NotBlank triggers ValidationException")
        void convert_fail_targetNull() {
            assertThrows(ValidationException.class, () -> currencyRateService.convert(Constants.Currency.USD, null, new BigDecimal("10")));
        }

        @Test
        @DisplayName("convert_fail_amountNull: @NotNull triggers ValidationException")
        void convert_fail_amountNull() {
            assertThrows(ValidationException.class, () -> currencyRateService.convert(Constants.Currency.USD, Constants.Currency.EUR, null));
        }

        @Test
        @DisplayName("convert_fail_amountLessThanOne: @Min(1) triggers ValidationException")
        void convert_fail_amountLessThanOne() {
            assertThrows(ValidationException.class, () -> currencyRateService.convert(Constants.Currency.USD, Constants.Currency.EUR, new BigDecimal("0")));
        }
    }
}
