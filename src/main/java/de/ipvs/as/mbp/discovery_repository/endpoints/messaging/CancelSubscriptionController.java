package de.ipvs.as.mbp.discovery_repository.endpoints.messaging;

import de.ipvs.as.mbp.discovery_repository.TopicConfiguration;
import de.ipvs.as.mbp.discovery_repository.service.messaging.endpoints.MessagingController;
import de.ipvs.as.mbp.discovery_repository.service.messaging.endpoints.MessagingEndpoint;
import de.ipvs.as.mbp.discovery_repository.service.subscription.SubscriptionService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

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

        //Extract reference IDs
        List<Object> referenceIds = messagePayload.optJSONArray("referenceIds").toList();

        //Null check
        if (referenceIds == null) return null;

        //Stream through all reference IDs
        referenceIds.stream()
                .filter(Objects::nonNull) //Ignore illegal objects
                .filter(o -> o instanceof String) //Consider only strings
                .map(o -> (String) o) //Cast objects to string
                .filter(s -> !s.isEmpty()) //Ignore empty reference IDs
                .forEach(r -> this.subscriptionService.unregisterSubscription(r)); //Unregister the subscription
        return null;
    }
}
