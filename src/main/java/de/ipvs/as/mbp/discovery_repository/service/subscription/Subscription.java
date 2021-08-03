package de.ipvs.as.mbp.discovery_repository.service.subscription;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Objects;

/**
 * Objects of this class represent subscriptions of the IoT platform at the repository, such that the IoT platform
 * is notified asynchronously when the results for a certain query change.
 */
public class Subscription {
    //The topic to which the asynchronous notifications are supposed to be sent
    private String notificationTopic;

    //The reference ID to use in the notification messages
    private String referenceId;

    //The requirements of the query
    private JSONArray requirements;

    //The scoring criteria of the query
    private JSONArray scoringCriteria;

    //The most recent query result
    private List<JSONObject> queryResult;

    /**
     * Creates a new, incomplete {@link Subscription} from a given reference ID.
     *
     * @param referenceId The reference ID to use
     */
    public Subscription(String referenceId) {
        setReferenceId(referenceId);
    }

    /**
     * Creates a new {@link Subscription} object from a given return topic, a reference ID, an array of
     * query requirements, an array of scoring criteria and a list containing the most recent results for the query.
     *
     * @param notificationTopic The return topic to which the asynchronous notifications are supposed to be sent
     * @param referenceId       The reference ID to use in the notification messages
     * @param requirements      The requirements of the query
     * @param scoringCriteria   The scoring criteria of the query
     * @param queryResult       The most recent query result
     */
    public Subscription(String notificationTopic, String referenceId, JSONArray requirements, JSONArray scoringCriteria, List<JSONObject> queryResult) {
        setNotificationTopic(notificationTopic);
        setReferenceId(referenceId);
        setRequirements(requirements);
        setScoringCriteria(scoringCriteria);
        setQueryResult(queryResult);
    }

    /**
     * Returns the notification topic under which the asynchronous notifications are supposed to be published.
     *
     * @return The notification topic
     */
    public String getNotificationTopic() {
        return notificationTopic;
    }

    /**
     * Sets the notification topic under which the asynchronous notifications are supposed to be published.
     *
     * @param notificationTopic The notification topic to set
     * @return The {@link Subscription}
     */
    public Subscription setNotificationTopic(String notificationTopic) {
        //Sanity checks
        if ((notificationTopic == null) || notificationTopic.isEmpty())
            throw new IllegalArgumentException("The return topic must not be null or empty.");

        this.notificationTopic = notificationTopic;
        return this;
    }

    /**
     * Returns the reference ID to use in the notification messages.
     *
     * @return The reference ID
     */
    public String getReferenceId() {
        return referenceId;
    }

    /**
     * Sets the reference ID to use in the notification messages.
     *
     * @param referenceId The reference ID to set
     * @return The {@link Subscription}
     */
    public Subscription setReferenceId(String referenceId) {
        //Sanity checks
        if ((referenceId == null) || referenceId.isEmpty())
            throw new IllegalArgumentException("The reference ID must not be null or empty.");

        this.referenceId = referenceId;
        return this;
    }

    /**
     * Returns the requirements of the query.
     *
     * @return The requirements
     */
    public JSONArray getRequirements() {
        return requirements;
    }

    /**
     * Sets the requirements of the query.
     *
     * @param requirements The requirements to set
     * @return The {@link Subscription}
     */
    public Subscription setRequirements(JSONArray requirements) {
        //Null check
        if (requirements == null) throw new IllegalArgumentException("The requirements must not be null.");

        this.requirements = requirements;
        return this;
    }

    /**
     * Returns the scoring criteria of the query.
     *
     * @return The scoring criteria
     */
    public JSONArray getScoringCriteria() {
        return scoringCriteria;
    }

    /**
     * Sets the scoring criteria of the query.
     *
     * @param scoringCriteria The scoring criteria to set
     * @return The {@link Subscription}
     */
    public Subscription setScoringCriteria(JSONArray scoringCriteria) {
        //Null check
        if (scoringCriteria == null) throw new IllegalArgumentException("The scoring criteria must not be null.");

        this.scoringCriteria = scoringCriteria;
        return this;
    }

    /**
     * Returns the most recent result of the query.
     *
     * @return The query result
     */
    public List<JSONObject> getQueryResult() {
        return queryResult;
    }

    /**
     * Sets the most recent result of the query.
     *
     * @param queryResult The query result to set
     * @return The {@link Subscription}
     */
    public Subscription setQueryResult(List<JSONObject> queryResult) {
        //Null check
        if (queryResult == null) throw new IllegalArgumentException("The query result most not be null.");

        this.queryResult = queryResult;
        return this;
    }

    /**
     * Checks and returns whether a given {@link Object} is equal to this {@link Subscription}. For this, the
     * reference ID of the {@link Subscription} is compared.
     *
     * @param o The object to check against
     * @return True, if the given {@link Object} equals this {@link Subscription}; false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Subscription)) return false;
        Subscription that = (Subscription) o;
        return Objects.equals(referenceId, that.referenceId);
    }

    /**
     * Calculates a hash code from the reference ID of the {@link Subscription}.
     *
     * @return The resulting hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(referenceId);
    }
}
