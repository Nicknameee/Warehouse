package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Beneficiary;
import io.store.ua.entity.Transaction;
import io.store.ua.enums.*;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.api.data.DataTransTransaction;
import io.store.ua.models.api.external.request.LPInitiatePaymentRequestDTO;
import io.store.ua.models.api.external.response.LPInitiatePaymentResponse;
import io.store.ua.models.api.external.response.LPResponse;
import io.store.ua.models.data.CheckoutFinancialInformation;
import io.store.ua.models.data.ExternalReferences;
import io.store.ua.models.dto.TransactionDTO;
import io.store.ua.service.external.DataTransAPIService;
import io.store.ua.service.external.LiqPayAPIService;
import io.store.ua.utility.CodeGenerator;
import jakarta.validation.ValidationException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransactionServiceIT extends AbstractIT {
    @Autowired
    private TransactionService transactionService;
    @MockitoBean
    private DataTransAPIService dataTransService;
    @MockitoBean
    private LiqPayAPIService liqPayService;

    private Beneficiary beneficiary;

    @BeforeEach
    void setupBeneficiary() {
        beneficiary = beneficiaryRepository.save(
                Beneficiary.builder()
                        .name(RandomStringUtils.secure().nextAlphabetic(10))
                        .iban("UA" + RandomStringUtils.secure().nextNumeric(27))
                        .swift(RandomStringUtils.secure().nextAlphabetic(8).toUpperCase())
                        .card(RandomStringUtils.secure().nextNumeric(16))
                        .isActive(true)
                        .build()
        );
    }

    private TransactionDTO buildTransactionDTO(PaymentProvider paymentProvider,
                                               String purpose,
                                               BigInteger amount,
                                               String currencyCode,
                                               Long beneficiaryId) {
        return TransactionDTO.builder()
                .purpose(purpose)
                .amount(amount)
                .currency(currencyCode)
                .receiverFinancialAccountId(beneficiaryId)
                .paymentProvider(paymentProvider.name())
                .build();
    }

    @Nested
    @DisplayName("findBy(...)")
    class FindByTests {
        @Test
        @DisplayName("findBy_success: findBy filters by multiple fields (id/reference/currency/amount/time/enums/provider)")
        void findBy_success() {
            LocalDateTime now = LocalDateTime.now();
            String reference = CodeGenerator.TransactionCodeGenerator.generate(PaymentProvider.DATA_TRANS);

            transactionRepository.saveAll(List.of(Transaction.builder()
                            .transactionId(reference)
                            .reference(reference)
                            .flowType(TransactionFlowType.CREDIT)
                            .purpose(TransactionPurpose.FEE)
                            .status(TransactionStatus.INITIATED)
                            .amount(BigInteger.valueOf(150_900))
                            .currency(Currency.USD.name())
                            .beneficiaryId(beneficiary.getId())
                            .paymentProvider(PaymentProvider.DATA_TRANS)
                            .createdAt(now.minusHours(1))
                            .paidAt(now.plusHours(1))
                            .build(),
                    Transaction.builder()
                            .transactionId(CodeGenerator.TransactionCodeGenerator.generate(PaymentProvider.LIQ_PAY))
                            .reference(CodeGenerator.TransactionCodeGenerator.generate(PaymentProvider.LIQ_PAY))
                            .flowType(TransactionFlowType.DEBIT)
                            .purpose(TransactionPurpose.RENT)
                            .status(TransactionStatus.CANCELLED)
                            .amount(BigInteger.valueOf(5000))
                            .currency(Currency.EUR.name())
                            .beneficiaryId(beneficiary.getId())
                            .paymentProvider(PaymentProvider.LIQ_PAY)
                            .createdAt(now.minusDays(3))
                            .build()));

            List<Transaction> result = transactionService.findBy(reference,
                    reference,
                    Currency.USD.name().toLowerCase(),
                    BigInteger.valueOf(100_000),
                    BigInteger.valueOf(300_000),
                    now.minusHours(10),
                    now.plusHours(10),
                    now.minusHours(10),
                    now.plusHours(10),
                    TransactionFlowType.CREDIT.name(),
                    TransactionPurpose.FEE.name(),
                    TransactionStatus.INITIATED.name(),
                    beneficiary.getId(),
                    PaymentProvider.DATA_TRANS.name(),
                    500,
                    1);

            assertThat(result).isNotEmpty();
            assertThat(result).extracting(Transaction::getTransactionId).containsExactly(reference);
        }

        @Test
        @DisplayName("findBy_success: findBy filters by multiple fields (id/reference/currency/amount/time/enums/provider)")
        void findBy_success_emptyResultSet() {
            List<Transaction> result = transactionService.findBy(null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    500,
                    1);

            assertThat(result).isEmpty();
        }

        @ParameterizedTest
        @CsvSource({
                "0,1",
                "1,0",
                "-1,1",
                "1,-1",
                "0,0"
        })
        @DisplayName("findBy_fails_whenInvalidPageOrSize: throws ConstraintViolationException for invalid pagination arguments")
        void findBy_fails_whenInvalidPageOrSize(int pageSize, int pageNumber) {
            assertThrows(jakarta.validation.ConstraintViolationException.class, () -> transactionService.findBy(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    pageSize,
                    pageNumber
            ));
        }
    }

    @Nested
    @DisplayName("initiateIncomingPayment(transactionDTO: TransactionDTO, autoSettle: Boolean)")
    class InitiateIncomingPaymentTests {
        public static Stream<UnaryOperator<TransactionDTO>> generateTransactionMutationFlows() {
            return Stream.of(
                    transactionDTO -> {
                        transactionDTO.setPurpose(null);
                        return transactionDTO;
                    },
                    transactionDTO -> {
                        transactionDTO.setAmount(null);
                        return transactionDTO;
                    },
                    transactionDTO -> {
                        transactionDTO.setAmount(BigInteger.ZERO);
                        return transactionDTO;
                    },
                    transactionDTO -> {
                        transactionDTO.setCurrency(null);
                        return transactionDTO;
                    },
                    transactionDTO -> {
                        transactionDTO.setCurrency("");
                        return transactionDTO;
                    },
                    transactionDTO -> {
                        transactionDTO.setCurrency("usd");
                        return transactionDTO;
                    },
                    transactionDTO -> {
                        transactionDTO.setReceiverFinancialAccountId(null);
                        return transactionDTO;
                    },
                    transactionDTO -> {
                        transactionDTO.setPaymentProvider(null);
                        return transactionDTO;
                    }
            );
        }

        @Test
        @DisplayName("initiateIncomingPayment_success: initialises and saves transaction with autoSettle")
        void initiateIncomingPayment_success() {
            when(dataTransService.provider()).thenReturn(PaymentProvider.DATA_TRANS);
            when(liqPayService.provider()).thenReturn(PaymentProvider.LIQ_PAY);
            when(dataTransService.initiateIncomingPayment(any(Transaction.class), eq(true))).thenCallRealMethod();

            long initialCount = transactionRepository.count();

            var responseTransaction = DataTransTransaction.builder()
                    .transactionId(RandomStringUtils.secure().nextAlphanumeric(30))
                    .reference(RandomStringUtils.secure().nextAlphanumeric(30))
                    .build();

            when(dataTransService.initiateIncomingPaymentAPICall(anyString(), any(BigInteger.class), eq(true))).thenReturn(responseTransaction);

            TransactionDTO transactionDTO = buildTransactionDTO(PaymentProvider.DATA_TRANS,
                    TransactionPurpose.STOCK_OUTBOUND_REVENUE.name(),
                    BigInteger.valueOf(RandomUtils.secure().randomInt(100_000, 1_000_000)),
                    Currency.USD.name(),
                    beneficiary.getId());

            CheckoutFinancialInformation result = transactionService.initiateIncomingPayment(transactionDTO, true);

            var transaction = transactionRepository.findByTransactionId(responseTransaction.getTransactionId());

            assertTrue(transaction.isPresent());
            assertThat(transaction.get().getStatus()).isEqualTo(TransactionStatus.SETTLED);
            assertEquals(responseTransaction.getTransactionId(), transaction.get().getTransactionId());
            assertEquals(responseTransaction.getReference(), transaction.get().getReference());

            assertThat(result.getPaymentProvider()).isEqualTo(PaymentProvider.DATA_TRANS);
            assertNotNull(result.getTransactionId());
            assertNotNull(result.getReference());

            assertThat(transactionRepository.count()).isEqualTo(initialCount + 1);

            verify(dataTransService, times(1)).initiateIncomingPayment(any(Transaction.class), eq(true));
            verify(liqPayService, never()).initiateIncomingPayment(any(), anyBoolean());
        }

        @Test
        @DisplayName("initiateIncomingPayment_success_withoutAutoSettle: initialises and saves transaction without autoSettle")
        void initiateIncomingPayment_success_withoutAutoSettle() {
            when(dataTransService.provider()).thenReturn(PaymentProvider.DATA_TRANS);
            when(liqPayService.provider()).thenReturn(PaymentProvider.LIQ_PAY);
            when(liqPayService.initiateIncomingPayment(any(Transaction.class), eq(false))).thenCallRealMethod();

            long initialCount = transactionRepository.count();

            when(liqPayService.initiateIncomingPaymentAPICall(any(LPInitiatePaymentRequestDTO.class))).thenReturn(new LPInitiatePaymentResponse());

            TransactionDTO transactionDTO = buildTransactionDTO(PaymentProvider.LIQ_PAY,
                    TransactionPurpose.FEE.name(),
                    BigInteger.valueOf(RandomUtils.secure().randomInt(100_000, 1_000_000)),
                    Currency.EUR.name(),
                    beneficiary.getId());

            CheckoutFinancialInformation financialInformation = transactionService.initiateIncomingPayment(transactionDTO, false);

            assertThat(financialInformation.getPaymentProvider().name()).isEqualTo(transactionDTO.getPaymentProvider());
            assertNotNull(financialInformation.getTransactionId());
            assertNotNull(financialInformation.getReference());
            assertThat(transactionRepository.count()).isEqualTo(initialCount + 1);

            verify(liqPayService, times(1)).initiateIncomingPayment(any(Transaction.class), eq(false));
            verify(dataTransService, never()).initiateIncomingPayment(any(), anyBoolean());
        }

        @Test
        @DisplayName("initiateIncomingPayment_fails_whenFinancialServiceIsNotFoundForProvider: throws NotFoundException when financial service is not found for provider")
        void initiateIncomingPayment_fails_whenFinancialServiceIsNotFoundForProvider() {
            when(dataTransService.provider()).thenReturn(PaymentProvider.DATA_TRANS);
            when(liqPayService.provider()).thenReturn(PaymentProvider.LIQ_PAY);

            long initialCount = transactionRepository.count();

            TransactionDTO transactionDTO = buildTransactionDTO(PaymentProvider.GOOGLE_PAY,
                    TransactionPurpose.OTHER.name(),
                    BigInteger.valueOf(300_000),
                    Currency.USD.name(),
                    beneficiary.getId());

            assertThrows(NotFoundException.class, () -> transactionService.initiateIncomingPayment(transactionDTO, false));
            assertThat(transactionRepository.count()).isEqualTo(initialCount);

            verify(dataTransService, never()).initiateIncomingPayment(any(), anyBoolean());
            verify(liqPayService, never()).initiateIncomingPayment(any(), anyBoolean());
        }

        @Test
        @DisplayName("initiateIncomingPayment_fails_whenFinancialServiceIsNotFoundForProvider: throws NotFoundException when financial service is not found for provider")
        void initiateIncomingPayment_fails_whenBeneficiary() {
            when(dataTransService.provider()).thenReturn(PaymentProvider.DATA_TRANS);
            when(liqPayService.provider()).thenReturn(PaymentProvider.LIQ_PAY);

            long initialCount = transactionRepository.count();

            TransactionDTO transactionDTO = buildTransactionDTO(PaymentProvider.GOOGLE_PAY,
                    TransactionPurpose.OTHER.name(),
                    BigInteger.valueOf(300_000),
                    Currency.USD.name(),
                    beneficiary.getId());

            assertThrows(NotFoundException.class, () -> transactionService.initiateIncomingPayment(transactionDTO, false));
            assertThat(transactionRepository.count()).isEqualTo(initialCount);

            verify(dataTransService, never()).initiateIncomingPayment(any(), anyBoolean());
            verify(liqPayService, never()).initiateIncomingPayment(any(), anyBoolean());
        }

        @Test
        @DisplayName("initiateIncomingPayment_fails_whenProviderDoesNotSupportAutoSettle: throws BusinessException when provider does not support autoSettle = true (LiqPay)")
        void initiateIncomingPayment_fails_whenProviderDoesNotSupportAutoSettle() {
            when(dataTransService.provider()).thenReturn(PaymentProvider.DATA_TRANS);
            when(liqPayService.provider()).thenReturn(PaymentProvider.LIQ_PAY);
            when(liqPayService.initiateIncomingPayment(any(Transaction.class), eq(true))).thenCallRealMethod();

            long initialCount = transactionRepository.count();

            TransactionDTO transactionDTO = buildTransactionDTO(PaymentProvider.LIQ_PAY,
                    TransactionPurpose.FEE.name(),
                    BigInteger.valueOf(100_000),
                    Currency.USD.name(),
                    beneficiary.getId());

            assertThrows(UnsupportedOperationException.class, () -> transactionService.initiateIncomingPayment(transactionDTO, true));
            assertThat(transactionRepository.count()).isEqualTo(initialCount);

            verify(liqPayService, times(1)).initiateIncomingPayment(any(Transaction.class), eq(true));
            verify(dataTransService, never()).initiateIncomingPayment(any(), anyBoolean());
        }

        @ParameterizedTest(name = "Invalid DTO case #{index}")
        @MethodSource("generateTransactionMutationFlows")
        @DisplayName("initiateIncomingPayment_fails_onInvalidTransaction: throws ValidationException and does not call providers")
        void initiateIncomingPayment_fails_onInvalidTransaction(UnaryOperator<TransactionDTO> invalidMutationFlow) {
            long initialCount = transactionRepository.count();

            TransactionDTO transactionDTO = invalidMutationFlow.apply(buildTransactionDTO(PaymentProvider.DATA_TRANS,
                    TransactionPurpose.STOCK_OUTBOUND_REVENUE.name(),
                    BigInteger.valueOf(RandomUtils.secure().randomInt(100_000, 1_000_000)),
                    Currency.USD.name(),
                    beneficiary.getId()));

            assertThatThrownBy(() -> transactionService.initiateIncomingPayment(transactionDTO, true))
                    .isInstanceOf(ValidationException.class);

            assertThat(transactionRepository.count()).isEqualTo(initialCount);

            verifyNoInteractions(dataTransService, liqPayService);
        }
    }

    @Nested
    @DisplayName("initiateOutcomingPayment(...)")
    class InitiateOutcomingPaymentTests {
        static Stream<UnaryOperator<TransactionDTO>> invalidDTOMutations() {
            return Stream.of(
                    transactionDTO -> {
                        transactionDTO.setPurpose(null);
                        return transactionDTO;
                    },
                    transactionDTO -> {
                        transactionDTO.setPurpose("INVALID_ENUM");
                        return transactionDTO;
                    },
                    transactionDTO -> {
                        transactionDTO.setAmount(null);
                        return transactionDTO;
                    },
                    transactionDTO -> {
                        transactionDTO.setAmount(BigInteger.ZERO);
                        return transactionDTO;
                    },
                    transactionDTO -> {
                        transactionDTO.setAmount(new BigInteger("-1"));
                        return transactionDTO;
                    },
                    transactionDTO -> {
                        transactionDTO.setCurrency(null);
                        return transactionDTO;
                    },
                    transactionDTO -> {
                        transactionDTO.setCurrency("");
                        return transactionDTO;
                    },
                    transactionDTO -> {
                        transactionDTO.setCurrency("usd");
                        return transactionDTO;
                    },
                    transactionDTO -> {
                        transactionDTO.setReceiverFinancialAccountId(null);
                        return transactionDTO;
                    }
            );
        }

        @Test
        @DisplayName("initiateOutcomingPayment_success: delegates to LiqPay (autoSettle=false) and persists provider-returned transaction")
        void initiateOutcomingPayment_success() {
            when(dataTransService.provider()).thenReturn(PaymentProvider.DATA_TRANS);
            when(liqPayService.provider()).thenReturn(PaymentProvider.LIQ_PAY);
            when(liqPayService.initiateOutcomingPayment(any(Transaction.class), eq(false))).thenCallRealMethod();

            long initialCount = transactionRepository.count();

            when(liqPayService.initiateOutcomingPaymentAPICall(any(LPInitiatePaymentRequestDTO.class))).thenReturn(LPResponse.builder()
                    .status(LPResponse.Status.UNKNOWN)
                    .build());

            TransactionDTO transactionDTO = buildTransactionDTO(PaymentProvider.LIQ_PAY,
                    TransactionPurpose.SHIPPING.name(),
                    BigInteger.valueOf(RandomUtils.secure().randomInt(100_000, 1_000_000)),
                    Currency.USD.name(),
                    beneficiary.getId());

            Transaction outcomingPayment = transactionService.initiateOutcomingPayment(transactionDTO, false);

            assertThat(transactionRepository.count()).isEqualTo(initialCount + 1);
            assertNotNull(outcomingPayment.getTransactionId());
            assertNotNull(outcomingPayment.getReference());
            assertThat(outcomingPayment.getStatus()).isEqualTo(TransactionStatus.INITIATED);
            assertThat(outcomingPayment.getFlowType()).isEqualTo(TransactionFlowType.DEBIT);
            assertThat(outcomingPayment.getPurpose()).isEqualTo(TransactionPurpose.SHIPPING);
            assertThat(outcomingPayment.getCurrency()).isEqualTo(Currency.USD.name());
            assertThat(outcomingPayment.getBeneficiaryId()).isEqualTo(beneficiary.getId());
            assertThat(outcomingPayment.getPaymentProvider()).isEqualTo(PaymentProvider.LIQ_PAY);

            verify(liqPayService, times(1)).initiateOutcomingPayment(any(Transaction.class), eq(false));
            verify(dataTransService, never()).initiateOutcomingPayment(any(), anyBoolean());
        }

        @Test
        @DisplayName("initiateOutcomingPayment_fails_whenFinancialServiceIsNotFoundForProvider: throws NotFoundException when provider is not wired")
        void initiateOutcomingPayment_fails_whenFinancialServiceIsNotFoundForProvider() {
            when(dataTransService.provider()).thenReturn(PaymentProvider.DATA_TRANS);
            when(liqPayService.provider()).thenReturn(PaymentProvider.LIQ_PAY);

            TransactionDTO transactionDTO = buildTransactionDTO(PaymentProvider.GOOGLE_PAY,
                    TransactionPurpose.OTHER.name(),
                    BigInteger.valueOf(10_000),
                    Currency.USD.name(),
                    beneficiary.getId());

            assertThrows(NotFoundException.class, () ->
                    transactionService.initiateOutcomingPayment(transactionDTO, true));

            verify(dataTransService, never()).initiateOutcomingPayment(any(), anyBoolean());
            verify(liqPayService, never()).initiateOutcomingPayment(any(), anyBoolean());
        }

        @ParameterizedTest(name = "invalid DTO case #{index}")
        @MethodSource("invalidDTOMutations")
        @DisplayName("fails_onInvalidDtoFields: throws ValidationException and no adapter calls")
        void initiateOutcomingPayment_fails_whenInvalidTransaction(UnaryOperator<TransactionDTO> invalidMutationFlow) {
            long initialCount = transactionRepository.count();

            TransactionDTO transactionDTO = invalidMutationFlow.apply(buildTransactionDTO(PaymentProvider.DATA_TRANS,
                    TransactionPurpose.STOCK_OUTBOUND_REVENUE.name(),
                    BigInteger.valueOf(RandomUtils.secure().randomInt(10_000, 1_000_000)),
                    Currency.USD.name(),
                    beneficiary.getId()));

            assertThatThrownBy(() -> transactionService.initiateOutcomingPayment(transactionDTO, true))
                    .isInstanceOf(ValidationException.class);

            assertThat(transactionRepository.count()).isEqualTo(initialCount);

            verifyNoInteractions(dataTransService, liqPayService);
        }
    }

    @Nested
    @DisplayName("settlePayment(...)")
    class SettlePaymentTests {
        static Stream<UnaryOperator<TransactionDTO>> invalidDTOMutations() {
            return Stream.of(
                    transactionDTO -> {
                        transactionDTO.setTransactionId(null);
                        return transactionDTO;
                    },
                    transactionDTO -> {
                        transactionDTO.setTransactionId("");
                        return transactionDTO;
                    },
                    transactionDTO -> {
                        transactionDTO.setPaymentProvider(null);
                        return transactionDTO;
                    }
            );
        }

        @Test
        @DisplayName("settlePayment_success: delegates to provider and persists SETTLED")
        void settlePayment_success() {
            when(dataTransService.provider()).thenReturn(PaymentProvider.DATA_TRANS);
            when(liqPayService.provider()).thenReturn(PaymentProvider.LIQ_PAY);
            when(dataTransService.settlePayment(any(Transaction.class))).thenCallRealMethod();

            var reference = RandomStringUtils.secure().nextAlphanumeric(33);
            Transaction initialTransaction = transactionRepository.save(Transaction.builder()
                    .transactionId(reference)
                    .reference(reference)
                    .flowType(TransactionFlowType.CREDIT)
                    .purpose(TransactionPurpose.OTHER)
                    .status(TransactionStatus.INITIATED)
                    .amount(BigInteger.valueOf(1_000))
                    .currency(Currency.USD.name())
                    .beneficiaryId(beneficiary.getId())
                    .externalReferences(ExternalReferences.builder()
                            .reference(reference)
                            .transactionId(reference)
                            .build())
                    .paymentProvider(PaymentProvider.DATA_TRANS)
                    .build());

            when(dataTransService.settlePaymentAPICall(anyString(), any(BigInteger.class), anyString(), anyString()))
                    .thenReturn(new DataTransTransaction());

            TransactionDTO transactionDTO = TransactionDTO.builder()
                    .transactionId(initialTransaction.getTransactionId())
                    .paymentProvider(PaymentProvider.DATA_TRANS.name())
                    .build();

            Transaction updatedTransaction = transactionService.settlePayment(transactionDTO);

            assertThat(updatedTransaction.getStatus()).isEqualTo(TransactionStatus.SETTLED);
            assertThat(transactionRepository.findById(initialTransaction.getId()).orElseThrow().getStatus())
                    .isEqualTo(TransactionStatus.SETTLED);

            verify(dataTransService, times(1)).settlePayment(any(Transaction.class));
            verify(liqPayService, never()).settlePayment(any());
        }

        @Test
        @DisplayName("settlePayment_fails_whenTransactionWasNotFoundBy: throws NotFoundException when transactionId does not exist")
        void settlePayment_fails_whenTransactionWasNotFoundBy() {
            when(dataTransService.provider()).thenReturn(PaymentProvider.DATA_TRANS);
            when(liqPayService.provider()).thenReturn(PaymentProvider.LIQ_PAY);

            TransactionDTO transactionDTO = TransactionDTO.builder()
                    .transactionId(RandomStringUtils.secure().nextAlphanumeric(10))
                    .paymentProvider(PaymentProvider.DATA_TRANS.name())
                    .build();

            assertThrows(NotFoundException.class, () -> transactionService.settlePayment(transactionDTO));

            verify(dataTransService, never()).settlePayment(any());
            verify(liqPayService, never()).settlePayment(any());
        }

        @ParameterizedTest(name = "invalid DTO case #{index}")
        @MethodSource("invalidDTOMutations")
        @DisplayName("settlePayment_fails_whenInvalidTransaction: throws ValidationException and no adapter/service calls")
        void settlePayment_fails_whenInvalidTransaction(UnaryOperator<TransactionDTO> invalidMutationFlow) {
            long initialCount = transactionRepository.count();

            TransactionDTO transactionDTO = invalidMutationFlow.apply(TransactionDTO.builder()
                    .transactionId(RandomStringUtils.secure().nextAlphanumeric(24))
                    .paymentProvider(PaymentProvider.DATA_TRANS.name())
                    .build());

            assertThatThrownBy(() -> transactionService.settlePayment(transactionDTO))
                    .isInstanceOf(ValidationException.class);

            assertThat(transactionRepository.count()).isEqualTo(initialCount);

            verifyNoInteractions(dataTransService, liqPayService);
        }
    }

    @Nested
    @DisplayName("cancelPayment(...)")
    class CancelPaymentTests {
        static Stream<UnaryOperator<TransactionDTO>> invalidDTOMutations() {
            return Stream.of(
                    transactionDTO -> {
                        transactionDTO.setTransactionId(null);
                        return transactionDTO;
                    },
                    transactionDTO -> {
                        transactionDTO.setTransactionId("");
                        return transactionDTO;
                    },
                    transactionDTO -> {
                        transactionDTO.setPaymentProvider(null);
                        return transactionDTO;
                    }
            );
        }

        @Test
        @DisplayName("cancelPayment_success: sets CANCELLED and saves when status = INITIATED (no provider call)")
        void cancelPayment_success() {
            Transaction initialTransaction = transactionRepository.save(Transaction.builder()
                    .transactionId(RandomStringUtils.secure().nextAlphanumeric(24))
                    .reference(RandomStringUtils.secure().nextAlphanumeric(16))
                    .flowType(TransactionFlowType.DEBIT)
                    .purpose(TransactionPurpose.OTHER)
                    .status(TransactionStatus.INITIATED)
                    .amount(BigInteger.valueOf(5_000))
                    .currency(Currency.USD.name())
                    .beneficiaryId(beneficiary.getId())
                    .paymentProvider(PaymentProvider.LIQ_PAY)
                    .build());

            TransactionDTO transactionDTO = TransactionDTO.builder()
                    .transactionId(initialTransaction.getTransactionId())
                    .paymentProvider(PaymentProvider.LIQ_PAY.name())
                    .build();

            Transaction canceledTransaction = transactionService.cancelPayment(transactionDTO);

            assertThat(canceledTransaction.getStatus()).isEqualTo(TransactionStatus.CANCELLED);
            assertThat(transactionRepository.findById(initialTransaction.getId()).orElseThrow().getStatus())
                    .isEqualTo(TransactionStatus.CANCELLED);

            verifyNoInteractions(dataTransService, liqPayService);
        }

        @Test
        @DisplayName("cancelPayment_fails_whenTransactionIsAlreadyFinalized: throws BusinessException when status is already finalized (e.g., SETTLED)")
        void cancelPayment_fails_whenTransactionIsAlreadyFinalized() {
            Transaction settledTransaction = transactionRepository.save(Transaction.builder()
                    .transactionId(RandomStringUtils.secure().nextAlphanumeric(24))
                    .reference(RandomStringUtils.secure().nextAlphanumeric(16))
                    .flowType(TransactionFlowType.CREDIT)
                    .purpose(TransactionPurpose.OTHER)
                    .status(TransactionStatus.SETTLED)
                    .amount(BigInteger.valueOf(500_000))
                    .currency(Currency.USD.name())
                    .beneficiaryId(beneficiary.getId())
                    .paymentProvider(PaymentProvider.DATA_TRANS)
                    .build());

            TransactionDTO transactionDTO = TransactionDTO.builder()
                    .transactionId(settledTransaction.getTransactionId())
                    .paymentProvider(PaymentProvider.DATA_TRANS.name())
                    .build();

            assertThrows(BusinessException.class, () -> transactionService.cancelPayment(transactionDTO));
        }

        @ParameterizedTest(name = "invalid DTO case #{index}")
        @MethodSource("invalidDTOMutations")
        @DisplayName("cancelPayment_fails_whenInvalidTransaction: throws ValidationException and no adapter or service calls")
        void cancelPayment_fails_whenInvalidTransaction(UnaryOperator<TransactionDTO> invalidMutationFlow) {
            long initialCount = transactionRepository.count();

            TransactionDTO transactionDTO = invalidMutationFlow.apply(TransactionDTO.builder()
                    .transactionId(RandomStringUtils.secure().nextAlphanumeric(24))
                    .paymentProvider(PaymentProvider.DATA_TRANS.name())
                    .build());

            assertThatThrownBy(() -> transactionService.cancelPayment(transactionDTO))
                    .isInstanceOf(ValidationException.class);

            assertThat(transactionRepository.count()).isEqualTo(initialCount);

            verifyNoInteractions(dataTransService, liqPayService);
        }
    }
}
