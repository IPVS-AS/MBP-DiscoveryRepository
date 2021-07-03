package de.ipvs.as.mbp.discovery_repository.service.descriptions;

import de.ipvs.as.mbp.discovery_repository.service.repository.RepositoryClient;
import de.ipvs.as.mbp.discovery_repository.util.OrderedJSONObject;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This service offers technology-agnostic functions for validating, storing, managing and searching device descriptions
 * in an external repository. This service also operates as a gateway that provided domain-specific methods and
 * objects and thus abstracts from repository-related behaviour and communication idiosyncrasies.
 */
@Service
@PropertySource(value = "classpath:application.properties")
public class DeviceDescriptionsService {

    @Autowired
    private RepositoryClient repositoryClient;

    /*
    Get JSON schema for device descriptions and an example device description from the classpath
     */
    @Value("classpath:data_model/device_description.schema.json")
    private Resource schemaResource;

    @Value("classpath:data_model/device_description_example.json")
    private Resource exampleResource;

    /*
    Inject repository-related properties from the application.properties file.
    */
    @Value("${repository.host}")
    private String hostAddress;

    @Value("${repository.port}")
    private int port;

    @Value("${repository.username}")
    private String username;

    @Value("${repository.password}")
    private String password;

    @Value("${repository.collection_name}")
    private String collectionName;


    //Store the JSON schema and example device description
    private Schema deviceDescriptionSchema;
    private JSONObject exampleDeviceDescription;

    /**
     * Creates the {@link DeviceDescriptionsService}.
     */
    public DeviceDescriptionsService() {

    }

    /**
     * Initializes the {@link DeviceDescriptionsService} by establishing the connection to the external repository.
     */
    @PostConstruct
    public void initialize() {
        //Establish the connection
        this.repositoryClient.connect(this.hostAddress, this.port, this.username, this.password, this.collectionName);
    }

    /**
     * Returns an example device description as {@link JSONObject}.
     *
     * @return The example device description
     */
    public JSONObject getExampleDeviceDescription() {
        //Check if example is already available
        if (this.exampleDeviceDescription != null) {
            return this.exampleDeviceDescription;
        }

        try {
            //Read example JSON file and return it
            this.exampleDeviceDescription = new OrderedJSONObject(new JSONTokener(exampleResource.getInputStream()));
            //Return example device description
            return this.exampleDeviceDescription;
        } catch (IOException e) {
            //Failed, return empty JSONObject instead
            return new JSONObject();
        }
    }

    /**
     * Returns all available device descriptions as a map that contains the device descriptions and their
     * identifiers.
     *
     * @return The map (device description ID --> device description) of all available device descriptions
     */
    public Map<String, JSONObject> getAllDeviceDescriptions() {
        //Get all documents
        return this.repositoryClient.getAllDocuments();
    }

    /**
     * Returns the device description that matches the given identifier as {@link JSONObject} or null if the device
     * description cannot be found.
     *
     * @param id The identifier of the device description
     * @return The found device description or null
     */
    public JSONObject getDeviceDescription(String id) {
        //Sanity check
        if ((id == null) || (id.isEmpty())) {
            throw new IllegalArgumentException("The identifier of the device description must not be null or empty");
        }

        //Return document with this ID
        return this.repositoryClient.getDocument(id);
    }

    /**
     * Deletes the device description that matches the given identifier as {@link JSONObject}.
     *
     * @param id The identifier of the device description to delete
     */
    public void deleteDeviceDescription(String id) {
        //Sanity check
        if ((id == null) || (id.isEmpty())) {
            throw new IllegalArgumentException("The identifier of the device description must not be null or empty");
        }

        //Delete the document
        this.repositoryClient.deleteDocument(id);
    }

    /**
     * Returns the number of available device descriptions.
     *
     * @return The number of device descriptions
     */
    public long getDeviceDescriptionsCount() {
        //Get documents count
        return this.repositoryClient.getDocumentsCount();
    }

    /**
     * Inserts a given device description to the repository.
     *
     * @param deviceDescription The device description to insert
     * @return The identifier of the inserted device description
     */
    public String addDeviceDescription(JSONObject deviceDescription) {
        //Sanity check
        if (deviceDescription == null) {
            throw new IllegalArgumentException("The device description must not be null or empty.");
        }

        //Insert device description as document and return the identifier
        return this.repositoryClient.insertDocument(deviceDescription);
    }

    /**
     * Clears the entire repository and deletes all device descriptions that are contained in it.
     */
    public void clearRepository() {
        //Clear repository
        this.repositoryClient.clearRepository();
    }

    /**
     * Returns a summary map (key name --> key data type) of all capability keys and their associated data types
     * that occur in the device descriptions that are stored in the repository.
     *
     * @return The map of occurring capability keys and their data types
     */
    public Map<String, String> getCapabilitiesSummary() {
        //Get capabilities summary
        return this.repositoryClient.getKeySummary();
    }

    /**
     * Validates a device description, given as {@link JSONObject}, against the JSON schema for device descriptions.
     * The resulting list contains messages that describe the occurred violations.
     *
     * @param deviceDescription The device description to validate
     * @return A list of messages describing the violations
     */
    public List<String> validateDeviceDescription(JSONObject deviceDescription) {
        //Check if schema is already available
        if (this.deviceDescriptionSchema == null) {
            try {
                //Read schema from file and store it
                JSONObject rawSchema = new JSONObject(new JSONTokener(schemaResource.getInputStream()));
                this.deviceDescriptionSchema = SchemaLoader.load(rawSchema);
            } catch (IOException e) {
                throw new RuntimeException("JSON schema for device descriptions could not be parsed.");
            }
        }

        try {
            //Use the schema to validate the given device description
            this.deviceDescriptionSchema.validate(deviceDescription);
        } catch (ValidationException e) {
            //Get validation issues and return the reasons
            return e.getAllMessages();
        }

        //No validation exception, everything fine
        return Collections.emptyList();
    }

    /**
     * Returns whether the repository is available in which the device descriptions are stored.
     *
     * @return True, if the repository is available; false otherwise
     */
    public boolean isRepositoryAvailable() {
        //Check whether a connection to the repository exists
        return this.repositoryClient.isConnected();
    }

    /**
     * Returns the host address of the repository.
     *
     * @return The host address
     */
    public String getHostAddress() {
        return hostAddress;
    }

    /**
     * Returns the port of the repository.
     *
     * @return The port
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the username that is used for authorization at the repository.
     *
     * @return The username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the password that is used for authorization at the repository.
     *
     * @return The password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Returns the name of the collection that is used within repository.
     *
     * @return The collection name
     */
    public String getCollectionName() {
        return collectionName;
    }
}
