package de.ipvs.as.mbp.discovery_repository.service.repository.impl.elasticsearch.parsers;

import org.elasticsearch.index.query.*;
import org.json.JSONObject;

/**
 * Parser for description requirements.
 */
public class DescriptionRequirementParser implements RequirementParser {

    //Type name of the requirements that are supported by this parser
    private static final String TYPE_NAME = "description";

    /**
     * Parses a requirement, given as {@link JSONObject}, of the supported requirement type, transforms it to a
     * corresponding {@link QueryBuilder} that implements this requirement and adds it as clause to a given
     * {@link BoolQueryBuilder}.
     *
     * @param boolQuery   The boolean query to extend for the requirement
     * @param requirement The requirement to parse and transform
     */
    @Override
    public void parse(BoolQueryBuilder boolQuery, JSONObject requirement) {
        //Retrieve match field and operator
        String match = requirement.optString("match");
        String operator = requirement.optString("operator");

        //Create query
        MatchQueryBuilder matchQuery = QueryBuilders.matchQuery("description", match).operator(Operator.fromString(operator));

        //Add query to the bool query
        boolQuery.filter(matchQuery);
    }

    /**
     * Returns the type name of the requirements that can be parsed and transformed to {@link QueryBuilder}s by using
     * this requirement parser.
     *
     * @return The requirement type name
     */
    @Override
    public String getRequirementTypeName() {
        return TYPE_NAME;
    }
}
