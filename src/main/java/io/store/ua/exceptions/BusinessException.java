package io.store.ua.exceptions;

import org.springframework.http.HttpStatus;

public class BusinessException extends ApplicationException {
  public BusinessException(String message) {
    super(message, HttpStatus.CONFLICT);
  }
}
