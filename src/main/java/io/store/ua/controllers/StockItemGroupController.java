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

    @GetMapping("/findBy")
    public ResponseEntity<List<StockItemGroup>> findBy(@RequestParam(name = "codePart", required = false) String codePart,
                                                       @RequestParam(name = "isActive", required = false) Boolean isActive,
                                                       @RequestParam(name = "pageSize") int pageSize,
                                                       @RequestParam(name = "page") int page) {
        return ResponseEntity.ok(stockItemGroupService.findBy(codePart, isActive, pageSize, page));
    }

    @PostMapping
    public ResponseEntity<StockItemGroup> save(@RequestBody StockItemGroupDTO stockItemGroupDTO) {
        return ResponseEntity.ok(stockItemGroupService.save(stockItemGroupDTO));
    }

    @PutMapping
    public ResponseEntity<StockItemGroup> update(@RequestParam(name = "groupId") Long groupId,
                                                 @RequestParam(name = "name", required = false) String name,
                                                 @RequestParam(name = "isActive", required = false) Boolean isActive) {
        return ResponseEntity.ok(stockItemGroupService.update(groupId, name, isActive));
    }
}
