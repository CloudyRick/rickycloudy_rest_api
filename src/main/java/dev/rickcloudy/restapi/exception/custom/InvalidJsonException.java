package dev.rickcloudy.restapi.exception.custom;

import dev.rickcloudy.restapi.exception.HttpException;
import org.springframework.http.HttpStatus;

public class InvalidJsonException extends HttpException {
    public InvalidJsonException(HttpStatus status, String message) {super(status, message);}
}
