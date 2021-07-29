package de.ipvs.as.mbp.discovery_repository.service.subscription;

import de.ipvs.as.mbp.discovery_repository.service.descriptions.DeviceDescriptionsService;
import de.ipvs.as.mbp.discovery_repository.service.messaging.MessageService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service that offers functions related to the subscriptions that the IoT platform can register at the repository.
 */
@Service
public class SubscriptionService {

    @Autowired
    private DeviceDescriptionsService deviceDescriptionsService;

    @Autowired
    private MessageService messageService;

    //Map (reference ID --> subscription) of all registered subscriptions
    private final Map<String, Subscription> subscriptionsMap;

    /**
     * Creates the {@link SubscriptionService}.
     */
    public SubscriptionService() {
        //Initialize data structures
        this.subscriptionsMap = new HashMap<>();
    }

    /**
     * Initializes the {@link SubscriptionService}.
     */
    @PostConstruct
    public void initialize() {

    }

    /**
     * Registers a given {@link Subscription} at the {@link SubscriptionService}. If a {@link Subscription} with the
     * same reference ID is already registered, the old {@link Subscription} will be replaced by the new one.
     *
     * @param subscription The subscription to register
     */
    public void registerSubscription(Subscription subscription) {
        //Null check
        if (subscription == null) throw new IllegalArgumentException("The subscription must not be null.");

        //Register subscription
        this.subscriptionsMap.put(subscription.getReferenceId(), subscription);
    }

    /**
     * Unregisters a certain {@link Subscription}, given by its reference ID, from the {@link SubscriptionService}.
     *
     * @param referenceId The reference ID of the {@link Subscription} to unregister
     */
    public void unregisterSubscription(String referenceId) {
        //Sanity check
        if ((referenceId == null) || referenceId.isEmpty())
            throw new IllegalArgumentException("The reference ID must not be null or empty.");

        //Unregister the subscription
        this.subscriptionsMap.remove(referenceId);
    }

    /**
     * Checks whether the device description query results changed for one or multiple {@link Subscription}s. If this
     * is the case, the creators of the affected {@link Subscription}s will be notified with an asynchronous message
     * about the updated query results.
     */
    public void notifyForInsertOrDelete() {
        //Iterate over all available subscriptions
        this.subscriptionsMap.forEach((referenceId, subscription) -> {
            //Execute the query again for the current subscription
            List<JSONObject> newQueryResult = this.deviceDescriptionsService.queryDeviceDescriptions(subscription.getRequirements(), subscription.getScoringCriteria());

            //Compare size of new and old query result
            if (subscription.getQueryResult().size() == newQueryResult.size()) return;

            //Update subscription object
            subscription.setQueryResult(newQueryResult);

            //Create body of notification message
            JSONObject notificationMessageBody = new JSONObject();
            notificationMessageBody.put("referenceId", subscription.getReferenceId());
            notificationMessageBody.put("deviceDescriptions", new JSONArray().putAll(newQueryResult));

            //Publish the notification message
            messageService.publishMessage(subscription.getReturnTopic(), notificationMessageBody, "device_query_reply");
        });

    }
}
