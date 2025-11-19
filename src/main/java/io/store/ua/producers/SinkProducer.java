package io.store.ua.producers;

import io.store.ua.models.dto.ShipmentDTO;
import io.store.ua.models.dto.TransactionDTO;
import io.store.ua.utility.CodeGenerator;
import io.store.ua.validations.FieldValidator;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Validated
public class SinkProducer {
    public static final String SHIPMENT_TOPIC = "SHIPMENT_TOPIC";
    public static final String TRANSACTION_TOPIC = "TRANSACTION_TOPIC";
    private final FieldValidator fieldValidator;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Retryable(maxAttempts = 5, backoff = @Backoff(multiplier = 3.0))
    public String produceShipment(@NotNull(message = "Shipment can't be null") ShipmentDTO shipmentDTO) {
        fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.id, true);
        var key = CodeGenerator.KafkaCodeGenerator.generate(String.valueOf(shipmentDTO.getId()));

        kafkaTemplate.send(SHIPMENT_TOPIC, key, shipmentDTO);

        return key;
    }

    @Retryable(maxAttempts = 5, backoff = @Backoff(multiplier = 3.0))
    public String produceTransaction(@NotNull(message = "Transaction can't be null") TransactionDTO transactionDTO) {
        fieldValidator.validate(transactionDTO, true, TransactionDTO.Fields.purpose,
                TransactionDTO.Fields.flow,
                TransactionDTO.Fields.amount,
                TransactionDTO.Fields.currency,
                TransactionDTO.Fields.beneficiaryId,
                TransactionDTO.Fields.paymentProvider);
        var key = CodeGenerator.KafkaCodeGenerator.generate(UUID.randomUUID().toString());

        kafkaTemplate.send(TRANSACTION_TOPIC, key, transactionDTO);

        return key;
    }
}
