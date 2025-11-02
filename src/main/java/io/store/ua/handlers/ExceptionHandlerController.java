package io.store.ua.handlers;

import io.store.ua.exceptions.ApplicationException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

@RestControllerAdvice
public class ExceptionHandlerController {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleApplicationExceptions(Exception e) {
        if (e instanceof ApplicationException exception) {
            return ResponseEntity.status(exception.getStatus()).body(exception);
        } else if (e instanceof MethodArgumentNotValidException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(
                            new ApplicationException(
                                    exception.getBindingResult().getFieldErrors().stream()
                                            .map(DefaultMessageSourceResolvable::getDefaultMessage)
                                            .filter(Objects::nonNull)
                                            .toList()
                                            .toString(),
                                    HttpStatus.BAD_REQUEST));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApplicationException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
