package io.store.ua.controllers;

import io.store.ua.entity.Warehouse;
import io.store.ua.models.dto.WarehouseDTO;
import io.store.ua.service.WarehouseService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
public class WarehouseController {
    private final WarehouseService warehouseService;

    @GetMapping("/findAll")
    public ResponseEntity<List<Warehouse>> findAllWarehouses(@RequestParam(name = "codePrefix", required = false) String codePrefix,
                                                             @RequestParam(name = "namePrefix", required = false) String namePrefix,
                                                             @RequestParam(name = "managerId", required = false) Long managerId,
                                                             @RequestParam(name = "isActive", required = false) Boolean isActive,
                                                             @RequestParam(name = "pageSize")
                                                             @Min(value = 1, message = "A size of page can't be less than one") int pageSize,
                                                             @RequestParam(name = "page")
                                                             @Min(value = 1, message = "A number of page can't be less than one") int page) {

        return ResponseEntity.ok(warehouseService.findBy(codePrefix,
                namePrefix,
                managerId,
                isActive,
                pageSize,
                page));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('OWNER')")
    public ResponseEntity<Warehouse> save(@RequestBody WarehouseDTO warehouseDTO) {
        return ResponseEntity.ok(warehouseService.save(warehouseDTO));
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER')")
    public ResponseEntity<Warehouse> update(@RequestBody WarehouseDTO warehouseDTO) {
        return ResponseEntity.ok(warehouseService.update(warehouseDTO));
    }

    @PutMapping("/toggle")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER')")
    public ResponseEntity<Warehouse> toggleState(@RequestParam(name = "code") String code) {
        return ResponseEntity.ok(warehouseService.toggleState(code));
    }
}
