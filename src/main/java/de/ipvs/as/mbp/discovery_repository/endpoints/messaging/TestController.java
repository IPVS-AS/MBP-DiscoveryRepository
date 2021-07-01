package de.ipvs.as.mbp.discovery_repository.endpoints.messaging;

import de.ipvs.as.mbp.discovery_repository.service.messaging.endpoints.MessagingController;
import de.ipvs.as.mbp.discovery_repository.service.messaging.endpoints.MessagingEndpoint;
import org.json.JSONObject;

/**
 * Controller that exposes an endpoint for handling incoming test request messages. The purpose of these messages is
 * to test the availability of discovery repositories.
 */
@MessagingController
public class TestController {

    @MessagingEndpoint(topic = "+/discovery/+/test", type = "repository_test_reply")
    public JSONObject handleTestRequests(String topic, JSONObject message) {
        //Create body of reply message
        JSONObject replyMessageBody = new JSONObject();

        //Add number of available device descriptions
        replyMessageBody.put("devicesCount", 30);

        //Return body of the reply message
        return replyMessageBody;
    }
}
