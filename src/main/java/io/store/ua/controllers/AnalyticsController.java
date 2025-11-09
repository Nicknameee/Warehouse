package io.store.ua.controllers;

import io.store.ua.models.data.ItemSellingStatistic;
import io.store.ua.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    @GetMapping("/itemSelling")
    public ResponseEntity<List<ItemSellingStatistic>> fetchItemSellingStatistic(@RequestParam(name = "stock_item_id", required = false) Long stockItemId,
                                                                                @RequestParam(required = false)
                                                                                @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate from,
                                                                                @RequestParam(required = false)
                                                                                @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate to,
                                                                                @RequestParam int pageSize,
                                                                                @RequestParam int page) {

        return ResponseEntity.ok(analyticsService.fetchItemSellingStatistic(stockItemId, from, to, pageSize, page));
    }

}
