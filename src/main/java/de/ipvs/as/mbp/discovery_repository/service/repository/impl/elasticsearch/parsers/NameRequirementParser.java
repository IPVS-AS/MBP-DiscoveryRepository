package de.ipvs.as.mbp.discovery_repository.service.repository.impl.elasticsearch.parsers;

import de.ipvs.as.mbp.discovery_repository.service.repository.impl.elasticsearch.util.ParserUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.json.JSONObject;

/**
 * Parser for name requirements.
 */
public class NameRequirementParser implements RequirementParser {

    //Type name of the requirements that are supported by this parser
    private static final String TYPE_NAME = "name";

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
        //Create a corresponding wildcard query and add it to the bool query
        ParserUtils.createWildcardQuery(boolQuery, requirement, "name");
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
