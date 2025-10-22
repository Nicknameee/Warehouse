package io.store.ua.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class RegularObjectMapper extends ObjectMapper {
    public static final RegularObjectMapper INSTANCE = new RegularObjectMapper();

    private RegularObjectMapper() {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
        registerModule(new JavaTimeModule());
    }

    public static byte[] writeToBytes(Object object) throws JsonProcessingException {
        return INSTANCE.writeValueAsBytes(object);
    }

    public static String writeToString(Object object) throws JsonProcessingException {
        return INSTANCE.writeValueAsString(object);
    }

    public static <T> T read(String value, Class<T> type) throws JsonProcessingException {
        return INSTANCE.readValue(value, type);
    }
}
