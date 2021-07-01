package de.ipvs.as.mbp.discovery_repository.service.repository.impl.elasticsearch;

import de.ipvs.as.mbp.discovery_repository.service.repository.RepositoryClient;
import de.ipvs.as.mbp.discovery_repository.service.repository.handler.RepositoryExceptionHandler;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.GetMappingsResponse;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ElasticSearchClient implements RepositoryClient {
    //Credentials provider to use
    private final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

    //REST client to use for communicating with the elasticsearch repository
    private RestHighLevelClient restClient;

    //Name of the index to use
    private String indexName;

    //Exception handler to use for handling exceptions
    private RepositoryExceptionHandler exceptionHandler;


    /**
     * Creates and initializes the elasticsearch client.
     */
    public ElasticSearchClient() {

    }


    /**
     * Establishes a connection to the repository that is available at a given host address with a given port by
     * using a given username and password
     *
     * @param hostAddress    The host address of the messaging broker
     * @param port           The port of the messaging broker
     * @param username       The username to use
     * @param password       The password to use
     * @param collectionName The name that is supposed to be used for the repository collection in which the documents
     */
    @Override
    public void connect(String hostAddress, int port, String username, String password, String collectionName) {
        //Set index name to use
        this.indexName = collectionName;

        //Store credentials in the credentials provider
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        //Create REST client
        this.restClient = new RestHighLevelClient(
                RestClient.builder(new HttpHost(hostAddress, port, "http"))
                        .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                                .setDefaultCredentialsProvider(credentialsProvider)));

        //TODO prepare index (create, mapping...) --> in prepare funktion auslagern
    }

    /**
     * Gracefully disconnects from the repository in case a connection was previously established.
     */
    @Override
    public void disconnect() {
        //Since a REST client is used, no action needs to be done here
    }

    /**
     * Disconnects and destroys the client such that all allocated resources are released.
     */
    @Override
    public void close() {
        try {
            this.restClient.close();
        } catch (IOException e) {
            handleException(e);
        }
    }

    /**
     * Returns whether there is currently an active connection to the repository.
     *
     * @return True, if a connection exists; false otherwise
     */
    @Override
    public boolean isConnected() {
        try {
            //Perform ping in order to check whether the repository can be reached
            return this.restClient.ping(RequestOptions.DEFAULT);
        } catch (IOException e) {
            handleException(e);
            return false;
        }
    }

    /**
     * Inserts a document, given as {@link JSONObject}, into the repository and returns the identifier under which
     * it was stored.
     *
     * @param document The document to insert
     * @return The identifier under which the document was stored
     */
    @Override
    public String insertDocument(JSONObject document) {
        //Create index request
        IndexRequest indexRequest = new IndexRequest(this.indexName).source(document, XContentType.JSON);

        try {
            //Index the document
            IndexResponse response = this.restClient.index(indexRequest, RequestOptions.DEFAULT);
            //Get ID and return it
            return response.getId();
        } catch (IOException e) {
            handleException(e);
            return null;
        }
    }

    /**
     * Retrieves the document from the repository that matches the given identifier.
     *
     * @param id The identifier of the document to retrieve
     * @return The document as {@link JSONObject}
     */
    @Override
    public JSONObject getDocument(String id) {
        return null;
    }

    /**
     * Updates a document with a certain identifier in the repository by replacing it with a new document.
     *
     * @param id       The identifier of the document that is supposed to be updated
     * @param document The updated document to use
     */
    @Override
    public void updateDocument(String id, JSONObject document) {
        //Create update request
        UpdateRequest updateRequest = new UpdateRequest(this.indexName, id).doc(document);

        try {
            //Update the document
            this.restClient.update(updateRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            handleException(e);
        }
    }

    /**
     * Deletes a document, given by its identifier, from the repository.
     *
     * @param id The identifier of the document to delete
     */
    @Override
    public void deleteDocument(String id) {
        //Create delete request
        DeleteRequest deleteRequest = new DeleteRequest(this.indexName).id(id);

        try {
            //Perform deletion
            this.restClient.delete(deleteRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            handleException(e);
        }
    }

    /**
     * Clears the entire repository by deleting all of its documents.
     */
    @Override
    public void clearRepository() {
        try {
            //Check if index exists
            if (this.restClient.indices().exists(new GetIndexRequest(this.indexName), RequestOptions.DEFAULT)) {
                //Delete index
                this.restClient.indices().delete(new DeleteIndexRequest(this.indexName), RequestOptions.DEFAULT);
            }
        } catch (IOException e) {
            handleException(e);
        }
    }

    /**
     * Searches all documents in the repository for those that match a given query.
     *
     * @param query The query to use as {@link JSONObject}
     * @return The collection of documents that match the query
     */
    @Override
    public List<JSONObject> search(JSONObject query) {
        return null;
    }

    /**
     * Returns the total number of documents in the repository.
     *
     * @return The number of documents
     */
    @Override
    public long getDocumentsCount() {
        //Create count request
        CountRequest countRequest = new CountRequest(this.indexName);

        try {
            //Count all documents
            CountResponse countResponse = this.restClient.count(countRequest, RequestOptions.DEFAULT);
            //Return result
            return countResponse.getCount();
        } catch (IOException e) {
            handleException(e);
            return -1;
        }
    }

    /**
     * Returns a summary of all JSON keys that occur in the documents of the repository.
     *
     * @return The set of occurring keys
     */
    @Override
    public Set<String> getKeySummary() {
        //Create get mappings request
        GetMappingsRequest getMappingsRequest = new GetMappingsRequest().indices(this.indexName);

        try {
            //Retrieve the mapping
            GetMappingsResponse response = this.restClient.indices().getMapping(getMappingsRequest, RequestOptions.DEFAULT);
            //Extract mappings
            Map<String, MappingMetadata> mappings = response.mappings();
            //TODO
        } catch (IOException e) {
            handleException(e);
            return Collections.emptySet();
        }

        return Collections.emptySet();
    }

    /**
     * Returns all available documents from the repository as a map that contains the identifiers of the documents
     * as well as their contents.
     *
     * @return The map (document ID --> document content) of all available documents
     */
    @Override
    public Map<String, JSONObject> getAllDocuments() {
        //Build search request
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().query(new MatchAllQueryBuilder());

        //Create search request for all documents
        SearchRequest searchRequest = new SearchRequest(this.indexName).source(sourceBuilder);

        SearchResponse response;
        try {
            //Perform search request
            response = this.restClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            handleException(e);
            return Collections.emptyMap();
        }

        //Get hits
        SearchHit[] searchHits = response.getHits().getHits();

        //Check for any hits
        if ((searchHits == null) || (searchHits.length < 1)) {
            return Collections.emptyMap();
        }

        //Convert all hits to JSON objects and put them in a map (document ID --> document content)
        return Arrays.stream(searchHits).collect(Collectors.toMap(SearchHit::getId, h -> new JSONObject(h.getSourceAsString())));
    }

    /**
     * Sets the exception handler that is supposed to be used for handling exceptions that occur during the
     * communication between the client and the repository.
     *
     * @param exceptionHandler The exception handler to set or null, if exceptions should not be handled
     */
    @Override
    public void setExceptionHandler(RepositoryExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * Returns the exception handler that is currently used for handling exceptions that occur during the
     * communication between the client and the repository.
     *
     * @return The exception handler
     */
    @Override
    public RepositoryExceptionHandler getExceptionHandler() {
        return this.exceptionHandler;
    }

    /**
     * Handles a given exception by either calling the exception handler or printing information about the exception
     * to the standard output.
     *
     * @param exception The exception to handle
     */
    private void handleException(Exception exception) {
        //Sanity check
        if (exception == null) {
            return;
        }

        //Check if exception handler is set
        if (this.exceptionHandler != null) {
            //Call exception handler
            this.exceptionHandler.handleException(exception);
            return;
        }

        //No exception handler set, thus print to standard output
        System.err.printf("%s: %s%n", exception.getClass().getSimpleName(), exception.getMessage());
        exception.printStackTrace();
    }
}
