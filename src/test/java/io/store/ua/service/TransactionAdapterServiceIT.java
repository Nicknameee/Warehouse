package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Transaction;
import io.store.ua.enums.PaymentProvider;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.data.CheckoutFinancialInformation;
import io.store.ua.service.external.DataTransAPIService;
import io.store.ua.service.external.LiqPayAPIService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class TransactionAdapterServiceIT extends AbstractIT {
    @Autowired
    private TransactionAdapterService transactionAdapterService;
    @MockitoBean
    private DataTransAPIService dataTransService;
    @MockitoBean
    private LiqPayAPIService liqPayService;

    @Nested
    @DisplayName("initiateIncomingPayment(transaction: Transaction, settleOnInitiation: boolean, provider: PaymentProvider)")
    class InitiateIncomingPaymentTests {
        @Test
        @DisplayName("initiateIncomingPayment_success(transaction: Transaction, settleOnInitiation: boolean, provider: PaymentProvider): delegates to matching provider")
        void initiateIncomingPayment_success_delegates() {
            when(dataTransService.provider()).thenReturn(PaymentProvider.DATA_TRANS);
            when(liqPayService.provider()).thenReturn(PaymentProvider.LIQ_PAY);

            Transaction transaction = Transaction.builder()
                    .currency(RandomStringUtils.secure().nextAlphabetic(3).toUpperCase())
                    .reference(RandomStringUtils.secure().nextAlphanumeric(16))
                    .transactionId(RandomStringUtils.secure().nextAlphanumeric(24))
                    .build();

            CheckoutFinancialInformation financialInformation = CheckoutFinancialInformation.builder()
                    .paymentProvider(PaymentProvider.DATA_TRANS)
                    .transactionId(RandomStringUtils.secure().nextAlphanumeric(24))
                    .reference(RandomStringUtils.secure().nextAlphanumeric(16))
                    .build();

            when(dataTransService.initiateIncomingPayment(transaction, true)).thenReturn(financialInformation);

            CheckoutFinancialInformation result = transactionAdapterService.initiateIncomingPayment(transaction,
                    true,
                    PaymentProvider.DATA_TRANS);

            assertThat(result).isNotNull();
            assertSame(financialInformation, result);
            assertThat(result.getPaymentProvider()).isEqualTo(PaymentProvider.DATA_TRANS);
            verify(dataTransService, times(1)).initiateIncomingPayment(transaction, true);
            verify(liqPayService, never()).initiateIncomingPayment(any(), anyBoolean());
        }

        @Test
        @DisplayName("initiateIncomingPayment_fail(transaction: Transaction, settleOnInitiation: boolean, provider: PaymentProvider): throws NotFoundException when provider missing")
        void initiateIncomingPayment_fail_notFound() {
            when(dataTransService.provider()).thenReturn(PaymentProvider.DATA_TRANS);
            when(liqPayService.provider()).thenReturn(PaymentProvider.LIQ_PAY);

            Transaction transaction = Transaction.builder().build();

            assertThrows(NotFoundException.class, () -> transactionAdapterService.initiateIncomingPayment(transaction,
                    false,
                    PaymentProvider.GOOGLE_PAY));
            verify(dataTransService, never()).initiateIncomingPayment(any(), anyBoolean());
            verify(liqPayService, never()).initiateIncomingPayment(any());
        }
    }

    @Nested
    @DisplayName("initiateOutcomingPayment(transaction: Transaction, settleOnInitiation: boolean, provider: PaymentProvider)")
    class InitiateOutcomingPaymentTests {
        @Test
        @DisplayName("initiateOutcomingPayment_success(transaction: Transaction, settleOnInitiation: boolean, provider: PaymentProvider): delegates to matching provider")
        void initiateOutcomingPayment_success_delegates() {
            when(dataTransService.provider()).thenReturn(PaymentProvider.DATA_TRANS);
            when(liqPayService.provider()).thenReturn(PaymentProvider.LIQ_PAY);

            Transaction transaction = Transaction.builder()
                    .reference(RandomStringUtils.secure().nextAlphanumeric(16))
                    .transactionId(RandomStringUtils.secure().nextAlphanumeric(24))
                    .build();

            when(liqPayService.initiateOutcomingPayment(transaction, false)).thenReturn(transaction);

            Transaction result = transactionAdapterService.initiateOutcomingPayment(transaction,
                    false,
                    PaymentProvider.LIQ_PAY);

            assertSame(transaction, result);
            verify(liqPayService, times(1)).initiateOutcomingPayment(transaction, false);
            verify(dataTransService, never()).initiateOutcomingPayment(any(), anyBoolean());
        }

        @Test
        @DisplayName("initiateOutcomingPayment_fail(transaction: Transaction, settleOnInitiation: boolean, provider: PaymentProvider): throws NotFoundException when provider missing")
        void initiateOutcomingPayment_fail_notFound() {
            when(dataTransService.provider()).thenReturn(PaymentProvider.DATA_TRANS);
            when(liqPayService.provider()).thenReturn(PaymentProvider.LIQ_PAY);

            Transaction transaction = Transaction.builder().build();

            assertThrows(NotFoundException.class, () -> transactionAdapterService.initiateOutcomingPayment(transaction,
                    true,
                    PaymentProvider.CASH));
            verify(dataTransService, never()).initiateOutcomingPayment(any(), anyBoolean());
            verify(liqPayService, never()).initiateOutcomingPayment(any());
        }
    }

    @Nested
    @DisplayName("settlePayment(transaction: Transaction, provider: PaymentProvider)")
    class SettlePaymentTests {
        @Test
        @DisplayName("settlePayment_success(transaction: Transaction, provider: PaymentProvider): delegates to matching provider")
        void settlePayment_success_delegates() {
            when(dataTransService.provider()).thenReturn(PaymentProvider.DATA_TRANS);
            when(liqPayService.provider()).thenReturn(PaymentProvider.LIQ_PAY);

            Transaction transaction = Transaction.builder()
                    .transactionId(RandomStringUtils.secure().nextAlphanumeric(24))
                    .reference(RandomStringUtils.secure().nextAlphanumeric(16))
                    .build();

            when(dataTransService.settlePayment(transaction)).thenReturn(transaction);

            Transaction result = transactionAdapterService.settlePayment(transaction, PaymentProvider.DATA_TRANS);

            assertSame(transaction, result);
            verify(dataTransService, times(1)).settlePayment(transaction);
            verify(liqPayService, never()).settlePayment(any());
        }

        @Test
        @DisplayName("settlePayment_fail(transaction: Transaction, provider: PaymentProvider): throws NotFoundException when provider missing")
        void settlePayment_fail_notFound() {
            when(dataTransService.provider()).thenReturn(PaymentProvider.DATA_TRANS);
            when(liqPayService.provider()).thenReturn(PaymentProvider.LIQ_PAY);

            Transaction transaction = Transaction.builder().build();

            assertThrows(NotFoundException.class, () -> transactionAdapterService.settlePayment(transaction, PaymentProvider.GOOGLE_PAY));
            verify(dataTransService, never()).settlePayment(any());
            verify(liqPayService, never()).settlePayment(any());
        }
    }
}
