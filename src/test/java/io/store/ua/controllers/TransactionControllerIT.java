package io.store.ua.controllers;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Beneficiary;
import io.store.ua.entity.Transaction;
import io.store.ua.enums.*;
import io.store.ua.models.api.data.DataTransTransaction;
import io.store.ua.models.api.external.request.LPInitiatePaymentRequestDTO;
import io.store.ua.models.api.external.response.LPResponse;
import io.store.ua.models.data.CheckoutFinancialInformation;
import io.store.ua.models.data.ExternalReferences;
import io.store.ua.models.dto.TransactionDTO;
import io.store.ua.service.external.DataTransAPIService;
import io.store.ua.service.external.LiqPayAPIService;
import io.store.ua.utility.CodeGenerator;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransactionControllerIT extends AbstractIT {
    private static final DateTimeFormatter DATE_DMY_HM = DateTimeFormatter.ofPattern("dd-MM-yyyy'At'HH:mm:ss");

    @MockitoBean
    private DataTransAPIService dataTransService;
    @MockitoBean
    private LiqPayAPIService liqPayService;

    private HttpHeaders ownerHeaders;
    private Beneficiary beneficiary;

    @BeforeAll
    void setupAuthentication() {
        ownerHeaders = generateAuthenticationHeaders();
    }

    @BeforeEach
    void seedBeneficiary() {
        beneficiary = generateBeneficiary();
    }

    private HttpHeaders generateHeaders(HttpHeaders base) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(base);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private TransactionDTO buildTransactionDTO(PaymentProvider provider,
                                               String purpose,
                                               BigInteger amount,
                                               String currency,
                                               Long beneficiaryId) {
        return TransactionDTO.builder()
                .purpose(purpose)
                .amount(amount)
                .currency(currency)
                .beneficiaryId(beneficiaryId)
                .paymentProvider(provider.name())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/transactions/findBy")
    class FindByTransactionsTests {
        @Test
        @DisplayName("findBy_success_filtersByAllParams")
        void findBy_success_filtersByAllParams() {
            LocalDateTime now = LocalDateTime.now();
            String code = CodeGenerator.TransactionCodeGenerator.generate(PaymentProvider.DATA_TRANS);

            transactionRepository.saveAll(List.of(
                    Transaction.builder()
                            .transactionId(code)
                            .reference(code)
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
                            .amount(BigInteger.valueOf(5_000))
                            .currency(Currency.EUR.name())
                            .beneficiaryId(beneficiary.getId())
                            .paymentProvider(PaymentProvider.LIQ_PAY)
                            .createdAt(now.minusDays(2))
                            .build()
            ));

            String url = UriComponentsBuilder.fromPath("/api/v1/transactions/findBy")
                    .queryParam("transactionId", code)
                    .queryParam("reference", code)
                    .queryParam("currency", Currency.USD.name())
                    .queryParam("amountFrom", 100_000)
                    .queryParam("amountTo", 300_000)
                    .queryParam("createdFrom", now.minusHours(10).format(DATE_DMY_HM))
                    .queryParam("createdTo", now.plusHours(10).format(DATE_DMY_HM))
                    .queryParam("paidFrom", now.minusHours(10).format(DATE_DMY_HM))
                    .queryParam("paidTo", now.plusHours(10).format(DATE_DMY_HM))
                    .queryParam("flowType", TransactionFlowType.CREDIT.name())
                    .queryParam("purpose", TransactionPurpose.FEE.name())
                    .queryParam("status", TransactionStatus.INITIATED.name())
                    .queryParam("beneficiaryId", beneficiary.getId())
                    .queryParam("paymentProvider", PaymentProvider.DATA_TRANS.name())
                    .queryParam("pageSize", 100)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Transaction>> response = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull()
                    .hasSize(1);
            assertThat(response.getBody().getFirst().getTransactionId())
                    .isEqualTo(code);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/transactions/incoming/initiate")
    class InitiateIncomingPaymentTests {
        @Test
        @DisplayName("initiateIncoming_success_dataTransWithAutoSettle")
        void initiateIncoming_success_dataTransWithAutoSettle() {
            when(dataTransService.provider())
                    .thenReturn(PaymentProvider.DATA_TRANS);
            when(liqPayService.provider())
                    .thenReturn(PaymentProvider.LIQ_PAY);
            when(dataTransService.initiateIncomingPayment(any(Transaction.class), eq(true)))
                    .thenCallRealMethod();

            DataTransTransaction transTransaction = DataTransTransaction.builder()
                    .transactionId(RandomStringUtils.secure().nextAlphanumeric(30))
                    .reference(RandomStringUtils.secure().nextAlphanumeric(30))
                    .build();

            when(dataTransService.initiateIncomingPaymentAPICall(anyString(), any(BigInteger.class), eq(true)))
                    .thenReturn(transTransaction);

            long beforeCount = transactionRepository.count();

            TransactionDTO transactionDTO = buildTransactionDTO(
                    PaymentProvider.DATA_TRANS,
                    TransactionPurpose.STOCK_OUTBOUND_REVENUE.name(),
                    BigInteger.valueOf(RandomUtils.secure().randomInt(100_000, 900_000)),
                    Currency.USD.name(),
                    beneficiary.getId()
            );

            String url = UriComponentsBuilder.fromPath("/api/v1/transactions/incoming/initiate")
                    .queryParam("autoSettle", true)
                    .build(true)
                    .toUriString();

            ResponseEntity<CheckoutFinancialInformation> response = restClient.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(transactionDTO, generateHeaders(ownerHeaders)),
                    CheckoutFinancialInformation.class
            );

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();
            assertThat(response.getBody().getPaymentProvider())
                    .isEqualTo(PaymentProvider.DATA_TRANS);
            assertThat(response.getBody().getTransactionId())
                    .isNotBlank();
            assertThat(response.getBody().getReference())
                    .isNotBlank();
            assertThat(transactionRepository.count())
                    .isEqualTo(beforeCount + 1);

            Transaction persisted = transactionRepository.findByTransactionId(transTransaction.getTransactionId())
                    .orElseThrow();

            assertThat(persisted.getStatus())
                    .isEqualTo(TransactionStatus.SETTLED);

            verify(dataTransService, times(1))
                    .initiateIncomingPayment(any(Transaction.class), eq(true));
            verify(liqPayService, never())
                    .initiateIncomingPayment(any(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/transactions/outgoing/initiate")
    class InitiateOutgoingPaymentTests {
        @Test
        @DisplayName("initiateOutgoing_success_liqpayWithoutAutoSettle")
        void initiateOutgoing_success_liqpayWithoutAutoSettle() {
            when(dataTransService.provider())
                    .thenReturn(PaymentProvider.DATA_TRANS);
            when(liqPayService.provider())
                    .thenReturn(PaymentProvider.LIQ_PAY);
            when(liqPayService.initiateOutcomingPayment(any(Transaction.class), eq(false)))
                    .thenCallRealMethod();
            when(liqPayService.initiateOutcomingPaymentAPICall(any(LPInitiatePaymentRequestDTO.class)))
                    .thenReturn(LPResponse.builder().status(LPResponse.Status.UNKNOWN).build());

            long beforeCount = transactionRepository.count();

            TransactionDTO transactionDTO = buildTransactionDTO(
                    PaymentProvider.LIQ_PAY,
                    TransactionPurpose.SHIPPING.name(),
                    BigInteger.valueOf(RandomUtils.secure().randomInt(100_000, 500_000)),
                    Currency.USD.name(),
                    beneficiary.getId()
            );

            String url = UriComponentsBuilder.fromPath("/api/v1/transactions/outgoing/initiate")
                    .queryParam("autoSettle", false)
                    .build(true)
                    .toUriString();

            ResponseEntity<Transaction> response = restClient.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(transactionDTO, generateHeaders(ownerHeaders)),
                    Transaction.class
            );

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();
            assertThat(response.getBody().getStatus())
                    .isEqualTo(TransactionStatus.INITIATED);
            assertThat(response.getBody().getFlowType())
                    .isEqualTo(TransactionFlowType.DEBIT);
            assertThat(transactionRepository.count())
                    .isEqualTo(beforeCount + 1);

            verify(liqPayService, times(1))
                    .initiateOutcomingPayment(any(Transaction.class), eq(false));
            verify(dataTransService, never())
                    .initiateOutcomingPayment(any(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/transactions/settle")
    class SettlePaymentTests {
        @Test
        @DisplayName("settle_success_dataTrans")
        void settle_success_dataTrans() {
            when(dataTransService.provider())
                    .thenReturn(PaymentProvider.DATA_TRANS);
            when(liqPayService.provider())
                    .thenReturn(PaymentProvider.LIQ_PAY);
            when(dataTransService.settlePayment(any(Transaction.class)))
                    .thenCallRealMethod();
            when(dataTransService.settlePaymentAPICall(anyString(), any(BigInteger.class), anyString(), anyString()))
                    .thenReturn(new DataTransTransaction());

            String reference = RandomStringUtils.secure().nextAlphanumeric(28);

            Transaction transaction = transactionRepository.save(Transaction.builder()
                    .transactionId(reference)
                    .reference(reference)
                    .flowType(TransactionFlowType.CREDIT)
                    .purpose(TransactionPurpose.OTHER)
                    .status(TransactionStatus.INITIATED)
                    .amount(BigInteger.valueOf(1_000))
                    .currency(Currency.USD.name())
                    .beneficiaryId(beneficiary.getId())
                    .paymentProvider(PaymentProvider.DATA_TRANS)
                    .externalReferences(ExternalReferences.builder()
                            .reference(reference)
                            .transactionId(reference)
                            .build())
                    .build());

            TransactionDTO transactionDTO = TransactionDTO.builder()
                    .transactionId(transaction.getTransactionId())
                    .paymentProvider(PaymentProvider.DATA_TRANS.name())
                    .build();

            String url = UriComponentsBuilder.fromPath("/api/v1/transactions/settle")
                    .build(true)
                    .toUriString();

            ResponseEntity<Transaction> response = restClient.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(transactionDTO, generateHeaders(ownerHeaders)),
                    Transaction.class
            );

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();
            assertThat(response.getBody().getStatus())
                    .isEqualTo(TransactionStatus.SETTLED);
            assertThat(transactionRepository.findById(transaction.getId())
                    .orElseThrow()
                    .getStatus())
                    .isEqualTo(TransactionStatus.SETTLED);

            verify(dataTransService, times(1))
                    .settlePayment(any(Transaction.class));
            verify(liqPayService, never())
                    .settlePayment(any());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/transactions/cancel")
    class CancelPaymentTests {
        @Test
        @DisplayName("cancel_success_initiatedLocally")
        void cancel_success_initiatedLocally() {
            Transaction transaction = transactionRepository.save(Transaction.builder()
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
                    .transactionId(transaction.getTransactionId())
                    .paymentProvider(PaymentProvider.LIQ_PAY.name())
                    .build();

            String url = UriComponentsBuilder.fromPath("/api/v1/transactions/cancel")
                    .build(true)
                    .toUriString();

            ResponseEntity<Transaction> response = restClient.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(transactionDTO, generateHeaders(ownerHeaders)),
                    Transaction.class
            );

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();
            assertThat(response.getBody().getStatus())
                    .isEqualTo(TransactionStatus.CANCELLED);
            assertThat(transactionRepository.findById(transaction.getId())
                    .orElseThrow()
                    .getStatus())
                    .isEqualTo(TransactionStatus.CANCELLED);

            verifyNoInteractions(dataTransService, liqPayService);
        }
    }
}
