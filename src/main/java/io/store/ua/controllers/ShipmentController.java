package io.store.ua.controllers;

import io.store.ua.models.dto.ShipmentDTO;
import io.store.ua.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class ShipmentController {
    private final ShipmentService shipmentService;

    @GetMapping("/findAll")
    public ResponseEntity<?> findAll(@RequestParam(name = "pageSize") int pageSize,
                                     @RequestParam(name = "page") int page) {
        return ResponseEntity.ok(shipmentService.findAll(pageSize, page));
    }

    @GetMapping("/findBy")
    public ResponseEntity<?> findBy(@RequestParam(value = "warehouseIdSender", required = false) Long warehouseIdSender,
                                    @RequestParam(value = "warehouseIdRecipient", required = false) Long warehouseIdRecipient,
                                    @RequestParam(value = "stockItemId", required = false) Long stockItemId,
                                    @RequestParam(value = "status", required = false) String status,
                                    @RequestParam(value = "shipmentDirection", required = false) String shipmentDirection,
                                    @RequestParam(value = "from", required = false)
                                    @DateTimeFormat(pattern = "dd-MM-yyyy'At'HH:mm:ss") LocalDateTime from,
                                    @RequestParam(value = "to", required = false)
                                    @DateTimeFormat(pattern = "dd-MM-yyyy'At'HH:mm:ss") LocalDateTime to,
                                    @RequestParam("pageSize") int pageSize,
                                    @RequestParam("page") int page) {
        return ResponseEntity.ok(shipmentService.findBy(warehouseIdSender,
                warehouseIdRecipient,
                stockItemId,
                status,
                shipmentDirection,
                from,
                to,
                pageSize,
                page));
    }

    @GetMapping("/findBy/id")
    public ResponseEntity<?> findById(@RequestParam(name = "shipmentId") Long shipmentId) {
        return ResponseEntity.ok(shipmentService.findById(shipmentId));
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody ShipmentDTO shipmentDTO) {
        return ResponseEntity.ok(shipmentService.save(shipmentDTO));
    }

    @PutMapping
    public ResponseEntity<?> update(@RequestBody ShipmentDTO shipmentDTO) {
        return ResponseEntity.ok(shipmentService.update(shipmentDTO));
    }
}
