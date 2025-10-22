package io.store.ua.handlers;

import io.store.ua.exceptions.ApplicationException;
import io.store.ua.models.api.Response;
import java.util.Objects;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionHandlerController {
  @ExceptionHandler(Exception.class)
  public Response handleApplicationExceptions(Exception e) {
    if (e instanceof ApplicationException exception) {
      return Response.of(exception.getStatus()).exception(exception);
    } else if (e instanceof MethodArgumentNotValidException exception) {
      return Response.of(HttpStatus.BAD_REQUEST)
          .exception(
              new ApplicationException(
                  exception.getBindingResult().getFieldErrors().stream()
                      .map(DefaultMessageSourceResolvable::getDefaultMessage)
                      .filter(Objects::nonNull)
                      .toList()
                      .toString(),
                  HttpStatus.BAD_REQUEST));
    }

    return Response.of(HttpStatus.INTERNAL_SERVER_ERROR)
        .exception(new ApplicationException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR))
        .build();
  }
}
