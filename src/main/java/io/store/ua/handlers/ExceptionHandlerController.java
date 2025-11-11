package io.store.ua.handlers;

import io.store.ua.exceptions.ApplicationException;
import jakarta.validation.ValidationException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.nio.file.AccessDeniedException;
import java.util.Objects;

@RestControllerAdvice
public class ExceptionHandlerController {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleApplicationExceptions(Exception e) {
        if (e instanceof ApplicationException exception) {
            return ResponseEntity.status(exception.getStatus()).body(exception);
        } else if (e instanceof MethodArgumentNotValidException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApplicationException(
                            exception.getBindingResult().getFieldErrors().stream()
                                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                                    .filter(Objects::nonNull)
                                    .toList()
                                    .toString(),
                            HttpStatus.BAD_REQUEST));
        } else if (e instanceof BadCredentialsException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } else if (e instanceof ValidationException
                || e instanceof MissingServletRequestParameterException
                || e instanceof MissingServletRequestPartException
                || e instanceof HttpMessageNotReadableException || e instanceof HandlerMethodValidationException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApplicationException(e.getMessage(), HttpStatus.BAD_REQUEST));
        } else if (e instanceof AccessDeniedException || e instanceof AuthorizationDeniedException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApplicationException(e.getMessage(), HttpStatus.FORBIDDEN));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApplicationException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
