package de.ipvs.as.mbp.discovery_repository.service.repository.impl.elasticsearch.util;


import de.ipvs.as.mbp.discovery_repository.service.repository.impl.elasticsearch.parsers.RequirementParser;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.json.JSONObject;

/**
 * Collection of utility functions related to {@link RequirementParser}s.
 */
public class ParserUtils {

    /**
     * Creates a {@link WildcardQueryBuilder} from a given {@link JSONObject} wrapping the match field and the
     * corresponding string operator, a given field path referencing the field in the device description to which
     * the query is supposed to be applied and a {@link BoolQueryBuilder} to which the resulting wildcard query
     * should be added as clause.
     *
     * @param boolQuery The bool query to extend for the wildcard query
     * @param details   The details {@link JSONObject} wrapping the match field and the operator to use
     * @param fieldPath The path referencing the field within the device descriptions to which the wildcard query
     *                  is supposed to be applied
     */
    public static void createWildcardQuery(BoolQueryBuilder boolQuery, JSONObject details, String fieldPath) {
        //Get match field and operator from the details
        String match = details.optString("match");
        String operator = details.optString("operator");

        //Extend the match field for wildcards, depending on the chosen operator
        String wildcardMatch = ParserUtils.extendWithWildcards(match, operator);

        //Create query
        WildcardQueryBuilder query = QueryBuilders.wildcardQuery("location.description", wildcardMatch).caseInsensitive(true);

        //Add query to the bool query, depending on the operator
        if (operator.equals("not_equals")) {
            boolQuery.mustNot(query);
        } else {
            boolQuery.filter(query);
        }
    }

    /**
     * Extends a given match string for wildcards, such that it behaves like a given operator is applied to it
     * within a query.
     *
     * @param match    The match string to extend
     * @param operator The operator that is supposed to be represented
     * @return The resulting extended match string
     */
    public static String extendWithWildcards(String match, String operator) {
        //Sanity checks
        if (match == null) {
            match = "";
        }
        if (operator == null) {
            return match;
        }

        //Check the operator
        switch (operator.toLowerCase()) {
            case "equals":
            case "not_equals":
                return match;
            case "contains":
                return "*" + match + "*";
            case "begins_with":
                return match + "*";
            case "ends_with":
                return "*" + match;
            default: //Unknown operator
                return match;
        }
    }
}
