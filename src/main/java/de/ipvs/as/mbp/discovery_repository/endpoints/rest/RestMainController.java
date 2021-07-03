package de.ipvs.as.mbp.discovery_repository.endpoints.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ipvs.as.mbp.discovery_repository.error.ApplicationException;
import de.ipvs.as.mbp.discovery_repository.service.descriptions.DeviceDescriptionsService;
import de.ipvs.as.mbp.discovery_repository.service.messaging.PubSubService;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Main REST controller for responding to REST requests.
 */
@RestController
public class RestMainController {
    //ObjectMapper to use for transforming JSON
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DeviceDescriptionsService deviceDescriptionsService;

    @Autowired
    private PubSubService pubSubService;

    @GetMapping(value = "/deviceDescriptions", produces = "application/json")
    public ResponseEntity<List<JsonNode>> getDeviceDescriptions() {
        //Get all device descriptions
        Map<String, JSONObject> deviceDescriptions = deviceDescriptionsService.getAllDeviceDescriptions();

        //Transform descriptions to list
        List<JsonNode> deviceDescriptionsList = deviceDescriptions.entrySet().stream().map(d -> {
            //Extend JSONObject for identifier
            d.getValue().put("id", d.getKey());
            return transformJSON(d.getValue());
        }).collect(Collectors.toList());

        //Return the list of device descriptions as response
        return ResponseEntity.ok(deviceDescriptionsList);
    }

    @GetMapping(value = "/example", produces = "application/json")
    public ResponseEntity<JsonNode> getExampleDeviceDescription() {
        //Get example device descriptions
        JSONObject exampleDeviceDescription = deviceDescriptionsService.getExampleDeviceDescription();

        //Transform example device description and return it
        return ResponseEntity.ok(this.transformJSON(exampleDeviceDescription));
    }

    @GetMapping(value = "/status", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getStatus() {
        //Create hashmap in order to collect the status information
        Map<String, Object> statusMap = new HashMap<>();

        //Add status information to mao
        statusMap.put("repository_available", deviceDescriptionsService.isRepositoryAvailable());
        statusMap.put("repository_host", deviceDescriptionsService.getHostAddress());
        statusMap.put("repository_port", deviceDescriptionsService.getPort());
        statusMap.put("repository_username", deviceDescriptionsService.getUsername());
        statusMap.put("repository_password", "*".repeat(deviceDescriptionsService.getPassword().length()));
        statusMap.put("repository_collection", deviceDescriptionsService.getCollectionName());
        statusMap.put("broker_available", pubSubService.isConnected());
        statusMap.put("broker_host", pubSubService.getBrokerHost());
        statusMap.put("broker_port", pubSubService.getBrokerPort());
        statusMap.put("device_descriptions_count", deviceDescriptionsService.getDeviceDescriptionsCount());

        //Return status map as response
        return ResponseEntity.ok(statusMap);
    }

    @PostMapping(value = "/deviceDescriptions", produces = "application/json")
    public ResponseEntity<JsonNode> insertDeviceDescription(@RequestBody String deviceDescription) {
        //Apply basic sanity checks
        if ((deviceDescription == null) || (deviceDescription.isEmpty())) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "The device description must not be null or empty.");
        }

        //Try to parse device description as JSON
        JSONObject jsonDescription;
        try {
            jsonDescription = new JSONObject(deviceDescription);
        } catch (JSONException e) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "The device description does not seem to consist out of valid JSON.");
        }

        //Validate the JSON object against the JSON schema for device descriptions
        List<String> violationMessages = this.deviceDescriptionsService.validateDeviceDescription(jsonDescription);

        //Check if no violations could be found
        if ((violationMessages != null) && (!violationMessages.isEmpty())) {
            //Violations exist, thus return them as response
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "The provided device description does not comply to the schema.", violationMessages);
        }

        //No violations, thus add the device description to the repository
        String id = this.deviceDescriptionsService.addDeviceDescription(jsonDescription);

        //Check if identifier is valid
        if ((id == null) || (id.isEmpty())) {
            //Insert failed, because no identifier is available
            throw new ApplicationException(HttpStatus.INTERNAL_SERVER_ERROR, "The device description is valid, but the insertion failed. Is the repository available?");
        }

        //Copy device description and extend it for the id
        JSONObject insertedDescription = new JSONObject(jsonDescription, JSONObject.getNames(jsonDescription));
        insertedDescription.put("id", id);

        //Return response with the extended device description
        return new ResponseEntity<>(transformJSON(insertedDescription), HttpStatus.CREATED);
    }

    @DeleteMapping(value = "/deviceDescriptions/{id}", produces = "application/json")
    public ResponseEntity<Void> deleteDeviceDescription(@PathVariable("id") String id) {
        //Apply basic sanity checks
        if ((id == null) || (id.isEmpty())) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "The device description identifier must not be null or empty.");
        }

        //Delete the device description
        this.deviceDescriptionsService.deleteDeviceDescription(id);

        //Return response with the extended device description
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/deviceDescriptions", produces = "application/json")
    public ResponseEntity<Void> clearRepository() {
        //Clear the repository
        this.deviceDescriptionsService.clearRepository();

        //Return response with the extended device description
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/capabilities", produces = "application/json")
    public ResponseEntity<Map<String, String>> getCapabilitiesSummary() {
        //Get summary about capabilities in the device descriptions
        Map<String, String> capabilitiesMap = this.deviceDescriptionsService.getCapabilitiesSummary();

        //Return response with the extended device description
        return ResponseEntity.ok(capabilitiesMap);
    }

    /**
     * Transforms a given {@link JSONObject} to a {@link JsonNode} that can be used in {@link ResponseEntity}s in order
     * to return JSON replies. If the transformation fails, null is returned instead.
     *
     * @param object The {@link JSONObject} to transform
     * @return The resulting {@link JsonNode}
     */
    private JsonNode transformJSON(JSONObject object) {
        try {
            return objectMapper.readTree(object.toString());
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
