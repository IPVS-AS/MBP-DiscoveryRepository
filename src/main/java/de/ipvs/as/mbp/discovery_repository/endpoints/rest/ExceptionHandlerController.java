package de.ipvs.as.mbp.discovery_repository.endpoints.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Controller for handling exceptions and converting them into server responses.
 */
@ControllerAdvice
public class ExceptionHandlerController {
    /**
     * Handles all exceptions that are not handled otherwise.
     *
     * @param exception The exception to handle
     * @return The server response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception exception) {
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
