package de.ipvs.as.mbp.discovery_repository.error;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Collection;

/**
 * Objects of this class represent generic error messages for wrapping server-side errors and exceptions in a way
 * that can be handled by the client. Each {@link ApiError} consists out of a {@link HttpStatus} code, a timestamp,
 * a general message describing the error and an optional {@link Collection} of detail messages.
 */
public class ApiError {
    private HttpStatus status;
    private int statusCode;
    private String message;
    private Collection<?> detailMessages;
    private final Instant timestamp = Instant.now();

    public ApiError(HttpStatus status, String message) {
        setStatus(status);
        setMessage(message);
    }

    public ApiError(HttpStatus status, String message, Collection<?> detailMessages) {
        this(status, message);
        setDetailMessages(detailMessages);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ApiError setStatus(HttpStatus status) {
        //Sanity check
        if (status == null) {
            throw new IllegalArgumentException("The status must not be null.");
        }

        this.status = status;
        this.statusCode = status.value();
        return this;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public ApiError setMessage(String message) {
        //Sanity check
        if ((message == null) || (message.isEmpty())) {
            throw new IllegalArgumentException("The message must not be null or empty.");
        }

        this.message = message;
        return this;
    }

    public Collection<?> getDetailMessages() {
        return detailMessages;
    }

    public ApiError setDetailMessages(Collection<?> detailMessages) {
        this.detailMessages = detailMessages;
        return this;
    }

    public String getTimestamp() {
        return timestamp.toString();
    }
}
