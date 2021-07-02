package de.ipvs.as.mbp.discovery_repository.endpoints.rest;

import de.ipvs.as.mbp.discovery_repository.error.ApiError;
import de.ipvs.as.mbp.discovery_repository.error.ApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Controller for handling exceptions and converting them into server responses that are directly sent as reply
 * to the requesting clients.
 */
@ControllerAdvice
public class ExceptionHandlerController {

    /**
     * Handles all {@link ApplicationException}s that are thrown and not handled otherwise by creating a
     * corresponding server response from the exception that is directly replied to the requesting client.
     *
     * @param exception The {@link ApplicationException} to handle
     * @return The resulting server response
     */
    @ExceptionHandler({ApplicationException.class})
    public ResponseEntity<ApiError> handleRepositoryException(ApplicationException exception) {
        //Create API error from the exception
        ApiError apiError = new ApiError(exception.getStatus(), exception.getMessage(), exception.getDetailMessages());

        //Reply with API error
        return new ResponseEntity<>(apiError, exception.getStatus());
    }

    /**
     * Handles all {@link Exception}s that are thrown and not handled otherwise by creating a
     * corresponding server response from the exception that is directly replied to the requesting client.
     *
     * @param exception The exception to handle
     * @return The resulting server response
     */
    @ExceptionHandler({Exception.class})
    public ResponseEntity<ApiError> handleException(Exception exception) {
        //Create API error from the exception
        ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());

        //Reply with API error
        return new ResponseEntity<>(apiError, apiError.getStatus());
    }
}
