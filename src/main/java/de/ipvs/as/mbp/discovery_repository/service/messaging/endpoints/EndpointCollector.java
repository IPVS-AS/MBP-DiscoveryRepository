package de.ipvs.as.mbp.discovery_repository.service.messaging.endpoints;

import de.ipvs.as.mbp.discovery_repository.Main;
import de.ipvs.as.mbp.discovery_repository.service.messaging.MessageService;
import de.ipvs.as.mbp.discovery_repository.service.messaging.PubSubService;
import org.json.JSONObject;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * This component is responsible for detecting {@link MessagingController}s in the classpath and dispatching the
 * received messages to their {@link MessagingEndpoint}s.
 */
@Component
public class EndpointCollector {

    //Auto-wired components
    private final PubSubService pubSubService;
    private final MessageService messageService;

    /**
     * Creates and initializes the endpoint manager by using a given {@link PubSubService} that enables
     * publish-subscribe-based messaging and a given {@link MessageService} that is able to create appropriate
     * reply messages.
     *
     * @param pubSubService  The publish-subscribe-based messaging service to use
     * @param messageService The message service to use for construction reply messages
     */
    @Autowired
    public EndpointCollector(PubSubService pubSubService, MessageService messageService) {
        //Store component references globally
        this.pubSubService = pubSubService;
        this.messageService = messageService;

        //Create reflections object
        Reflections reflections = new Reflections(Main.BASE_PACKAGES);

        //Find classes with the MessagingController annotation
        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(MessagingController.class);

        //Iterate over the found controllers
        for (Class<?> controller : annotatedClasses) {
            //Iterate over the methods of this class
            for (Method method : controller.getDeclaredMethods()) {
                //Skip methods without endpoint annotation
                if (!method.isAnnotationPresent(MessagingEndpoint.class)) {
                    continue;
                }

                //Get the endpoint annotation of the current method
                MessagingEndpoint endpointAnnotation = method.getAnnotation(MessagingEndpoint.class);

                //Subscribe the client to the topic of this method and set a handler for dispatching the message
                pubSubService.subscribe(endpointAnnotation.topic(), (t, m) -> this.dispatchMessage(method, t, m));
            }
        }
    }

    /**
     * Dispatches a given message that was received under a given topic to the responsible {@link MessagingEndpoint}
     * method. For this, the message is transformed to a {@link JSONObject} and passed to an instance of the
     * method forming the responsible endpoint. Furthermore, possible bodies of reply messages that are returned as
     * {@link JSONObject}s from the endpoint method are transformed to reply messages and published accordingly.
     *
     * @param method  The endpoint method to dispatch the method to
     * @param topic   The topic under which the received message was published
     * @param message The received message
     */
    private void dispatchMessage(Method method, String topic, String message) {
        {
            try {
                //Create JSON object from the message string
                JSONObject jsonMessage = new JSONObject(message);

                //Get annotation of the method
                MessagingEndpoint endpointAnnotation = method.getAnnotation(MessagingEndpoint.class);

                //Create object from the controller class of the method
                Object object = method.getDeclaringClass().getConstructor().newInstance();

                //Check if message returns a JSON object as reply
                if (method.getReturnType().isAssignableFrom(JSONObject.class)) {
                    //Call the method to handle the message and get the reply message body
                    JSONObject replyMessageBody = (JSONObject) method.invoke(object, topic, jsonMessage);

                    //Publish the reply message
                    messageService.publishReplyMessage(replyMessageBody, jsonMessage, endpointAnnotation.type());
                }
            } catch (Exception e) {
                //Handle exceptions
                System.err.println("Error occurred while invoking endpoint method: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
