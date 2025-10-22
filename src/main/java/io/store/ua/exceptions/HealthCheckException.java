package io.store.ua.exceptions;

import org.springframework.http.HttpStatus;

public class HealthCheckException extends ApplicationException {
    public HealthCheckException() {
        super("Health Check Fail", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public HealthCheckException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
