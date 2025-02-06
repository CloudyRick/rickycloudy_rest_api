package dev.rickcloudy.restapi.exception.custom;

import dev.rickcloudy.restapi.exception.HttpException;
import org.springframework.http.HttpStatus;

public class NotFoundException extends HttpException {
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
