package de.ipvs.as.mbp.discovery_repository.endpoints.messaging;

import de.ipvs.as.mbp.discovery_repository.TopicConfiguration;
import de.ipvs.as.mbp.discovery_repository.service.subscription.Subscription;
import de.ipvs.as.mbp.discovery_repository.service.descriptions.DeviceDescriptionsService;
import de.ipvs.as.mbp.discovery_repository.service.messaging.endpoints.MessagingController;
import de.ipvs.as.mbp.discovery_repository.service.messaging.endpoints.MessagingEndpoint;
import de.ipvs.as.mbp.discovery_repository.service.subscription.SubscriptionService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Controller that exposes an endpoint for handling incoming device description query request messages.
 * The purpose of these messages is to query the device descriptions that are stored in the repository of this
 * application and to possibly enable asynchronous notifications about changes in the result set.
 */
@Component
@MessagingController
public class QueryController {

    @Autowired
    private DeviceDescriptionsService deviceDescriptionsService;

    @Autowired
    private SubscriptionService subscriptionService;

    @MessagingEndpoint(topic = TopicConfiguration.SUB_TOPIC_QUERY, type = "repository_test_reply")
    public JSONObject handleQueryRequests(String topic, JSONObject message) {
        //Get message payload
        JSONObject messagePayload = message.getJSONObject("message");

        //Extract requirements and scoring criteria from payload
        JSONArray requirements = messagePayload.optJSONArray("requirements");
        JSONArray scoringCriteria = messagePayload.optJSONArray("scoringCriteria");

        //Get subscription details
        JSONObject subscriptionDetails = messagePayload.optJSONObject("subscription");

        //Query the device description repository using the requirements and scoring criteria
        List<JSONObject> matchingDeviceDescriptions = deviceDescriptionsService.queryDeviceDescriptions(requirements, scoringCriteria);

        //Create body of reply message
        JSONObject replyMessageBody = new JSONObject();

        //Check if subscription details are available
        replyMessageBody.put("referenceId", JSONObject.NULL);
        if ((subscriptionDetails != null) && (!subscriptionDetails.optString("referenceId", "").isEmpty())) {
            //Extract subscription fields
            String returnTopic = subscriptionDetails.getString("returnTopic");
            String referenceId = subscriptionDetails.getString("referenceId");

            //Copy reference ID to reply message
            replyMessageBody.put("referenceId", referenceId);

            //Register corresponding subscription
            Subscription newSubscription = new Subscription(returnTopic, referenceId, requirements, scoringCriteria, matchingDeviceDescriptions);
            this.subscriptionService.registerSubscription(newSubscription);
        }

        //Add the matching device descriptions to the message
        replyMessageBody.put("deviceDescriptions", new JSONArray().putAll(matchingDeviceDescriptions));

        //Return body of the reply message
        return replyMessageBody;
    }
}
