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
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.client.indices.*;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * An implementation of the {@link RepositoryClient} interface for Elasticsearch repositories.
 */
public class ElasticSearchClient implements RepositoryClient {

    //File containing the mapping for the index
    private static final Resource MAPPING_RESOURCE = new ClassPathResource("index/mapping.json");

    //Credentials provider to use
    private final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

    //REST client to use for communicating with the elasticsearch repository
    private RestHighLevelClient restClient;

    //Name of the index to use
    private String indexName;

    //Exception handler to use for handling exceptions
    private RepositoryExceptionHandler exceptionHandler;

    //Document describing the mapping to use for the index
    private JSONObject indexMappingDocument;


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

        //Prepare and initialize the index to use
        initializeIndex();
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
        IndexRequest indexRequest = new IndexRequest(this.indexName).source(document.toString(), XContentType.JSON)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);;

        try {
            //Index the document
            IndexResponse response = this.restClient.index(indexRequest, RequestOptions.DEFAULT);

            //Get ID and return it
            return response.getId();
        } catch (IOException e) {
            //Handle the exception
            handleException(e);
            return null;
        }
    }

    /**
     * Retrieves the document from the repository that matches the given identifier as {@link JSONObject} or null
     * if the document cannot be found.
     *
     * @param id The identifier of the document to retrieve
     * @return The document as {@link JSONObject} or null
     */
    @Override
    public JSONObject getDocument(String id) {
        //Create get request
        GetRequest getRequest = new GetRequest(this.indexName).id(id);

        try {
            //Retrieve the document
            GetResponse response = this.restClient.get(getRequest, RequestOptions.DEFAULT);
            //Get the document, transform it to JSONObject and return it
            return new JSONObject(response.getSourceAsString());
        } catch (IOException e) {
            //Handle the exception
            handleException(e);
            return null;
        }
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
        UpdateRequest updateRequest = new UpdateRequest(this.indexName, id).doc(document)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);

        try {
            //Update the document
            this.restClient.update(updateRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            //Handle the exception
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
        DeleteRequest deleteRequest = new DeleteRequest(this.indexName).id(id)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);;

        try {
            //Perform deletion
            this.restClient.delete(deleteRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            //Handle the exception
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
            if (!this.restClient.indices().exists(new GetIndexRequest(this.indexName), RequestOptions.DEFAULT)) {
                return;
            }
            //Delete index
            this.restClient.indices().delete(new DeleteIndexRequest(this.indexName), RequestOptions.DEFAULT);

            //Prepare and initialize a new index
            this.initializeIndex();
        } catch (IOException e) {
            //Handle the exception
            handleException(e);
        }
    }

    /**
     * Searches all documents in the repository for those that match a given query,
     * consisting out of a {@link JSONArray} of requirements and a {@link JSONArray} of scoring criteria.
     *
     * @param requirements    The requirements of the query
     * @param scoringCriteria The scoring criteria of the query
     * @return A list of matching device descriptions
     */
    @Override
    public List<JSONObject> query(JSONArray requirements, JSONArray scoringCriteria) {
        //Use the query generator to create a corresponding boolean query
        BoolQueryBuilder query = QueryGenerator.generate(requirements, scoringCriteria);

        //Create search source with appropriate configuration from the query
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .query(query)
                .size(20)
                .timeout(new TimeValue(30, TimeUnit.SECONDS));

        //Create search request
        SearchRequest searchRequest = new SearchRequest(this.indexName).source(sourceBuilder);

        //Conduct the search
        SearchResponse response;
        try {
            response = this.restClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            //Handle the exception
            handleException(e);
            return Collections.emptyList();
        }

        //Get hits from the response
        SearchHit[] searchHits = response.getHits().getHits();

        //Check if there are any hits
        if ((searchHits == null) || (searchHits.length < 1)) {
            return Collections.emptyList();
        }

        //Transform hits to JSON objects and collect them to a list
        return Arrays.stream(searchHits).map(h -> new JSONObject(h.getSourceAsString())).collect(Collectors.toList());
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
            //Handle the exception
            handleException(e);
            return -1;
        }
    }

    /**
     * Returns a summary map (key name --> key data type) of all keys and their associated data types
     * that occur in the documents of the repository.
     *
     * @return The map of occurring keys and their data types
     */
    @Override
    public Map<String, String> getKeySummary() {
        //Create get mappings request
        GetMappingsRequest getMappingsRequest = new GetMappingsRequest().indices(this.indexName);

        try {
            //Retrieve the mapping
            GetMappingsResponse response = this.restClient.indices().getMapping(getMappingsRequest, RequestOptions.DEFAULT);

            //Extract mappings
            Map<String, MappingMetadata> mappings = response.mappings();

            //Check if mapping for the current index is available
            if (mappings.isEmpty() || (!mappings.containsKey(this.indexName))) {
                return Collections.emptyMap();
            }

            //Get mapping for current index and parse it as JSON
            JSONObject mappingData = new JSONObject(mappings.get(this.indexName).source().toString());

            //Get capability object
            JSONObject capabilityObject = mappingData.getJSONObject("properties").getJSONObject("capabilities").getJSONObject("properties");

            //Get types of the capabilities
            return capabilityObject.keySet().stream().collect(Collectors.toMap(k -> k, k -> capabilityObject.getJSONObject(k).getJSONObject("properties").getJSONObject("value").getString("type")));
        } catch (Exception e) {
            //Handle the exception
            handleException(e);
            return Collections.emptyMap();
        }
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
            //Handle the exception
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
     * Prepares and initializes the repository index that is supposed to be used.
     */
    private void initializeIndex() {
        try {
            //Check if index already exists
            if (this.restClient.indices().exists(new GetIndexRequest(this.indexName), RequestOptions.DEFAULT)) {
                //Index already exists, so do nothing
                return;
            }

            //Index does not exist, check if index mapping is already available
            if (this.indexMappingDocument == null) {
                //Read mapping from class path file
                this.indexMappingDocument = new JSONObject(new JSONTokener(MAPPING_RESOURCE.getInputStream()));
            }

            //Create request for creating the index with the mapping
            CreateIndexRequest createIndexRequest = new CreateIndexRequest(this.indexName)
                    .mapping(this.indexMappingDocument.toString(), XContentType.JSON);

            //Create index
            CreateIndexResponse response = this.restClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
            System.out.println();
        } catch (IOException e) {
            //Handle the exception
            handleException(e);
        }
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
