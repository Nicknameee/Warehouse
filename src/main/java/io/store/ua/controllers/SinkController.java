package io.store.ua.controllers;

import io.store.ua.models.dto.QueueResponseDTO;
import io.store.ua.models.dto.ShipmentDTO;
import io.store.ua.producers.ShipmentSinkProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sink")
@RequiredArgsConstructor
public class SinkController {
    private final ShipmentSinkProducer shipmentSinkProducer;

    @PostMapping("/shipments")
    public ResponseEntity<?> enqueueShipment(@RequestBody ShipmentDTO shipmentDTO) {
        String key = shipmentSinkProducer.produce(shipmentDTO);

        return ResponseEntity.accepted()
                .body(QueueResponseDTO.builder()
                        .key(key)
                        .topic(ShipmentSinkProducer.TOPIC)
                        .isEnqueued(true)
                        .build());
    }
}
