package de.ipvs.as.mbp.discovery_repository.endpoints.messaging;

import de.ipvs.as.mbp.discovery_repository.TopicConfiguration;
import de.ipvs.as.mbp.discovery_repository.service.messaging.endpoints.MessagingController;
import de.ipvs.as.mbp.discovery_repository.service.messaging.endpoints.MessagingEndpoint;
import de.ipvs.as.mbp.discovery_repository.service.subscription.SubscriptionService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Controller that exposes an endpoint for handling incoming subscription cancel request messages.
 * The purpose of these messages is to cancel existing subscriptions that have been previously registered.
 */
@Component
@MessagingController
public class CancelSubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @MessagingEndpoint(topic = TopicConfiguration.SUB_TOPIC_CANCEL)
    public JSONObject handleCancelSubscriptionRequests(String topic, JSONObject message) {
        //Get message payload
        JSONObject messagePayload = message.getJSONObject("message");

        //Extract reference ID
        String referenceId = messagePayload.optString("referenceId");

        //Sanity check
        if ((referenceId == null) || referenceId.isEmpty()) return null;

        //Unregister the subscription with the given reference ID
        this.subscriptionService.unregisterSubscription(referenceId);
        return null;
    }
}
