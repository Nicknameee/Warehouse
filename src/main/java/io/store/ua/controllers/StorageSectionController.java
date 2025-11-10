package io.store.ua.controllers;

import io.store.ua.service.StorageSectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/storage/sections")
@RequiredArgsConstructor
@Validated
public class StorageSectionController {
    private final StorageSectionService storageSectionService;

    @GetMapping("/findAll")
    public ResponseEntity<?> findAll(@RequestParam(name = "pageSize") int pageSize,
                                     @RequestParam(name = "page") int page) {
        return ResponseEntity.ok(storageSectionService.findBy(null, pageSize, page));
    }

    @GetMapping("/findBy")
    public ResponseEntity<?> findBy(@RequestParam(name = "warehouse_id", required = false) Long warehouseId,
                                    @RequestParam(name = "pageSize") int pageSize,
                                    @RequestParam(name = "page") int page) {
        return ResponseEntity.ok(storageSectionService.findBy(warehouseId, pageSize, page));
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestParam(name = "warehouse_id") Long warehouseId,
                                  @RequestParam(name = "code") String code) {
        return ResponseEntity.ok(storageSectionService.save(warehouseId, code));
    }

    @PutMapping
    public ResponseEntity<?> update(@RequestParam(name = "id") Long sectionId,
                                    @RequestParam(name = "code") String newCode) {
        return ResponseEntity.ok(storageSectionService.update(sectionId, newCode));
    }
}
