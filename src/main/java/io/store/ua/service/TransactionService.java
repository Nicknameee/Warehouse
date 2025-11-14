package io.store.ua.service;

import io.store.ua.entity.Transaction;
import io.store.ua.enums.PaymentProvider;
import io.store.ua.enums.TransactionFlowType;
import io.store.ua.enums.TransactionPurpose;
import io.store.ua.enums.TransactionStatus;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.data.CheckoutFinancialInformation;
import io.store.ua.models.dto.TransactionDTO;
import io.store.ua.repository.BeneficiaryRepository;
import io.store.ua.repository.TransactionRepository;
import io.store.ua.validations.FieldValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Validated
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final TransactionAdapterService transactionAdapterService;
    private final EntityManager entityManager;
    private final FieldValidator fieldValidator;

    private static <E extends Enum<E>> E parseEnumOrThrow(String value, Class<E> type, String fieldName) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (Exception e) {
            StringBuilder allowed = new StringBuilder();

            for (E v : type.getEnumConstants()) {
                if (!allowed.isEmpty()) {
                    allowed.append(", ");
                }

                allowed.append(v.name());
            }

            throw new ValidationException("Invalid %s '%s'. Allowed values: [%s]".formatted(fieldName, value, allowed));
        }
    }

    public List<Transaction> findAll(@Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                                     @Min(value = 1, message = "A page number can't be less than 1") int page) {
        return transactionRepository.findAll(Pageable.ofSize(pageSize).withPage(page - 1)).getContent();
    }

    public List<Transaction> findBy(String transactionId,
                                    String reference,
                                    String currency,
                                    BigInteger minAmount,
                                    BigInteger maxAmount,
                                    LocalDateTime createdFrom,
                                    LocalDateTime createdTo,
                                    LocalDateTime paidFrom,
                                    LocalDateTime paidTo,
                                    String flowType,
                                    String purpose,
                                    String status,
                                    Long beneficiaryID,
                                    String paymentProvider,
                                    @Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                                    @Min(value = 1, message = "A page number can't be less than 1") int page) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Transaction> criteriaQuery = criteriaBuilder.createQuery(Transaction.class);
        Root<Transaction> root = criteriaQuery.from(Transaction.class);

        List<Predicate> predicates = new ArrayList<>();

        if (StringUtils.isNotBlank(transactionId)) {
            predicates.add(criteriaBuilder.equal(root.get(Transaction.Fields.transactionId), transactionId));
        }

        if (StringUtils.isNotBlank(reference)) {
            predicates.add(criteriaBuilder.equal(root.get(Transaction.Fields.reference), reference));
        }

        if (StringUtils.isNotBlank(currency)) {
            predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get(Transaction.Fields.currency)), currency.toLowerCase()));
        }

        if (minAmount != null && maxAmount != null) {
            predicates.add(criteriaBuilder.between(root.get(Transaction.Fields.amount), minAmount, maxAmount));
        } else if (minAmount != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(Transaction.Fields.amount), minAmount));
        } else if (maxAmount != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(Transaction.Fields.amount), maxAmount));
        }

        if (createdFrom != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(Transaction.Fields.createdAt), createdFrom));
        }
        if (createdTo != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(Transaction.Fields.createdAt), createdTo));
        }

        if (paidFrom != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(Transaction.Fields.paidAt), paidFrom));
        }
        if (paidTo != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(Transaction.Fields.paidAt), paidTo));
        }

        if (StringUtils.isNotBlank(flowType)) {
            TransactionFlowType transactionFlowType = parseEnumOrThrow(flowType, TransactionFlowType.class, Transaction.Fields.flowType);
            predicates.add(criteriaBuilder.equal(root.get(Transaction.Fields.flowType), transactionFlowType));
        }

        if (StringUtils.isNotBlank(purpose)) {
            TransactionPurpose transactionPurpose = parseEnumOrThrow(purpose, TransactionPurpose.class, Transaction.Fields.purpose);
            predicates.add(criteriaBuilder.equal(root.get(Transaction.Fields.purpose), transactionPurpose));
        }

        if (StringUtils.isNotBlank(status)) {
            TransactionStatus transactionStatus = parseEnumOrThrow(status, TransactionStatus.class, Transaction.Fields.status);
            predicates.add(criteriaBuilder.equal(root.get(Transaction.Fields.status), transactionStatus));
        }

        if (beneficiaryID != null) {
            predicates.add(criteriaBuilder.equal(root.get(Transaction.Fields.beneficiaryId), beneficiaryID));
        }

        if (StringUtils.isNotBlank(paymentProvider)) {
            PaymentProvider parsed = parseEnumOrThrow(paymentProvider, PaymentProvider.class, Transaction.Fields.paymentProvider);
            predicates.add(criteriaBuilder.equal(root.get(Transaction.Fields.paymentProvider), parsed));
        }

        criteriaQuery.select(root).where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));

        return entityManager.createQuery(criteriaQuery)
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
    }

    public void synchroniseTransaction(@NotNull TransactionDTO transactionDTO) {
        fieldValidator.validate(transactionDTO, true,
                TransactionDTO.Fields.purpose,
                TransactionDTO.Fields.flow,
                TransactionDTO.Fields.amount,
                TransactionDTO.Fields.currency,
                TransactionDTO.Fields.paymentProvider);

        PaymentProvider paymentProvider = parseEnumOrThrow(transactionDTO.getPaymentProvider(), PaymentProvider.class, TransactionDTO.Fields.paymentProvider);

        if (paymentProvider != PaymentProvider.CASH) {
            throw new ValidationException("Only cash payments can be synchronised");
        }

        TransactionFlowType transactionFlowType = parseEnumOrThrow(transactionDTO.getFlow(), TransactionFlowType.class, TransactionDTO.Fields.flow);

        switch (transactionFlowType) {
            case CREDIT:
                initiateIncomingPayment(transactionDTO, true);
                break;
            case DEBIT:
                initiateOutcomingPayment(transactionDTO, true);
                break;
            default:
                throw new ValidationException("Invalid transaction flow '%s'".formatted(transactionFlowType));
        }
    }

    public CheckoutFinancialInformation initiateIncomingPayment(@NotNull TransactionDTO transactionDTO, Boolean autoSettle) {
        fieldValidator.validate(transactionDTO, true,
                TransactionDTO.Fields.purpose,
                TransactionDTO.Fields.amount,
                TransactionDTO.Fields.currency,
                TransactionDTO.Fields.receiverFinancialAccountId,
                TransactionDTO.Fields.paymentProvider);

        if (!beneficiaryRepository.existsById(transactionDTO.getReceiverFinancialAccountId())) {
            throw new NotFoundException("Beneficiary with ID '%s' was not found".formatted(transactionDTO.getReceiverFinancialAccountId()));
        }

        PaymentProvider paymentProvider = parseEnumOrThrow(transactionDTO.getPaymentProvider(), PaymentProvider.class, TransactionDTO.Fields.paymentProvider);
        TransactionPurpose purpose = parseEnumOrThrow(transactionDTO.getPurpose(), TransactionPurpose.class, TransactionDTO.Fields.purpose);

        Transaction transaction = Transaction.builder()
                .flowType(TransactionFlowType.CREDIT)
                .purpose(purpose)
                .amount(transactionDTO.getAmount())
                .currency(transactionDTO.getCurrency())
                .beneficiaryId(transactionDTO.getReceiverFinancialAccountId())
                .paymentProvider(paymentProvider)
                .build();

        var result = transactionAdapterService.initiateIncomingPayment(transaction, autoSettle != null && autoSettle, paymentProvider);

        transactionRepository.save(transaction);

        return result;
    }

    public Transaction initiateOutcomingPayment(@NotNull TransactionDTO transactionDTO, Boolean autoSettle) {
        fieldValidator.validate(transactionDTO, true,
                TransactionDTO.Fields.purpose,
                TransactionDTO.Fields.amount,
                TransactionDTO.Fields.currency,
                TransactionDTO.Fields.receiverFinancialAccountId,
                TransactionDTO.Fields.paymentProvider);

        if (!beneficiaryRepository.existsById(transactionDTO.getReceiverFinancialAccountId())) {
            throw new NotFoundException("Beneficiary with ID '%s' was not found".formatted(transactionDTO.getReceiverFinancialAccountId()));
        }

        PaymentProvider paymentProvider = parseEnumOrThrow(transactionDTO.getPaymentProvider(), PaymentProvider.class, TransactionDTO.Fields.paymentProvider);
        TransactionPurpose purpose = parseEnumOrThrow(transactionDTO.getPurpose(), TransactionPurpose.class, TransactionDTO.Fields.purpose);

        Transaction transaction = Transaction.builder()
                .flowType(TransactionFlowType.DEBIT)
                .purpose(purpose)
                .amount(transactionDTO.getAmount())
                .currency(transactionDTO.getCurrency())
                .beneficiaryId(transactionDTO.getReceiverFinancialAccountId())
                .paymentProvider(paymentProvider)
                .build();

        transaction = transactionAdapterService.initiateOutcomingPayment(transaction,
                autoSettle != null && autoSettle,
                paymentProvider);

        return transactionRepository.save(transaction);
    }

    public Transaction settlePayment(@NotNull TransactionDTO transactionDTO) {
        fieldValidator.validate(transactionDTO, true,
                TransactionDTO.Fields.transactionId,
                TransactionDTO.Fields.paymentProvider);

        var transaction = transactionRepository.findByTransactionId(transactionDTO.getTransactionId())
                .orElseThrow(() -> new NotFoundException("Transaction with transactionID '%s' was not found".formatted(transactionDTO.getTransactionId())));

        PaymentProvider paymentProvider = parseEnumOrThrow(transactionDTO.getPaymentProvider(), PaymentProvider.class, TransactionDTO.Fields.paymentProvider);

        return transactionRepository.save(transactionAdapterService.settlePayment(transaction, paymentProvider));
    }

    public Transaction cancelPayment(@NotNull TransactionDTO transactionDTO) {
        fieldValidator.validate(transactionDTO, true,
                TransactionDTO.Fields.transactionId,
                TransactionDTO.Fields.paymentProvider);

        var transaction = transactionRepository.findByTransactionId(transactionDTO.getTransactionId())
                .orElseThrow(() -> new NotFoundException("Transaction with transactionID '%s' was not found".formatted(transactionDTO.getTransactionId())));

        if (transaction.getStatus() != TransactionStatus.INITIATED) {
            throw new BusinessException("Can't cancel payment, because transaction is already finalised");
        }

        transaction.setStatus(TransactionStatus.CANCELLED);

        return transactionRepository.save(transaction);
    }
}
