package dev.rickcloudy.restapi.exception;

import org.springframework.http.HttpStatus;

public class BlogPostNotFoundException extends HttpException {
    public BlogPostNotFoundException(HttpStatus status, String message) {
        super(status, message);
    }
}
