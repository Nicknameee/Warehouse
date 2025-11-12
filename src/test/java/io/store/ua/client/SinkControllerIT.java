package io.store.ua.client;

import io.store.ua.controllers.SinkController;
import io.store.ua.enums.*;
import io.store.ua.models.dto.QueueResponseDTO;
import io.store.ua.models.dto.ShipmentDTO;
import io.store.ua.models.dto.TransactionDTO;
import io.store.ua.producers.SinkProducer;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SinkControllerIT {
    private final SinkProducer sinkProducer = mock(SinkProducer.class);
    private final SinkController sinkController = new SinkController(sinkProducer);

    @Test
    @DisplayName("enqueueShipment returns ACCEPTED with queue metadata")
    void enqueueShipment_returnsAccepted() {
        ShipmentDTO shipmentDTO = ShipmentDTO.builder()
                .id(RandomUtils.secure().randomLong(1, 100_000_000))
                .status(ShipmentStatus.SENT.name())
                .build();

        String key = RandomStringUtils.secure().nextAlphanumeric(39);

        when(sinkProducer.produceShipment(shipmentDTO))
                .thenReturn(key);

        ResponseEntity<?> response = sinkController.enqueueShipment(shipmentDTO);

        assertThat(response.getStatusCode().value())
                .isEqualTo(HttpStatus.ACCEPTED.value());

        QueueResponseDTO body = (QueueResponseDTO) response.getBody();

        assertThat(body)
                .isNotNull();
        assertThat(body.isEnqueued())
                .isTrue();
        assertThat(body.getKey())
                .isEqualTo(key);
        assertThat(body.getTopic())
                .isEqualTo(SinkProducer.SHIPMENT_TOPIC);

        ArgumentCaptor<ShipmentDTO> argumentCaptor = ArgumentCaptor.forClass(ShipmentDTO.class);

        verify(sinkProducer)
                .produceShipment(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue().getId())
                .isEqualTo(shipmentDTO.getId());
        assertThat(argumentCaptor.getValue().getStatus())
                .isEqualTo(shipmentDTO.getStatus());
    }

    @Test
    @DisplayName("enqueueTransactions returns ACCEPTED with queue metadata")
    void enqueueTransactions_returnsAccepted() {
        TransactionDTO transactionDTO = TransactionDTO.builder()
                .purpose(TransactionPurpose.STOCK_OUTBOUND_REVENUE.name())
                .flow(TransactionFlowType.CREDIT.name())
                .amount(BigInteger.valueOf(100_000))
                .currency(Currency.UAH.name())
                .receiverFinancialAccountId(Long.MAX_VALUE)
                .paymentProvider(PaymentProvider.CASH.name())
                .build();

        String key = RandomStringUtils.secure().nextAlphanumeric(39);

        when(sinkProducer.produceTransaction(transactionDTO))
                .thenReturn(key);

        ResponseEntity<?> response = sinkController.enqueueTransactions(transactionDTO);

        assertThat(response.getStatusCode().value())
                .isEqualTo(HttpStatus.ACCEPTED.value());

        QueueResponseDTO body = (QueueResponseDTO) response.getBody();

        assertThat(body)
                .isNotNull();
        assertThat(body.isEnqueued())
                .isTrue();
        assertThat(body.getKey())
                .isEqualTo(key);
        assertThat(body.getTopic())
                .isEqualTo(SinkProducer.TRANSACTION_TOPIC);

        ArgumentCaptor<TransactionDTO> argumentCaptor = ArgumentCaptor.forClass(TransactionDTO.class);

        verify(sinkProducer)
                .produceTransaction(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getPaymentProvider())
                .isEqualTo(transactionDTO.getPaymentProvider());
        assertThat(argumentCaptor.getValue().getFlow())
                .isEqualTo(transactionDTO.getFlow());
    }
}
