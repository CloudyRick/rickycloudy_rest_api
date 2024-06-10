package dev.rickcloudy.restapi.exception;

import org.springframework.http.HttpStatus;

import java.io.Serial;

public class HttpException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;
    private final HttpStatus status;

    public HttpException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getHttpStatus() {
        return this.status;
    }
}
