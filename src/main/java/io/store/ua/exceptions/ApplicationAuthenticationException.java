package io.store.ua.exceptions;

import org.springframework.http.HttpStatus;

public class ApplicationAuthenticationException extends ApplicationException {
    public ApplicationAuthenticationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
