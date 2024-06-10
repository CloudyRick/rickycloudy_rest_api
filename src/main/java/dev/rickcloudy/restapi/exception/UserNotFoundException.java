package dev.rickcloudy.restapi.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends HttpException {
    public UserNotFoundException(HttpStatus status, String message) {
        super(status, message);
    }
}
