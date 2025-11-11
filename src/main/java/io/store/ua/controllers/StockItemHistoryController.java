package io.store.ua.controllers;

import io.store.ua.entity.immutable.StockItemHistory;
import io.store.ua.service.StockItemHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stockItemsHistory")
@RequiredArgsConstructor
public class StockItemHistoryController {
    private final StockItemHistoryService stockItemHistoryService;

    @GetMapping("/findBy")
    public ResponseEntity<List<StockItemHistory>> findBy(@RequestParam(name = "stock_item_id", required = false) Long stockItemId,
                                                         @RequestParam(name = "from", required = false)
                                                         @DateTimeFormat(pattern = "dd-MM-yyyy'At'HH:mm:ss") LocalDateTime from,
                                                         @RequestParam(name = "to", required = false)
                                                         @DateTimeFormat(pattern = "dd-MM-yyyy'At'HH:mm:ss") LocalDateTime to,
                                                         @RequestParam(name = "pageSize") int pageSize,
                                                         @RequestParam(name = "page") int page) {
        return ResponseEntity.ok(stockItemHistoryService.findBy(stockItemId,
                from,
                to,
                pageSize,
                page));
    }
}
