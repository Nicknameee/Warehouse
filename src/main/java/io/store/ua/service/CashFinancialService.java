package io.store.ua.service;

import io.store.ua.entity.Transaction;
import io.store.ua.enums.PaymentProvider;
import io.store.ua.enums.TransactionStatus;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.models.data.CheckoutFinancialInformation;
import io.store.ua.utility.CodeGenerator;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class CashFinancialService implements FinancialAPIService {
    @Override
    public PaymentProvider provider() {
        return PaymentProvider.CASH;
    }

    @Override
    public CheckoutFinancialInformation initiateIncomingPayment(Transaction transaction, boolean settleOnInitiation) {
        if (!settleOnInitiation) {
            throw new BusinessException("Cash payments should be settled automatically!");
        }

        String reference = CodeGenerator.TransactionCodeGenerator.generate(PaymentProvider.CASH);
        transaction.setReference(reference);
        transaction.setTransactionId(reference);
        transaction.setCreatedAt(LocalDateTime.now(Clock.systemUTC()));
        transaction.setStatus(TransactionStatus.SETTLED);
        transaction.setPaidAt(LocalDateTime.now(Clock.systemUTC()));
        transaction.setPaymentProvider(PaymentProvider.CASH);

        return CheckoutFinancialInformation.builder()
                .paymentProvider(PaymentProvider.CASH)
                .transactionId(reference)
                .reference(reference)
                .build();
    }

    @Override
    public Transaction initiateOutcomingPayment(Transaction transaction, boolean settleOnInitiation) {
        if (!settleOnInitiation) {
            throw new BusinessException("Cash payments should be settled automatically!");
        }

        String reference = CodeGenerator.TransactionCodeGenerator.generate(PaymentProvider.CASH);
        transaction.setReference(reference);
        transaction.setTransactionId(reference);
        transaction.setCreatedAt(LocalDateTime.now(Clock.systemUTC()));
        transaction.setStatus(TransactionStatus.SETTLED);
        transaction.setPaidAt(LocalDateTime.now(Clock.systemUTC()));
        transaction.setPaymentProvider(PaymentProvider.CASH);

        return transaction;
    }

    @Override
    public Transaction settlePayment(Transaction transaction) {
        if (transaction.getStatus() != TransactionStatus.INITIATED) {
            throw new BusinessException("Transaction is already finalized");
        }

        transaction.setStatus(TransactionStatus.SETTLED);
        transaction.setPaidAt(LocalDateTime.now(Clock.systemUTC()));

        return transaction;
    }
}
