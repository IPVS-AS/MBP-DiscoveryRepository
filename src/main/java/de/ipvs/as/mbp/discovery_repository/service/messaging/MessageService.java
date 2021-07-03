package de.ipvs.as.mbp.discovery_repository.service.messaging;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for sending messages via publish-subscribe-based messaging.
 */
@Service
@PropertySource(value = "classpath:application.properties")
public class MessageService {

    @Autowired
    private PubSubService pubSubService;

    //Read name of the repository from the properties file
    @Value("${app.name}")
    private String repositoryName;

    /**
     * Creates and initializes the service.
     */
    public MessageService() {

    }

    /**
     * Publishes a JSON message with a given message body of a certain type under a given topic.
     *
     * @param topic       The topic under which the message is supposed to be published
     * @param messageBody The body of the message to publish as {@link JSONObject}
     * @param type        The type name of the message
     */
    public void publishMessage(String topic, JSONObject messageBody, String type) {
        //Sanity checks
        if ((topic == null) || (topic.isEmpty())) {
            throw new IllegalArgumentException("The topic must not be null or empty.");
        } else if (messageBody == null) {
            throw new IllegalArgumentException("The message body must not be null.");
        }

        //Create JSON object for the overall message
        JSONObject messageObject = new JSONObject();

        //Extend it for default fields and the message body
        extendMessage(messageObject, messageBody, type);

        //Publish the message under the topic
        this.pubSubService.publish(topic, messageObject);
    }

    /**
     * Publishes a JSON reply message with a given message body of a certain type under a reply topic that was indicated
     * in a preceding request message, which is given as {@link JSONObject}. Optionally, also the type name of the reply
     * message can be added to the message.
     *
     * @param replyMessageBody The body of the reply message to use
     * @param requestMessage   The request message that resulted into this reply message
     * @param type             The type name of the reply message
     */
    public void publishReplyMessage(JSONObject replyMessageBody, JSONObject requestMessage, String type) {
        //Sanity checks
        if (replyMessageBody == null) {
            throw new IllegalArgumentException("The message body must not be null.");
        } else if (requestMessage == null) {
            throw new IllegalArgumentException("The request message must not be null.");
        } else if (!requestMessage.has("returnTopic")) {
            throw new IllegalArgumentException("The request message does not contain a return topic.");
        }

        //Extract return topic from the request message
        String returnTopic = requestMessage.optString("returnTopic");

        //Sanity checks for return topic
        if ((returnTopic == null) || (returnTopic.isEmpty())) {
            throw new IllegalArgumentException("The provided return topic must not be null or empty.");
        }

        //Create JSON object for the overall reply message
        JSONObject replyMessageObject = new JSONObject();

        //Extend it for default fields and the message body
        extendMessage(replyMessageObject, replyMessageBody, type);

        //Use repository name as sender name
        replyMessageObject.put("senderName", this.repositoryName);

        //Check if a correlation identifier was provided
        if (requestMessage.has("correlationId")) {
            //Extract correlation identifier
            String correlationId = requestMessage.optString("correlationId");

            //Check if correlation identifier contains information
            if ((correlationId != null) && (!correlationId.isEmpty())) {
                //Add correlation identifier to the reply message
                replyMessageObject.put("correlationId", correlationId);
            }
        }

        //Publish reply message under the return topic
        this.pubSubService.publish(returnTopic, replyMessageObject);
    }

    /**
     * Extends a message, given as {@link JSONObject}, for the default fields and also adds the message body,
     * given as {@link JSONObject}, to it. Optionally, also a given type name of the message body can be added.
     *
     * @param message     The message to extend
     * @param messageBody The message body to add to the message
     * @param type        The optional type name of the message
     */
    private void extendMessage(JSONObject message, JSONObject messageBody, String type) {
        //Sanity checks
        if (message == null) {
            throw new IllegalArgumentException("The message must not be null.");
        } else if (messageBody == null) {
            throw new IllegalArgumentException("The message body must not be null.");
        }

        //Add default fields
        if ((type != null) && (!type.isEmpty())) {
            message.put("type", type);
        }
        message.put("time", Instant.now().toEpochMilli());

        //Add message body
        message.put("message", messageBody);
    }
}
