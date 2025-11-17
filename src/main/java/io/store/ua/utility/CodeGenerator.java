package io.store.ua.utility;

import io.store.ua.enums.PaymentProvider;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CodeGenerator {
    private static final RandomStringUtils randomStringUtils = RandomStringUtils.secure();

    public static String generate(int length) {
        return randomStringUtils.nextAlphanumeric(length).toUpperCase();
    }

    public static class TransactionCodeGenerator {
        public static String generate(PaymentProvider paymentProvider) {
            return "TRX-%s-%s-%s".formatted(LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")),
                    randomStringUtils.nextAlphanumeric(15).toUpperCase(),
                    paymentProvider.getCode()).toUpperCase();
        }
    }

    public static class StockCodeGenerator {
        public static String generate() {
            final int length = 5;
            return ("%s-%s-%s-%s").formatted(randomStringUtils.nextAlphanumeric(length),
                    randomStringUtils.nextAlphanumeric(length),
                    randomStringUtils.nextAlphanumeric(length),
                    randomStringUtils.nextAlphanumeric(length)).toUpperCase();
        }
    }

    public static class ShipmentCodeGenerator {
        public static String generate() {
            return "SH-%s-%s".formatted(LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")), randomStringUtils.nextAlphanumeric(15).toUpperCase()).toUpperCase();
        }
    }

    public static class KafkaCodeGenerator {
        public static String generate(String arg) {
            return "%s-%s".formatted(LocalDateTime.now(Clock.systemUTC()), arg).toUpperCase();
        }
    }

    public static class WarehouseCodeGenerator {

        public static String generate() {
            return "WH-%s".formatted(RandomStringUtils.secure().nextAlphanumeric(8).toUpperCase());
        }
    }
}

