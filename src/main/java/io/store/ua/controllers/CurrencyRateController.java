package io.store.ua.controllers;

import io.store.ua.entity.cache.CurrencyRate;
import io.store.ua.service.CurrencyRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/currencyRates")
@RequiredArgsConstructor
public class CurrencyRateController {
    private final CurrencyRateService currencyRateService;

    @GetMapping("/findAll")
    public ResponseEntity<List<CurrencyRate>> findAll() {
        return ResponseEntity.ok(currencyRateService.findAll());
    }
}
