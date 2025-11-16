package io.store.ua.controllers;

import io.store.ua.entity.StorageSection;
import io.store.ua.service.StorageSectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/storage/sections")
@RequiredArgsConstructor
@Validated
public class StorageSectionController {
    private final StorageSectionService storageSectionService;

    @GetMapping("/findBy")
    public ResponseEntity<List<StorageSection>> findBy(@RequestParam(name = "warehouse_id", required = false) Long warehouseId,
                                                       @RequestParam(name = "isActive", required = false) Boolean isActive,
                                                       @RequestParam(name = "pageSize") int pageSize,
                                                       @RequestParam(name = "page") int page) {
        return ResponseEntity.ok(storageSectionService.findBy(warehouseId, isActive, pageSize, page));
    }

    @PostMapping
    public ResponseEntity<StorageSection> save(@RequestParam(name = "warehouse_id") Long warehouseId,
                                               @RequestParam(name = "code") String code) {
        return ResponseEntity.ok(storageSectionService.save(warehouseId, code));
    }

    @PutMapping
    public ResponseEntity<StorageSection> update(@RequestParam(name = "id") Long sectionId,
                                                 @RequestParam(name = "isActive", required = false) Boolean isActive,
                                                 @RequestParam(name = "code") String newCode) {
        return ResponseEntity.ok(storageSectionService.update(sectionId, isActive, newCode));
    }
}
