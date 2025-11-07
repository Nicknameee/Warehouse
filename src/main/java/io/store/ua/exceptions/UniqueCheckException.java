package io.store.ua.exceptions;

import org.springframework.http.HttpStatus;

public class UniqueCheckException extends ApplicationException {
    public UniqueCheckException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
