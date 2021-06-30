package de.ipvs.as.mbp.discovery_repository.service.messaging.handler;

/**
 * Message handler that is notified about all messages that are published at the publish-subscribe
 * messaging broker under a topic that matches a certain topic filter.
 */
public interface PubSubMessageHandler {
    /**
     * Handles a message that was published at the publish-subscribe messaging broker under a topic that
     * matches a certain topic filter.
     *
     * @param topic   The topic under which the message was published
     * @param message The published message
     */
    void handleMessage(String topic, String message);
}
