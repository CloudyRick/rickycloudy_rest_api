package dev.rickcloudy.restapi.exception.custom;

import dev.rickcloudy.restapi.exception.HttpException;
import org.springframework.http.HttpStatus;

public class AuthenticationException extends HttpException {
    public AuthenticationException (String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
