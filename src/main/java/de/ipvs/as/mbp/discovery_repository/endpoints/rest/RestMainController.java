package de.ipvs.as.mbp.discovery_repository.endpoints.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ipvs.as.mbp.discovery_repository.service.descriptions.DeviceDescriptionsService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
