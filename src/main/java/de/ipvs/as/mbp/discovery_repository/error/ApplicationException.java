package de.ipvs.as.mbp.discovery_repository.error;

import org.springframework.http.HttpStatus;

import java.util.Collection;

/**
 * Generic exception for basically all application-side errors that need to be reported to the client.
 */
public class ApplicationException extends RuntimeException {

    private HttpStatus status;
    private Collection<?> detailMessages;


    public ApplicationException(HttpStatus status) {
        super();
        setStatus(status);
    }

    public ApplicationException(HttpStatus status, String message) {
        super(message);
        setStatus(status);
    }

    public ApplicationException(HttpStatus status, String message, Collection<?> detailMessages) {
        this(status, message);
        setDetailMessages(detailMessages);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Collection<?> getDetailMessages() {
        return detailMessages;
    }

    private void setStatus(HttpStatus status) {
        //Sanity check
        if (status == null) {
            throw new IllegalArgumentException("The status must not be null.");
        }

        this.status = status;
    }

    private void setDetailMessages(Collection<?> detailMessages) {
        this.detailMessages = detailMessages;
    }
}