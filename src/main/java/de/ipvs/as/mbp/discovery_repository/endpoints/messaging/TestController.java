package de.ipvs.as.mbp.discovery_repository.endpoints.messaging;

import de.ipvs.as.mbp.discovery_repository.TopicConfiguration;
import de.ipvs.as.mbp.discovery_repository.service.descriptions.DeviceDescriptionsService;
import de.ipvs.as.mbp.discovery_repository.service.messaging.endpoints.MessagingController;
import de.ipvs.as.mbp.discovery_repository.service.messaging.endpoints.MessagingEndpoint;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Controller that exposes an endpoint for handling incoming test request messages. The purpose of these messages is
 * to test the availability of discovery repositories.
 */
@Component
@MessagingController
public class TestController {

    @Autowired
    private DeviceDescriptionsService deviceDescriptionsService;

    @MessagingEndpoint(topic = TopicConfiguration.SUB_TOPIC_TEST, type = "repository_test_reply")
    public JSONObject handleTestRequests(String topic, JSONObject message) {
        //Retrieve number of available device descriptions
        long deviceDescriptionsCount = deviceDescriptionsService.getDeviceDescriptionsCount();

        //Create body of reply message
        JSONObject replyMessageBody = new JSONObject();

        //Add number of available device descriptions
        replyMessageBody.put("devicesCount", deviceDescriptionsCount);

        //Return body of the reply message
        return replyMessageBody;
    }
}
