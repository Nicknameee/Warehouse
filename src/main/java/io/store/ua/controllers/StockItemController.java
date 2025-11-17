package io.store.ua.controllers;

import io.store.ua.entity.StockItem;
import io.store.ua.models.data.StockItemVersionGroup;
import io.store.ua.models.dto.StockItemDTO;
import io.store.ua.service.StockItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stockItems")
@RequiredArgsConstructor
public class StockItemController {
    private final StockItemService stockItemService;

    @GetMapping("/findBy")
    public ResponseEntity<List<StockItem>> findBy(@RequestParam(name = "warehouseId", required = false) List<Long> warehouseIds,
                                                  @RequestParam(name = "productId", required = false) List<Long> productIds,
                                                  @RequestParam(name = "stockItemGroupId", required = false) List<Long> stockItemGroupIds,
                                                  @RequestParam(name = "status", required = false) List<String> statuses,
                                                  @RequestParam(name = "storageSectionId", required = false) List<Long> storageSectionIds,
                                                  @RequestParam(name = "codePart", required = false) String codePart,
                                                  @RequestParam(name = "isItemActive", required = false) Boolean isItemActive,
                                                  @RequestParam(name = "isItemGroupActive", required = false) Boolean isItemGroupActive,
                                                  @RequestParam(name = "pageSize") int pageSize,
                                                  @RequestParam(name = "page") int page) {

        return ResponseEntity.ok(stockItemService.findBy(warehouseIds,
                productIds,
                stockItemGroupIds,
                statuses,
                storageSectionIds,
                codePart,
                isItemActive,
                isItemGroupActive,
                pageSize,
                page));
    }

    @GetMapping("/findVersionBatch")
    public ResponseEntity<List<StockItemVersionGroup>> findVersionBatch(@RequestParam(name = "warehouseId", required = false) List<Long> warehouseIds,
                                                                        @RequestParam(name = "productId", required = false) List<Long> productIds,
                                                                        @RequestParam(name = "stockItemGroupId", required = false) List<Long> stockItemGroupIds,
                                                                        @RequestParam(name = "status", required = false) List<String> statuses,
                                                                        @RequestParam(name = "storageSectionId", required = false) List<Long> storageSectionIds,
                                                                        @RequestParam(name = "codePart", required = false) String codePart,
                                                                        @RequestParam(name = "isItemActive", required = false) Boolean isItemActive,
                                                                        @RequestParam(name = "isItemGroupActive", required = false) Boolean isItemGroupActive,
                                                                        @RequestParam(name = "pageSize") int pageSize,
                                                                        @RequestParam(name = "page") int page) {

        return ResponseEntity.ok(stockItemService.findVersionBatch(warehouseIds,
                productIds,
                stockItemGroupIds,
                statuses,
                storageSectionIds,
                codePart,
                isItemActive,
                isItemGroupActive,
                pageSize,
                page));
    }

    @PostMapping
    public ResponseEntity<StockItem> create(@RequestBody StockItemDTO dto) {
        return ResponseEntity.ok(stockItemService.create(dto));
    }

    @PutMapping
    public ResponseEntity<StockItem> update(@RequestBody StockItemDTO dto) {
        return ResponseEntity.ok(stockItemService.update(dto));
    }
}
