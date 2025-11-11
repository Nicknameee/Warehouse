package io.store.ua.producers;

import io.store.ua.models.dto.ShipmentDTO;
import io.store.ua.utility.CodeGenerator;
import io.store.ua.validations.FieldValidator;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@RequiredArgsConstructor
@Validated
public class ShipmentSinkProducer {
    public static final String TOPIC = "SHIPMENT_SINK";
    private final FieldValidator fieldValidator;
    private final KafkaTemplate<String, ShipmentDTO> kafkaTemplate;

    @Retryable(maxAttempts = 5, backoff = @Backoff(multiplier = 3.0))
    public String produce(@NotNull(message = "Shipment can't be null") ShipmentDTO shipmentDTO) {
        fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.id, true);
        var key = CodeGenerator.KafkaCodeGenerator.generate(String.valueOf(shipmentDTO.getId()));

        kafkaTemplate.send(TOPIC, key, shipmentDTO);

        return key;
    }
}
