package de.ipvs.as.mbp.discovery_repository.service.repository;

import de.ipvs.as.mbp.discovery_repository.service.repository.handler.RepositoryExceptionHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public interface RepositoryClient {
    /**
     * Establishes a connection to the repository that is available at a given host address with a given port by
     * using a given username and password
     *
     * @param hostAddress    The host address of the messaging broker
     * @param port           The port of the messaging broker
     * @param username       The username to use
     * @param password       The password to use
     * @param collectionName The name that is supposed to be used for the repository collection in which the documents
     *                       are stored
     */
    void connect(String hostAddress, int port, String username, String password, String collectionName);

    /**
     * Gracefully disconnects from the repository in case a connection was previously established.
     */
    void disconnect();

    /**
     * Disconnects and destroys the client such that all allocated resources are released.
     */
    void close();

    /**
     * Returns whether there is currently an active connection to the repository.
     *
     * @return True, if a connection exists; false otherwise
     */
    boolean isConnected();

    /**
     * Inserts a document, given as {@link JSONObject}, into the repository and returns the identifier under which
     * it was stored.
     *
     * @param document The document to insert
     * @return The identifier under which the document was stored
     */
    String insertDocument(JSONObject document);

    /**
     * Retrieves the document from the repository that matches the given identifier as {@link JSONObject} or null
     * if the document cannot be found.
     *
     * @param id The identifier of the document to retrieve
     * @return The document as {@link JSONObject} or null
     */
    JSONObject getDocument(String id);

    /**
     * Updates a document with a certain identifier in the repository by replacing it with a new document.
     *
     * @param id       The identifier of the document that is supposed to be updated
     * @param document The updated document to use
     */
    void updateDocument(String id, JSONObject document);

    /**
     * Deletes a document, given by its identifier, from the repository.
     *
     * @param id The identifier of the document to delete
     */
    void deleteDocument(String id);

    /**
     * Clears the entire repository by deleting all of its documents.
     */
    void clearRepository();

    /**
     * Searches all documents in the repository for those that match a given query,
     * consisting out of a {@link JSONArray} of requirements and a {@link JSONArray} of scoring criteria.
     *
     * @param requirements    The requirements of the query
     * @param scoringCriteria The scoring criteria of the query
     * @return A list of matching device descriptions
     */
    List<JSONObject> query(JSONArray requirements, JSONArray scoringCriteria);

    /**
     * Returns the total number of documents in the repository.
     *
     * @return The number of documents
     */
    long getDocumentsCount();

    /**
     * Returns a summary map (key name --> key data type) of all keys and their associated data types
     * that occur in the documents of the repository.
     *
     * @return The map of occurring keys and their data types
     */
    Map<String, String> getKeySummary();

    /**
     * Returns all available documents from the repository as a map that contains the identifiers of the documents
     * as well as their contents.
     *
     * @return The map (document ID --> document content) of all available documents
     */
    Map<String, JSONObject> getAllDocuments();

    /**
     * Sets the exception handler that is supposed to be used for handling exceptions that occur during the
     * communication between the client and the repository.
     *
     * @param exceptionHandler The exception handler to set or null, if exceptions should not be handled
     */
    void setExceptionHandler(RepositoryExceptionHandler exceptionHandler);

    /**
     * Returns the exception handler that is currently used for handling exceptions that occur during the
     * communication between the client and the repository.
     *
     * @return The exception handler
     */
    RepositoryExceptionHandler getExceptionHandler();
}
