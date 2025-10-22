package io.store.ua.exceptions;

import org.springframework.http.HttpStatus;

public class ExternalException extends ApplicationException {
    public ExternalException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
