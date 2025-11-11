package io.store.ua.consumers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.store.ua.models.dto.ShipmentDTO;
import io.store.ua.producers.ShipmentSinkProducer;
import io.store.ua.service.ShipmentService;
import io.store.ua.utility.RegularObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SinkConsumer {
    private final ShipmentService shipmentService;

    @KafkaListener(topics = ShipmentSinkProducer.TOPIC, groupId = ShipmentSinkProducer.TOPIC)
    public void consume(@Payload String shipmentDTO) throws JsonProcessingException {
        shipmentService.synchroniseShipment(RegularObjectMapper.read(shipmentDTO, ShipmentDTO.class));
    }
}
