package io.store.ua.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PaymentProvider {
    GOOGLE_PAY("G_PAY"),
    DATA_TRANS("D_TRX"),
    LIQ_PAY("L_PAY"),
    CASH("C");

    @Getter
    private final String code;
}
