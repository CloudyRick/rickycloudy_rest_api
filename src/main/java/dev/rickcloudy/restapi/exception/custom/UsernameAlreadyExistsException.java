package dev.rickcloudy.restapi.exception.custom;

import dev.rickcloudy.restapi.exception.HttpException;
import org.springframework.http.HttpStatus;

public class UsernameAlreadyExistsException extends HttpException {
    public UsernameAlreadyExistsException(HttpStatus status, String message) {
        super(status, message);
    }
}
