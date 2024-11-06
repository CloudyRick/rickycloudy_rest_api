package dev.rickcloudy.restapi.exception.custom;

import dev.rickcloudy.restapi.exception.HttpException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends HttpException {
    public UserNotFoundException(HttpStatus status, String message) {
        super(status, message);
    }
}
