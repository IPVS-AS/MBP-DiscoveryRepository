package de.ipvs.as.mbp.discovery_repository.service.repository.impl.elasticsearch.parsers;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.json.JSONObject;

/**
 * Requirement parsers can be used to parse a requirement, given as {@link JSONObject}, of a certain type and to
 * transform it to a corresponding {@link QueryBuilder} that implements this requirement. The resulting
 * {@link QueryBuilder} is added as clause to a given {@link BoolQueryBuilder}.
 */
public interface RequirementParser {
    /**
     * Parses a requirement, given as {@link JSONObject}, of the supported requirement type, transforms it to a
     * corresponding {@link QueryBuilder} that implements this requirement and adds it as clause to a given
     * {@link BoolQueryBuilder}.
     *
     * @param boolQuery   The boolean query to extend for the requirement
     * @param requirement The requirement to parse and transform
     */
    void parse(BoolQueryBuilder boolQuery, JSONObject requirement);

    /**
     * Returns the type name of the requirements that can be parsed and transformed to {@link QueryBuilder}s by using
     * this requirement parser.
     *
     * @return The requirement type name
     */
    String getRequirementTypeName();
}
