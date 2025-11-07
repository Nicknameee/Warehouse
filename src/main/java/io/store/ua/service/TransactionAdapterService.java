package io.store.ua.service;

import io.store.ua.entity.Transaction;
import io.store.ua.enums.PaymentProvider;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.data.CheckoutFinancialInformation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TransactionAdapterService {
    private final List<FinancialAPIService> financialAPIServices;

    public CheckoutFinancialInformation initiateIncomingPayment(Transaction transaction, boolean settleOnInitiation, PaymentProvider provider) {
        FinancialAPIService financialAPIService = financialAPIServices.stream()
                .filter(service -> service.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No financial service found for specified payment provider!"));

        return financialAPIService.initiateIncomingPayment(transaction, settleOnInitiation);
    }

    public Transaction initiateOutcomingPayment(Transaction transaction, boolean settleOnInitiation, PaymentProvider provider) {
        FinancialAPIService financialAPIService = financialAPIServices.stream()
                .filter(service -> service.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No financial service found for specified payment provider!"));

        return financialAPIService.initiateOutcomingPayment(transaction, settleOnInitiation);
    }

    public Transaction settlePayment(Transaction transaction, PaymentProvider provider) {
        FinancialAPIService financialAPIService = financialAPIServices.stream()
                .filter(service -> service.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No financial service found for specified payment provider!"));

        return financialAPIService.settlePayment(transaction);
    }
}
