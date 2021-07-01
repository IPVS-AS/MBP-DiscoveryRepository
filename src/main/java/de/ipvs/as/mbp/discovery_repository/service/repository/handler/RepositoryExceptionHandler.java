package de.ipvs.as.mbp.discovery_repository.service.repository.handler;

/**
 * Handler for exceptions that are thrown during the communication between the client and the repository.
 */
public interface RepositoryExceptionHandler {
    /**
     * Handles an exception that was thrown during the communication between the client and the repository.
     *
     * @param exception The exception to handle
     */
    void handleException(Exception exception);
}
