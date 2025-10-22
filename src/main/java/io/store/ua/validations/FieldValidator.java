package io.store.ua.validations;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FieldValidator {
    private final Validator validator;

    private static Object readField(Object bean, String fieldName) {
        try {
            Field field = findField(bean.getClass(), fieldName);
            field.setAccessible(true);

            Object value = field.get(bean);
            field.setAccessible(false);

            return value;
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot read field: %s".formatted(fieldName), e);
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> aClass = type;
        while (aClass != null) {
            try {
                return aClass.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                aClass = aClass.getSuperclass();
            }
        }

        throw new NoSuchFieldException(name);
    }

    private static String joinMessages(Set<? extends ConstraintViolation<?>> v) {
        return v.stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining("; "));
    }

    @SuppressWarnings("rawtypes")
    private void validateValueObject(Object value) {
        List<String> errors = new ArrayList<>();

        if (value instanceof Iterable it) {
            for (Object el : it) {
                if (el == null) continue;
                var vs = validator.validate(el);
                if (!vs.isEmpty()) errors.add(joinMessages(vs));
            }
        } else if (value instanceof Map map) {
            for (Object el : map.values()) {
                if (el == null) continue;
                var vs = validator.validate(el);
                if (!vs.isEmpty()) errors.add(joinMessages(vs));
            }
        } else {
            var vs = validator.validate(value);
            if (!vs.isEmpty()) errors.add(joinMessages(vs));
        }

        if (!errors.isEmpty()) throw new ValidationException(String.join("; ", errors));
    }

    public void validateObjects(Object bean, boolean required, String... fields) {
        for (String field : fields) {
            validateObject(bean, field, required);
        }
    }

    public void validateObject(Object bean, String field, boolean required) {
        Objects.requireNonNull(bean);
        Objects.requireNonNull(field);

        Object value = readField(bean, field);
        if (value == null) {
            if (required) throw new ValidationException(field + ": is required");

        } else {
            Set<ConstraintViolation<Object>> violations = validator.validateProperty(bean, field);

            if (!violations.isEmpty()) {
                throw new ValidationException(joinMessages(violations));
            }

            validateValueObject(value);
        }
    }

    public void validate(Object bean, boolean required, String... fields) {
        for (String field : fields) {
            validate(bean, field, required);
        }
    }

    public void validate(Object bean, String field, boolean required) {
        Objects.requireNonNull(bean, "Object is null");
        Objects.requireNonNull(field, "Field name is null");

        Object value = readField(bean, field);

        if (value == null) {
            if (required) {
                throw new ValidationException("%s: is required".formatted(field));
            }
        } else {
            Set<ConstraintViolation<Object>> violations = validator.validateProperty(bean, field);

            if (!violations.isEmpty()) {
                throw new ValidationException(joinMessages(violations));
            }
        }
    }
}
