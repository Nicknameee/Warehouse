package io.store.ua.controllers;

import io.store.ua.models.dto.QueueResponseDTO;
import io.store.ua.models.dto.ShipmentDTO;
import io.store.ua.models.dto.TransactionDTO;
import io.store.ua.producers.SinkProducer;
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
    private final SinkProducer sinkProducer;

    @PostMapping("/shipments")
    public ResponseEntity<?> enqueueShipment(@RequestBody ShipmentDTO shipmentDTO) {
        String key = sinkProducer.produceShipment(shipmentDTO);

        return ResponseEntity.accepted()
                .body(QueueResponseDTO.builder()
                        .key(key)
                        .topic(SinkProducer.SHIPMENT_TOPIC)
                        .isEnqueued(true)
                        .build());
    }

    @PostMapping("/transactions")
    public ResponseEntity<?> enqueueTransactions(@RequestBody TransactionDTO transactionDTO) {
        String key = sinkProducer.produceTransaction(transactionDTO);

        return ResponseEntity.accepted()
                .body(QueueResponseDTO.builder()
                        .key(key)
                        .topic(SinkProducer.TRANSACTION_TOPIC)
                        .isEnqueued(true)
                        .build());
    }
}
