package dev.rickcloudy.restapi.exception.custom;

import dev.rickcloudy.restapi.exception.HttpException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends HttpException {
    public EmailAlreadyExistsException(HttpStatus status, String message) {
        super(status, message);
    }
}
