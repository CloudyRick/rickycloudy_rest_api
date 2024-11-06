package dev.rickcloudy.restapi.exception.custom;

import dev.rickcloudy.restapi.exception.HttpException;
import org.springframework.http.HttpStatus;

public class FileUploadException extends HttpException {
    public FileUploadException(HttpStatus status, String message) {
        super(status, message);
    }
}
