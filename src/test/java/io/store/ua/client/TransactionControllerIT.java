package io.store.ua.client;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Beneficiary;
import io.store.ua.entity.Transaction;
import io.store.ua.enums.*;
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

    private TransactionDTO buildTransactionDTO(PaymentProvider provider,
                                               String purpose,
                                               BigInteger amount,
                                               String currency,
                                               Long beneficiaryId) {
        return TransactionDTO.builder()
                .purpose(purpose)
                .status(TransactionStatus.INITIATED.name())
                .amount(amount)
                .currency(currency)
                .receiverFinancialAccountId(beneficiaryId)
                .paymentProvider(provider)
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/transactions/findAll")
    class FindAllTransactionsTests {
        @Test
        @DisplayName("findAll_success_returnsPaginatedList: returns paginated list")
        void findAll_success_returnsPaginatedList() {
            Transaction transaction = transactionRepository.save(Transaction.builder()
                    .transactionId(RandomStringUtils.secure().nextAlphanumeric(24))
                    .reference(RandomStringUtils.secure().nextAlphanumeric(16))
                    .flowType(TransactionFlowType.CREDIT)
                    .purpose(TransactionPurpose.OTHER)
                    .status(TransactionStatus.INITIATED)
                    .amount(BigInteger.valueOf(100_000))
                    .currency(Currency.USD.name())
                    .beneficiaryId(beneficiary.getId())
                    .paymentProvider(PaymentProvider.DATA_TRANS)
                    .build());

            String firstPageUrl = UriComponentsBuilder.fromPath("/api/v1/transactions/findAll")
                    .queryParam("pageSize", 1)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Transaction>> firstPage = restClient.exchange(
                    firstPageUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(firstPage.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(firstPage.getBody())
                    .isNotNull()
                    .hasSize(1);
            assertThat(firstPage.getBody()
                    .getFirst()
                    .getTransactionId())
                    .isNotNull()
                    .isEqualTo(transaction.getTransactionId());

            String invalidUrl = UriComponentsBuilder.fromPath("/api/v1/transactions/findAll")
                    .queryParam("pageSize", 0)
                    .queryParam("page", -1)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(invalidUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/transactions/findBy")
    class FindByTransactionsTests {
        @Test
        @DisplayName("findBy_success_filtersByAll: filters by id/ref/currency/amount/time/enums/provider")
        void findBy_success_filtersByAll() {
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
                            .build()));

            String url = UriComponentsBuilder.fromPath("/api/v1/transactions/findBy")
                    .queryParam("transaction_id", code)
                    .queryParam("reference", code)
                    .queryParam("currency", Currency.USD.name().toLowerCase())
                    .queryParam("min_amount", 100_000)
                    .queryParam("max_amount", 300_000)
                    .queryParam("created_from", now.minusHours(10).format(DATE_DMY_HM))
                    .queryParam("created_to", now.plusHours(10).format(DATE_DMY_HM))
                    .queryParam("paid_from", now.minusHours(10).format(DATE_DMY_HM))
                    .queryParam("paid_to", now.plusHours(10).format(DATE_DMY_HM))
                    .queryParam("flow_type", TransactionFlowType.CREDIT.name())
                    .queryParam("purpose", TransactionPurpose.FEE.name())
                    .queryParam("status", TransactionStatus.INITIATED.name())
                    .queryParam("beneficiary_id", beneficiary.getId())
                    .queryParam("payment_provider", PaymentProvider.DATA_TRANS.name())
                    .queryParam("pageSize", 100)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Transaction>> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull()
                    .hasSize(1);
            assertThat(response.getBody()
                    .getFirst()
                    .getTransactionId())
                    .isEqualTo(code);
        }

        @Test
        @DisplayName("findBy_success_emptyWhenNoFiltersMatch: empty when no filters match")
        void findBy_success_emptyWhenNoFiltersMatch() {
            String url = UriComponentsBuilder.fromPath("/api/v1/transactions/findBy")
                    .queryParam("pageSize", 50)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Transaction>> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull()
                    .isEmpty();
        }

        @Test
        @DisplayName("findBy_fails_invalidPagination: invalid pagination → 400")
        void findBy_fails_invalidPagination() {
            String url = UriComponentsBuilder.fromPath("/api/v1/transactions/findBy")
                    .queryParam("pageSize", 0)
                    .queryParam("page", 0)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("POST /api/v1/transactions/incoming/initiate")
    class InitiateIncomingPaymentTests {
        @Test
        @DisplayName("initiateIncoming_success_dataTransWithAutoSettle: DATA_TRANS with auto_settle=true → SETTLED and checkout info")
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

            TransactionDTO transactionDTO = buildTransactionDTO(PaymentProvider.DATA_TRANS,
                    TransactionPurpose.STOCK_OUTBOUND_REVENUE.name(),
                    BigInteger.valueOf(RandomUtils.secure().randomInt(100_000, 900_000)),
                    Currency.USD.name(),
                    beneficiary.getId());

            String url = UriComponentsBuilder.fromPath("/api/v1/transactions/incoming/initiate")
                    .queryParam("auto_settle", true)
                    .build(true)
                    .toUriString();

            ResponseEntity<CheckoutFinancialInformation> response = restClient.exchange(url,
                    HttpMethod.POST,
                    new HttpEntity<>(transactionDTO, ownerHeaders),
                    CheckoutFinancialInformation.class);

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

            assertThat(transactionRepository.count()).isEqualTo(beforeCount + 1);

            Transaction transaction = transactionRepository.findByTransactionId(transTransaction.getTransactionId())
                    .orElseThrow();

            assertThat(transaction.getStatus())
                    .isEqualTo(TransactionStatus.SETTLED);

            verify(dataTransService, times(1))
                    .initiateIncomingPayment(any(Transaction.class), eq(true));
            verify(liqPayService, never())
                    .initiateIncomingPayment(any(), anyBoolean());
        }

        @Test
        @DisplayName("initiateIncoming_success_liqpayWithoutAutoSettle: LIQ_PAY with auto_settle=false → INITIATED and checkout info")
        void initiateIncoming_success_liqpayWithoutAutoSettle() {
            when(dataTransService.provider())
                    .thenReturn(PaymentProvider.DATA_TRANS);
            when(liqPayService.provider())
                    .thenReturn(PaymentProvider.LIQ_PAY);
            when(liqPayService.initiateIncomingPayment(any(Transaction.class), eq(false)))
                    .thenCallRealMethod();
            when(liqPayService.initiateIncomingPaymentAPICall(any(LPInitiatePaymentRequestDTO.class)))
                    .thenReturn(new LPInitiatePaymentResponse());

            long beforeCount = transactionRepository.count();

            TransactionDTO transactionDTO = buildTransactionDTO(PaymentProvider.LIQ_PAY,
                    TransactionPurpose.FEE.name(),
                    BigInteger.valueOf(RandomUtils.secure().randomInt(50_000, 300_000)),
                    Currency.EUR.name(),
                    beneficiary.getId());

            String url = UriComponentsBuilder.fromPath("/api/v1/transactions/incoming/initiate")
                    .queryParam("auto_settle", false)
                    .build(true)
                    .toUriString();

            ResponseEntity<CheckoutFinancialInformation> response = restClient.exchange(url,
                    HttpMethod.POST,
                    new HttpEntity<>(transactionDTO, ownerHeaders),
                    CheckoutFinancialInformation.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();
            assertThat(response.getBody().getPaymentProvider())
                    .isEqualTo(PaymentProvider.LIQ_PAY);
            assertThat(transactionRepository.count())
                    .isEqualTo(beforeCount + 1);

            verify(liqPayService, times(1))
                    .initiateIncomingPayment(any(Transaction.class), eq(false));
            verify(dataTransService, never())
                    .initiateIncomingPayment(any(), anyBoolean());
        }

        @Test
        @DisplayName("initiateIncoming_fails_invalidDTO: invalid DTO → 4xx and no provider calls")
        void initiateIncoming_fails_invalidDTO() {
            TransactionDTO transactionDTO = TransactionDTO.builder()
                    .currency(Currency.USD.name())
                    .build();

            String url = UriComponentsBuilder.fromPath("/api/v1/transactions/incoming/initiate")
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(url,
                    HttpMethod.POST,
                    new HttpEntity<>(transactionDTO, ownerHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();

            verifyNoInteractions(dataTransService, liqPayService);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/transactions/outgoing/initiate")
    class InitiateOutgoingPaymentTests {
        @Test
        @DisplayName("initiateOutcoming_success_liqpayWithoutAutoSettle: delegates to LIQ_PAY (auto_settle=false), persists INITIATED")
        void initiateOutcoming_success_liqpayWithoutAutoSettle() {
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
                    .queryParam("auto_settle", false)
                    .build(true)
                    .toUriString();

            ResponseEntity<Transaction> response = restClient.exchange(url,
                    HttpMethod.POST,
                    new HttpEntity<>(transactionDTO, ownerHeaders),
                    Transaction.class);

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

        @Test
        @DisplayName("initiateOutcoming_fails_invalidDTO: invalid DTO → 4xx and no provider calls")
        void initiateOutcoming_fails_invalidDTO() {
            TransactionDTO transactionDTO = TransactionDTO.builder()
                    .paymentProvider(PaymentProvider.DATA_TRANS)
                    .currency("usd")
                    .build();

            String url = UriComponentsBuilder.fromPath("/api/v1/transactions/outgoing/initiate")
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(url,
                    HttpMethod.POST,
                    new HttpEntity<>(transactionDTO, ownerHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();

            verifyNoInteractions(dataTransService, liqPayService);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/transactions/settle")
    class SettlePaymentTests {
        @Test
        @DisplayName("settle_success_dataTrans: settles via DATA_TRANS → SETTLED")
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
                    .paymentProvider(PaymentProvider.DATA_TRANS)
                    .build();

            String url = UriComponentsBuilder.fromPath("/api/v1/transactions/settle")
                    .build(true)
                    .toUriString();

            ResponseEntity<Transaction> response = restClient.exchange(url,
                    HttpMethod.POST,
                    new HttpEntity<>(transactionDTO, ownerHeaders),
                    Transaction.class);

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

        @Test
        @DisplayName("settle_fails_unknownTransactionId: unknown transactionId → 4xx")
        void settle_fails_unknownTransactionId() {
            when(dataTransService.provider())
                    .thenReturn(PaymentProvider.DATA_TRANS);
            when(liqPayService.provider())
                    .thenReturn(PaymentProvider.LIQ_PAY);

            TransactionDTO transactionDTO = TransactionDTO.builder()
                    .transactionId(RandomStringUtils.secure().nextAlphanumeric(20))
                    .paymentProvider(PaymentProvider.DATA_TRANS)
                    .build();

            String url = UriComponentsBuilder.fromPath("/api/v1/transactions/settle")
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(url,
                    HttpMethod.POST,
                    new HttpEntity<>(transactionDTO, ownerHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
            verifyNoMoreInteractions(dataTransService, liqPayService);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/transactions/cancel")
    class CancelPaymentTests {
        @Test
        @DisplayName("cancel_success_initiatedLocally: cancels INITIATED locally (no provider calls)")
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
                    .paymentProvider(PaymentProvider.LIQ_PAY)
                    .build();

            String url = UriComponentsBuilder.fromPath("/api/v1/transactions/cancel")
                    .build(true)
                    .toUriString();

            ResponseEntity<Transaction> response = restClient.exchange(url,
                    HttpMethod.POST,
                    new HttpEntity<>(transactionDTO, ownerHeaders),
                    Transaction.class);

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

        @Test
        @DisplayName("cancel_fails_finalizedTransaction: finalized transaction → 4xx")
        void cancel_fails_finalizedTransaction() {
            Transaction settled = transactionRepository.save(Transaction.builder()
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
                    .transactionId(settled.getTransactionId())
                    .paymentProvider(PaymentProvider.DATA_TRANS)
                    .build();

            String url = UriComponentsBuilder.fromPath("/api/v1/transactions/cancel")
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(url,
                    HttpMethod.POST,
                    new HttpEntity<>(transactionDTO, ownerHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }
}
