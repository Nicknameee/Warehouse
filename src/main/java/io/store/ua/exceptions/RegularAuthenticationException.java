package io.store.ua.exceptions;

import org.springframework.http.HttpStatus;

public class RegularAuthenticationException extends ApplicationException {
    public RegularAuthenticationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
