package io.store.ua.consumers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.store.ua.models.dto.ShipmentDTO;
import io.store.ua.models.dto.TransactionDTO;
import io.store.ua.producers.SinkProducer;
import io.store.ua.service.ShipmentService;
import io.store.ua.service.TransactionService;
import io.store.ua.utility.RegularObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SinkConsumer {
    private final ShipmentService shipmentService;
    private final TransactionService transactionService;

    @KafkaListener(topics = SinkProducer.SHIPMENT_TOPIC, groupId = SinkProducer.SHIPMENT_TOPIC)
    public void consumeShipment(@Payload String shipmentDTO) throws JsonProcessingException {
        shipmentService.synchroniseShipment(RegularObjectMapper.read(shipmentDTO, ShipmentDTO.class));
    }

    @KafkaListener(topics = SinkProducer.TRANSACTION_TOPIC, groupId = SinkProducer.TRANSACTION_TOPIC)
    public void consumeTransaction(@Payload String transactionDTO) throws JsonProcessingException {
        transactionService.synchroniseTransaction(RegularObjectMapper.read(transactionDTO, TransactionDTO.class));
    }

}
