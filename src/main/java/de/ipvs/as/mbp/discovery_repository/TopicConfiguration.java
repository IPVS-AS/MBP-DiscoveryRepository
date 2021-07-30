package de.ipvs.as.mbp.discovery_repository;

/**
 * Collection of topic-related configurations, including the topics that are subscribed by the application.
 */
public class TopicConfiguration {
    //Base topic to subscribe
    private static final String SUB_TOPIC_BASE = "+/discovery/+";

    //Topic to subscribe for test requests
    public static final String SUB_TOPIC_TEST = SUB_TOPIC_BASE + "/test";

    //Topic to subscribe for device description query requests
    public static final String SUB_TOPIC_QUERY = SUB_TOPIC_BASE + "/query";

    //Topic to subscribe for cancel subscription requests
    public static final String SUB_TOPIC_CANCEL = SUB_TOPIC_BASE + "/cancel";
}
