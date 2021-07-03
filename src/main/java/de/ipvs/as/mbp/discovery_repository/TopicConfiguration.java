package de.ipvs.as.mbp.discovery_repository;

/**
 * Collection of topic-related configurations, including the topics that are subscribed by the application.
 */
public class TopicConfiguration {
    //Base topic to subscribe
    private static final String SUB_TOPIC_BASE = "+/discovery/+";

    //Topic to subscribe for test requests
    public static final String SUB_TOPIC_TEST = SUB_TOPIC_BASE + "/test";
}
