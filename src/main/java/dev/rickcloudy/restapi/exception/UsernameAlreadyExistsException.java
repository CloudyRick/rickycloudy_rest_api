package dev.rickcloudy.restapi.exception;

import org.springframework.http.HttpStatus;

public class UsernameAlreadyExistsException extends HttpException{
    public UsernameAlreadyExistsException(HttpStatus status, String message) {
        super(status, message);
    }
}
