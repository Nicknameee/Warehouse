package io.store.ua.controllers;

import io.store.ua.entity.StockItemGroup;
import io.store.ua.models.dto.StockItemGroupDTO;
import io.store.ua.service.StockItemGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stockItemGroups")
@RequiredArgsConstructor
public class StockItemGroupController {
    private final StockItemGroupService stockItemGroupService;

    @GetMapping("/findAll")
    public ResponseEntity<List<StockItemGroup>> findAll(@RequestParam(name = "pageSize") int pageSize,
                                                        @RequestParam(name = "page") int page) {
        return ResponseEntity.ok(stockItemGroupService.findAll(pageSize, page));
    }

    @GetMapping("/findBy/code")
    public ResponseEntity<StockItemGroup> findByCode(@RequestParam(name = "code") String code) {
        return ResponseEntity.ok(stockItemGroupService.findByCode(code));
    }

    @PostMapping
    public ResponseEntity<StockItemGroup> save(@RequestBody StockItemGroupDTO stockItemGroupDTO) {
        return ResponseEntity.ok(stockItemGroupService.save(stockItemGroupDTO));
    }

    @PutMapping
    public ResponseEntity<StockItemGroup> update(@RequestParam(name = "groupId") Long groupId,
                                                 @RequestParam(name = "name", required = false) String name,
                                                 @RequestParam(name = "is_active", required = false) Boolean isActive) {
        return ResponseEntity.ok(stockItemGroupService.update(groupId, name, isActive));
    }
}
