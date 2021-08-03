package de.ipvs.as.mbp.discovery_repository.endpoints.messaging;

import de.ipvs.as.mbp.discovery_repository.TopicConfiguration;
import de.ipvs.as.mbp.discovery_repository.service.descriptions.DeviceDescriptionsService;
import de.ipvs.as.mbp.discovery_repository.service.messaging.endpoints.MessagingController;
import de.ipvs.as.mbp.discovery_repository.service.messaging.endpoints.MessagingEndpoint;
import de.ipvs.as.mbp.discovery_repository.service.subscription.Subscription;
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

    @MessagingEndpoint(topic = TopicConfiguration.SUB_TOPIC_QUERY, type = "test_reply")
    public JSONObject handleQueryRequests(String topic, JSONObject message) {
        //Get message payload
        JSONObject messagePayload = message.getJSONObject("message");

        //Extract relevant fields from payload
        String referenceId = messagePayload.optString("referenceId");
        JSONArray requirements = messagePayload.optJSONArray("requirements");
        JSONArray scoringCriteria = messagePayload.optJSONArray("scoringCriteria");
        String notificationTopic = messagePayload.optString("notificationTopic");

        //Query the device description repository using the requirements and scoring criteria
        List<JSONObject> candidateDevices = deviceDescriptionsService.queryDeviceDescriptions(requirements, scoringCriteria);

        //Create body of reply message
        JSONObject replyMessageBody = new JSONObject();

        //Create revision object
        JSONObject revisionObject = new JSONObject();

        //Create operation object
        JSONObject operationObject = new JSONObject();
        operationObject.put("type", "replace");
        operationObject.put("deviceDescriptions", new JSONArray().putAll(candidateDevices));

        //Set fields of revision object
        revisionObject.put("referenceIds", new JSONArray().put(referenceId.isEmpty() ? JSONObject.NULL : referenceId));
        revisionObject.put("operations", new JSONArray().put(operationObject));

        //Add revision object to reply message
        replyMessageBody.put("revisions", new JSONArray().put(revisionObject));

        //Check whether a subscription is supposed to be created
        if((notificationTopic == null) || notificationTopic.isEmpty()) return replyMessageBody;

        //Register corresponding subscription
        Subscription subscription = new Subscription(notificationTopic, referenceId, requirements, scoringCriteria, candidateDevices);
        this.subscriptionService.registerSubscription(subscription);

        //Return body of the reply message
        return replyMessageBody;
    }
}
