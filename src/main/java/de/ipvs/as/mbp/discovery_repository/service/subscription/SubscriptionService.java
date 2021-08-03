package de.ipvs.as.mbp.discovery_repository.service.subscription;

import de.ipvs.as.mbp.discovery_repository.service.descriptions.DeviceDescriptionsService;
import de.ipvs.as.mbp.discovery_repository.service.messaging.MessageService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Service that offers functions related to the subscriptions that the IoT platform can register at the repository.
 */
@Service
public class SubscriptionService {

    @Autowired
    private DeviceDescriptionsService deviceDescriptionsService;

    @Autowired
    private MessageService messageService;

    //Map (notification topic --> subscriptions) of all registered subscriptions
    private final Map<String, Set<Subscription>> subscriptionsMap;

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

        //Check if the subscription map already contains an entry for the notification topic
        if (this.subscriptionsMap.containsKey(subscription.getNotificationTopic())) {
            //Get the set of subscriptions for this topic and update it
            Set<Subscription> subscriptions = this.subscriptionsMap.get(subscription.getNotificationTopic());
            subscriptions.remove(subscription);
            subscriptions.add(subscription);
        } else {
            //Create new set of subscriptions for the notification topic
            this.subscriptionsMap.put(subscription.getNotificationTopic(), new HashSet<>(Collections.singleton(subscription)));
        }
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

        //Create dummy subscription from the reference ID
        Subscription dummySubscription = new Subscription(referenceId);

        //Unregister the subscription
        Set<String> keysToDelete = new HashSet<>();
        this.subscriptionsMap.forEach((t, subscriptions) -> {
            subscriptions.remove(dummySubscription);
            if (subscriptions.isEmpty()) keysToDelete.add(t);
        });
        keysToDelete.forEach(this.subscriptionsMap::remove);
    }

    /**
     * Returns the total number of currently registered subscriptions.
     *
     * @return The number of subscriptions
     */
    public int getSubscriptionsCount() {
        return this.subscriptionsMap.size();
    }

    /**
     * Checks whether the device description query results changed for one or multiple {@link Subscription}s
     * after the insertion of a new device description. If this is the case, the affected subscribers
     * will be notified with a notification message about the new device description.
     *
     * @param insertedDeviceDescription The inserted device description
     */
    public void notifyAboutInsert(JSONObject insertedDeviceDescription) {
        //Iterate over the subscription map
        this.subscriptionsMap.forEach((notificationTopic, subscriptions) -> {
            //Create set storing the reference IDs of the affected subscriptions
            Set<String> referenceIds = new HashSet<>();

            //Iterate over all subscriptions
            subscriptions.forEach(sub -> {
                //Execute the query again for the current subscription
                List<JSONObject> newQueryResult = this.deviceDescriptionsService.queryDeviceDescriptions(sub.getRequirements(), sub.getScoringCriteria());

                //Check if the new device description is part of the result set
                if (newQueryResult.stream().noneMatch(d -> deviceDescriptionsEquals(d, insertedDeviceDescription)))
                    return;

                //Update subscription object
                sub.setQueryResult(newQueryResult);

                //Add reference ID to the set
                referenceIds.add(sub.getReferenceId());
            });

            //Create body of notification message
            JSONObject notificationMessageBody = new JSONObject();

            //Create revision object
            JSONObject revisionObject = new JSONObject();

            //Create operation object
            JSONObject operationObject = new JSONObject();
            operationObject.put("type", "upsert");
            operationObject.put("deviceDescriptions", new JSONArray().put(insertedDeviceDescription));

            //Set fields of revision object
            revisionObject.put("referenceIds", new JSONArray().putAll(referenceIds));
            revisionObject.put("operations", new JSONArray().put(operationObject));

            //Add revision object to reply message
            notificationMessageBody.put("revisions", new JSONArray().put(revisionObject));

            //Publish the notification message
            messageService.publishMessage(notificationTopic, notificationMessageBody, "query_reply");
        });
    }

    /**
     * Checks whether the device description query results changed for one or multiple {@link Subscription}s
     * after the deletion of a device description. If this is the case, the affected subscribers
     * will be notified with a notification message about the deleted device description.
     *
     * @param deviceDescription The device description that has been deleted
     */
    public void notifyAboutDelete(JSONObject deviceDescription) {
        //Retrieve MAC address from the device description
        String macAddress = deviceDescription.has("identifiers") ? deviceDescription.optJSONObject("identifiers").optString("mac_address") : "";

        //Check if MAC address is valid
        if (macAddress.isEmpty()) return;

        //Iterate over the subscription map
        this.subscriptionsMap.forEach((notificationTopic, subscriptions) -> {
            //Create set storing the reference IDs of the affected subscriptions
            Set<String> referenceIds = new HashSet<>();

            //Iterate over all subscriptions
            subscriptions.forEach(sub -> {
                //Check if the current result set of the subscription contains the affected device description
                if (sub.getQueryResult().stream().noneMatch(d -> deviceDescriptionsEquals(d, deviceDescription))) {
                    return;
                }

                //Update subscription object
                sub.setQueryResult(this.deviceDescriptionsService.queryDeviceDescriptions(sub.getRequirements(), sub.getScoringCriteria()));

                //Result set of the current subscriptions is affected, so add the reference ID to the set
                referenceIds.add(sub.getReferenceId());
            });

            //Create body of notification message
            JSONObject notificationMessageBody = new JSONObject();

            //Create revision object
            JSONObject revisionObject = new JSONObject();

            //Create operation object
            JSONObject operationObject = new JSONObject();
            operationObject.put("type", "delete");
            operationObject.put("macAddresses", new JSONArray().put(macAddress));

            //Set fields of revision object
            revisionObject.put("referenceIds", new JSONArray().putAll(referenceIds));
            revisionObject.put("operations", new JSONArray().put(operationObject));

            //Add revision object to reply message
            notificationMessageBody.put("revisions", new JSONArray().put(revisionObject));

            //Publish the notification message
            messageService.publishMessage(notificationTopic, notificationMessageBody, "query_reply");
        });
    }

    /**
     * Checks whether the device description query results changed for one or multiple {@link Subscription}s
     * after deleting all device descriptions. If this is the case, the affected subscribers
     * will be notified with a notification message about the full deletion.
     */
    public void notifyAboutClear() {
        //Iterate over the subscription map
        this.subscriptionsMap.forEach((notificationTopic, subscriptions) -> {
            //Create set storing the reference IDs of the affected subscriptions
            Set<String> referenceIds = new HashSet<>();

            //Iterate over all subscriptions
            subscriptions.forEach(sub -> {
                //Check if the current result set of the subscription contained any device description
                if (sub.getQueryResult().isEmpty()) return;

                //Update subscription object
                sub.setQueryResult(new LinkedList<>());

                //Result set of the current subscriptions is affected, so add the reference ID to the set
                referenceIds.add(sub.getReferenceId());
            });

            //Create body of notification message
            JSONObject notificationMessageBody = new JSONObject();

            //Create revision object
            JSONObject revisionObject = new JSONObject();

            //Create operation object
            JSONObject operationObject = new JSONObject();
            operationObject.put("type", "replace");
            operationObject.put("deviceDescriptions", new JSONArray());

            //Set fields of revision object
            revisionObject.put("referenceIds", new JSONArray().putAll(referenceIds));
            revisionObject.put("operations", new JSONArray().put(operationObject));

            //Add revision object to reply message
            notificationMessageBody.put("revisions", new JSONArray().put(revisionObject));

            //Publish the notification message
            messageService.publishMessage(notificationTopic, notificationMessageBody, "query_reply");
        });
    }

    /**
     * Checks and returns whether two device descriptions, given as {@link JSONObject}, are equal, i.e. describe
     * the same device. For this, the MAC addresses of the device descriptions are compared.
     *
     * @param deviceDescription1 The first device description to compare
     * @param deviceDescription2 The second device description to compare
     * @return True, if both device descriptions are equal; false otherwise
     */
    private boolean deviceDescriptionsEquals(JSONObject deviceDescription1, JSONObject deviceDescription2) {
        //Null checks
        if ((deviceDescription1 == null) || (deviceDescription2 == null)) return false;

        //Get identifiers
        JSONObject identifiers1 = deviceDescription1.optJSONObject("identifiers");
        JSONObject identifiers2 = deviceDescription2.optJSONObject("identifiers");

        //Check availability of identifiers
        if ((identifiers1 == null) || (identifiers2 == null)) return false;

        //Get MAC addresses
        String mac1 = identifiers1.optString("mac_address");
        String mac2 = identifiers2.optString("mac_address");

        //Check availability of MAC addresses
        if (mac1.isEmpty() || mac2.isEmpty()) return false;

        //Compare MAC addresses
        return mac1.equalsIgnoreCase(mac2);
    }
}
