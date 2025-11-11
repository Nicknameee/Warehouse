package io.store.ua.controllers;

import io.store.ua.service.CurrencyRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;

@RestController
@RequestMapping("/api/v1/currencyRates")
@RequiredArgsConstructor
public class CurrencyRateController {
    private final CurrencyRateService currencyRateService;

    @GetMapping("/findAll")
    public ResponseEntity<?> findAll() {
        return ResponseEntity.ok(currencyRateService.findAll());
    }

    @GetMapping("/findBy/currency")
    public ResponseEntity<?> findByCurrency(@RequestParam("currency") String currency) {
        return ResponseEntity.ok(currencyRateService.findByCurrency(currency));
    }

    @GetMapping("/convert")
    public ResponseEntity<?> convert(@RequestParam("baseCurrency") String baseCurrency,
                                     @RequestParam("targetCurrency") String targetCurrency,
                                     @RequestParam("amount") BigInteger amount) {
        return ResponseEntity.ok(currencyRateService.convert(baseCurrency, targetCurrency, amount));
    }

    @GetMapping("/convertFromCentsToCurrencyUnit")
    public ResponseEntity<?> convertFromCentsToCurrencyUnit(@RequestParam("baseCurrency") String baseCurrency,
                                                            @RequestParam("targetCurrency") String targetCurrency,
                                                            @RequestParam("amount") BigInteger amount) {
        return ResponseEntity.ok(currencyRateService.convertFromCentsToCurrencyUnit(baseCurrency, targetCurrency, amount));
    }
}
