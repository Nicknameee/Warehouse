package io.store.ua.controllers;

import io.store.ua.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    @GetMapping("/fetchItemSellingStatistic")
    public ResponseEntity<?> fetchItemSellingStatistic(@RequestParam(name = "stockItemId") Long stockItemId,
                                                       @RequestParam(name = "from", required = false)
                                                       @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate from,
                                                       @RequestParam(name = "to", required = false)
                                                       @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate to,
                                                       @RequestParam(name = "pageSize") int pageSize,
                                                       @RequestParam(name = "page") int page) {
        return ResponseEntity.ok(analyticsService.fetchItemSellingStatistic(stockItemId,
                from,
                to,
                pageSize,
                page));
    }

    @GetMapping("/fetchBeneficiaryFinancialStatistic")
    public ResponseEntity<?> fetchBeneficiaryFinancialStatistic(@RequestParam(name = "beneficiaryId") Long beneficiaryId,
                                                                @RequestParam(name = "from", required = false)
                                                                @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate from,
                                                                @RequestParam(name = "to", required = false)
                                                                @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate to,
                                                                @RequestParam(name = "pageSize") int pageSize,
                                                                @RequestParam(name = "page") int page) {
        return ResponseEntity.ok(analyticsService.fetchBeneficiaryFinancialStatistic(beneficiaryId,
                from,
                to,
                pageSize,
                page));
    }
}
