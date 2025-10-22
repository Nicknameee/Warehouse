package io.store.ua.exceptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class HttpException extends ApplicationException {
    private String content;

    public HttpException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }

    public HttpException(String message, HttpStatus httpStatus, String content) {
        super(message, httpStatus);
        this.content = content;
    }
}
