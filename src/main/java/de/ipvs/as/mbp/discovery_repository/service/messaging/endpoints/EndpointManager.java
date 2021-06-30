package de.ipvs.as.mbp.discovery_repository.service.messaging.endpoints;

import de.ipvs.as.mbp.discovery_repository.service.messaging.PubSubService;
import org.json.JSONObject;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * This component is responsible for detecting {@link MessagingController}s in the classpath and forwarding the
 * received messages to their {@link MessagingEndpoint}s.
 */
@Component
public class EndpointManager {

    //Package to scan for MessagingControllers
    private static final String SCAN_PACKAGE = "de.ipvs.as.mbp.discovery_repository";

    /**
     * Creates and initializes the endpoint manager by using a given {@link PubSubService} that enables
     * publish-subscribe-based messaging.
     *
     * @param pubSubService The publish-subscribe-based messaging service to use
     */
    @Autowired
    public EndpointManager(PubSubService pubSubService) {
        //Create reflections object
        Reflections reflections = new Reflections(SCAN_PACKAGE);

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

                //Let the messaging client subscribe to the topic of this method and set a handler
                pubSubService.subscribe(endpointAnnotation.topic(), (topic, message) -> {
                    try {
                        //Create object from the controller class
                        Object object = controller.getConstructor().newInstance();
                        
                        method.getReturnType();

                        //Call the method to handle the message
                        JSONObject returnMessage = (JSONObject) method.invoke(object, topic, new JSONObject(message));
                    } catch (Exception e) {
                        System.err.println("Error occurred while invoking method: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        }
    }
}
