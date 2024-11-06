package dev.rickcloudy.restapi.exception.custom;

import dev.rickcloudy.restapi.exception.HttpException;
import org.springframework.http.HttpStatus;

public class BlogPostNotFoundException extends HttpException {
    public BlogPostNotFoundException(HttpStatus status, String message) {
        super(status, message);
    }
}
