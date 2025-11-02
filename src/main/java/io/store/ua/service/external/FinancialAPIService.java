package io.store.ua.service.external;

import io.store.ua.entity.Transaction;
import io.store.ua.enums.PaymentProvider;
import io.store.ua.models.data.CheckoutFinancialInformation;

public interface FinancialAPIService {
    PaymentProvider provider();

    default CheckoutFinancialInformation initiateIncomingPayment(Transaction transaction, boolean settleOnInitiation) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    default Transaction initiateOutcomingPayment(Transaction transaction, boolean settleOnInitiation) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    default Transaction settlePayment(Transaction transaction) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
