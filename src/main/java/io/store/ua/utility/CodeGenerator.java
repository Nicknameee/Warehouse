package io.store.ua.utility;

import io.store.ua.models.data.Address;
import io.store.ua.models.dto.WarehouseDTO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.StringJoiner;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CodeGenerator {
    private static final RandomStringUtils randomStringUtils = RandomStringUtils.secure();

    public static String generate(int length) {
        return randomStringUtils.nextAlphanumeric(length);
    }

    public static class TransactionCodeGenerator {
        public static class LiqPay {
            public static String generate() {
                return "LiqPay-%s-%s".formatted(LocalDate.now().toString(), randomStringUtils.nextAlphanumeric(10).toUpperCase());
            }
        }
    }

    public static class StockCodeGenerator {
        public static String generate() {
            final int length = 5;
            return ("%s-%s-%s-%s").formatted(randomStringUtils.nextAlphanumeric(length),
                    randomStringUtils.nextAlphanumeric(length),
                    randomStringUtils.nextAlphanumeric(length),
                    randomStringUtils.nextAlphanumeric(length));
        }
    }

    public static class WarehouseCodeGenerator {
        private static final String HASH_ALGORITHM = "SHA-256";

        public static String generate(WarehouseDTO warehouseDto) {
            if (warehouseDto == null || warehouseDto.getAddress() == null) {
                throw new IllegalArgumentException("WarehouseDTO and Address must not be null");
            }

            return sha256Hex(buildCanonicalKey(warehouseDto));
        }

        private static String buildCanonicalKey(WarehouseDTO warehouseDto) {
            Address address = warehouseDto.getAddress();

            return new StringJoiner("_")
                    .add(checkString(warehouseDto.getName()))
                    .add(checkString(address.getBuilding()))
                    .add(checkString(address.getStreet()))
                    .add(checkString(address.getCity()))
                    .add(checkString(address.getState()))
                    .add(checkString(address.getCountry()))
                    .add(checkString(address.getPostalCode()))
                    .add(checkString(address.getLatitude().toPlainString()))
                    .add(checkString(address.getLongitude().toPlainString()))
                    .toString();
        }

        private static String sha256Hex(String input) {
            try {
                MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
                byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

                return HexFormat.of().formatHex(hash);
            } catch (Exception e) {
                throw new IllegalStateException("%s unavailable".formatted(HASH_ALGORITHM), e);
            }
        }

        private static String checkString(String input) {
            return StringUtils.isBlank(input) ? StringUtils.EMPTY : input;
        }
    }
}

