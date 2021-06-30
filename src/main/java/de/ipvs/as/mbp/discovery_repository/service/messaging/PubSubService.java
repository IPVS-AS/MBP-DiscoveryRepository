package de.ipvs.as.mbp.discovery_repository.service.messaging;

import de.ipvs.as.mbp.discovery_repository.service.messaging.handler.PubSubMessageHandler;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * This service offers technology-agnostic messaging functions for interacting with a messaging client that connects
 * itself to an external publish-subscribe-based messaging broker.
 */
@Service
@PropertySource(value = "classpath:application.properties")
public class PubSubService {
    //Delay between re-connect attempts
    private static final int RECONNECT_DELAY = 10 * 1000;

    //Auto-wired components
    private final PubSubClient pubSubClient;

    //Thread pool for re-connects on connection los
    private final ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor();

    //Scheduled future for re-connect attempts
    private ScheduledFuture<?> reconnectAttempt;

    //Remembers all subscriptions in order to deal with failures
    private final Map<String, PubSubMessageHandler> subscriptions;

    /*
    Inject broker-related settings from the application.properties file.
     */
    @Value("${mqtt_broker.host}")
    private String brokerHost;

    @Value("${mqtt_broker.port}")
    private int brokerPort;

    /**
     * Creates the service for a given {@link PubSubClient} that enables publish-subscribe-based messaging.
     *
     * @param pubSubClient The messaging client to use (auto-wired)
     */
    @Autowired
    public PubSubService(PubSubClient pubSubClient) {
        //Store references to components
        this.pubSubClient = pubSubClient;

        //Initialize data structures
        this.subscriptions = new HashMap<>();

        //Provide the responsible method of this service as connection loss handler
        pubSubClient.setConnectionLossHandler(this::handleConnectionLoss);
    }

    /**
     * Initializes the service by establishing the connection between the publish-subscribe-based messaging client
     * and the configured messaging broker.
     */
    @PostConstruct
    public void init() {
        //Let the client connect
        connectClient();
    }


    /**
     * Publishes a message, given as string, under a given topic at the messaging broker.
     *
     * @param topic   The topic under which the string message is supposed to be published
     * @param message The message to publish
     */
    public void publish(String topic, String message) {
        //Publish message via the client
        pubSubClient.publish(topic, message);
    }

    /**
     * Publishes a message, given as JSON object, under a given topic at the messaging broker.
     *
     * @param topic      The topic under which the JSON message is supposed to be published
     * @param jsonObject The message to publish
     */
    public void publish(String topic, JSONObject jsonObject) {
        //Transform provided JSON message to string and publish it
        publish(topic, jsonObject.toString());
    }

    /**
     * Publishes a message, given as string, under several given topics at the messaging broker.
     *
     * @param topics  Collection of topics under which the string message is supposed to be published
     * @param message The message to publish
     */
    public void publish(Collection<String> topics, String message) {
        //Publish the message under each provided topic individually
        topics.forEach(t -> publish(t, message));
    }

    /**
     * Publishes a message, given as JSON object, under several given topics at the messaging broker.
     *
     * @param topics     Collection of topics under which the JSON message is supposed to be published
     * @param jsonObject The message to publish
     */
    public void publish(Collection<String> topics, JSONObject jsonObject) {
        //Transform provided JSON message to string and publish it
        publish(topics, jsonObject.toString());
    }

    /**
     * Subscribes a given message handler to a given topic filter at the messaging broker, such that the handler
     * is notified when a message is published at the broker under a topic that matches the topic filter.
     *
     * @param topicFilter The topic filter to subscribe to
     * @param handler     The handler to call in case a matching message is published at the broker
     */
    public void subscribe(String topicFilter, PubSubMessageHandler handler) {
        //Sanity check
        if (handler == null) {
            throw new IllegalArgumentException("The message handler must not be null.");
        }

        //Remember this subscription
        this.subscriptions.put(topicFilter, handler);

        //Perform subscription at the broker
        this.pubSubClient.subscribe(topicFilter, handler);
    }

    /**
     * Subscribes a given message handler to several given topic filters at the messaging broker, such that the
     * handler is notified when a message is published at the broker under a topic that matches at least one
     * of the topic filters.
     *
     * @param topicFilters The topic filters to subscribe to
     * @param handler      The handler to call in case a matching message is published at the broker
     */
    public void subscribe(List<String> topicFilters, PubSubMessageHandler handler) {
        //Create one subscription for each topic filter
        topicFilters.forEach(t -> subscribe(t, handler));
    }

    /**
     * Unsubscribes the publish-subscribe-based client from a given topic at the messaging broker. This only has an
     * effect if exactly the same topic filter is provided to this method as during the preceding subscription.
     *
     * @param topicFilter The topic filter to unsubscribe from
     */
    public void unsubscribe(String topicFilter) {
        //Remove topic filter from the remembered subscriptions
        this.subscriptions.remove(topicFilter);

        //Unsubscribe topic at message broker
        this.pubSubClient.unsubscribe(topicFilter);
    }


    /**
     * Gracefully disconnects from the messaging broker if a connection exists and
     * re-establishes the connection by using the broker settings that are returned by the settings service.
     */
    public void reconnect() {
        //Execute re-connect
        connectClient();
    }

    /**
     * Checks and returns whether the client is currently connected to the messaging broker.
     *
     * @return True, if the client is connected to the messaging broker; false otherwise.
     */
    public boolean isConnected() {
        return pubSubClient.isConnected();
    }

    /**
     * Lets the messaging client establish a connection to the messaging broker by using the broker settings
     * from the application.properties file.
     */
    private void connectClient() {
        //Establish the connection
        connectClient(this.brokerHost, this.brokerPort);
    }

    /**
     * Lets the messaging client establish a connection to the messaging broker by using the given broker address
     * and broker port.
     *
     * @param brokerAddress The broker address to use
     * @param brokerPort    The broker port to use
     */
    private void connectClient(String brokerAddress, int brokerPort) {
        //Establish the connection
        pubSubClient.connect(brokerAddress, brokerPort);

        //Subscribe to all remembered topics
        this.subscriptions.forEach(pubSubClient::subscribe);
    }

    /**
     * Starts and manages periodic re-connect attempts when the messaging client looses its connection to the
     * messaging broker. As soon as the connection could be established again, the re-connect attempts are terminated.
     * Furthermore it is ensured that only at most one re-connect attempt is active at the same time.
     *
     * @param cause A {@link Throwable} containing the cause of the connection loss (ignored)
     */
    private void handleConnectionLoss(Throwable cause) {
        //Print information to console
        System.err.println("PubSubClient lost connection:");
        cause.printStackTrace();

        //Do nothing if reconnect attempts are already active
        if ((this.reconnectAttempt != null) && (!this.reconnectAttempt.isDone())) {
            return;
        }

        //Schedule periodic reconnect attempts
        this.reconnectAttempt = threadPool.scheduleWithFixedDelay(() -> {
            //Check if client is connected now
            if (pubSubClient.isConnected()) {
                //Connection established, thus cancel the re-connect attempts
                reconnectAttempt.cancel(true);
                return;
            }

            //Try to re-connect to the messaging broker
            connectClient();
        }, 0, RECONNECT_DELAY, TimeUnit.MILLISECONDS);
    }
}
