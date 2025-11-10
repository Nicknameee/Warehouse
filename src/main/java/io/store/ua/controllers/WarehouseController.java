package io.store.ua.controllers;

import io.store.ua.models.dto.WarehouseDTO;
import io.store.ua.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
public class WarehouseController {
    private final WarehouseService warehouseService;

    @GetMapping("/findAll")
    public ResponseEntity<?> findAll(@RequestParam(name = "pageSize") int pageSize,
                                     @RequestParam(name = "page") int page) {
        return ResponseEntity.ok(warehouseService.findAll(pageSize, page));
    }

    @GetMapping("/findBy/code")
    public ResponseEntity<?> findByCode(@RequestParam(name = "code") String code) {
        return ResponseEntity.ok(warehouseService.findByCode(code));
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody WarehouseDTO warehouseDTO) {
        return ResponseEntity.ok(warehouseService.save(warehouseDTO));
    }

    @PutMapping
    public ResponseEntity<?> update(@RequestBody WarehouseDTO warehouseDTO) {
        return ResponseEntity.ok(warehouseService.update(warehouseDTO));
    }

    @PutMapping("/toggle")
    public ResponseEntity<?> toggleState(@RequestParam(name = "code") String code) {
        return ResponseEntity.ok(warehouseService.toggleState(code));
    }
}
