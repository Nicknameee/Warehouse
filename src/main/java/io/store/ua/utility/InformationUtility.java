package io.store.ua.utility;

import java.util.Locale;
import java.util.Optional;

public class InformationUtility {
    public static Optional<String> getCountryCode(String countryName) {
        for (String iso : Locale.getISOCountries()) {
            if (Locale.of("", iso)
                    .getDisplayCountry(Locale.ENGLISH)
                    .equalsIgnoreCase(countryName.trim())) {
                return Optional.of(iso);
            }
        }

        return Optional.empty();
    }
}
