package de.ipvs.as.mbp.discovery_repository.endpoints.messaging;

import de.ipvs.as.mbp.discovery_repository.service.messaging.PubSubService;
import de.ipvs.as.mbp.discovery_repository.service.messaging.endpoints.MessagingController;
import de.ipvs.as.mbp.discovery_repository.service.messaging.endpoints.MessagingEndpoint;
import org.json.JSONObject;

/**
 * Controller that exposes an endpoint for handling incoming test request messages. The purpose of these messages is
 * to test the availability of discovery repositories.
 */
@MessagingController
public class TestController {

    @MessagingEndpoint(topic = "+/discovery/+/test")
    public JSONObject handleTestRequests(String topic, JSONObject message){
        System.out.println("hier!");
        return new JSONObject();
    }
}
