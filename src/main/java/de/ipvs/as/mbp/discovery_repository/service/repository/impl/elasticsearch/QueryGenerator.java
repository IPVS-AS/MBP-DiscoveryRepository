package de.ipvs.as.mbp.discovery_repository.service.repository.impl.elasticsearch;

import de.ipvs.as.mbp.discovery_repository.service.repository.impl.elasticsearch.parsers.RequirementParser;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.json.JSONArray;
import org.json.JSONObject;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class provides methods that allow to generate {@link BoolQueryBuilder}s from given {@link JSONArray}s
 * of requirements and scoring criteria.
 */
public class QueryGenerator {

    //Package in which the requirement parsers can be found
    private static final String PARSER_PACKAGE = "de.ipvs.as.mbp.discovery_repository.service.repository.impl.elasticsearch.parsers";

    //Map (requirement type name --> parser) for retrieving fitting parsers
    private final static Map<String, RequirementParser> REQUIREMENT_PARSERS = new HashMap<>();

    static {
        //Get all available requirement parser classes
        Reflections reflections = new Reflections(PARSER_PACKAGE);
        Set<Class<? extends RequirementParser>> parserClasses = reflections.getSubTypesOf(RequirementParser.class);

        //Iterate over all requirement classes
        for (Class<? extends RequirementParser> parserClass : parserClasses) {
            try {
                //Create new instance of the requirement parser
                RequirementParser parser = parserClass.getDeclaredConstructor().newInstance();

                //Add parser to map, together with the requirement type for which it is responsible
                REQUIREMENT_PARSERS.put(parser.getRequirementTypeName().toLowerCase(), parser);
            } catch (Exception ignore) {
            }
        }
    }


    /**
     * Generates an equivalent {@link BoolQueryBuilder} from a given query, consisting out of a {@link JSONArray}
     * of requirements and a {@link JSONArray} of scoring criteria.
     *
     * @param requirements    The requirements of the query
     * @param scoringCriteria The scoring criteria of the query (ignored for now)
     * @return The resulting boolean query
     */
    public static BoolQueryBuilder generate(JSONArray requirements, JSONArray scoringCriteria) {
        //Sanity checks
        if (requirements == null) {
            requirements = new JSONArray();
        }
        if (scoringCriteria == null) {
            scoringCriteria = new JSONArray();
        }

        //Create bool query builder that serves as foundation for adding requirements
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        //Iterate through all requirements
        for (Object requirement : requirements) {
            //Skip requirement if not a JSONObject
            if (!(requirement instanceof JSONObject)) {
                continue;
            }

            //Cast to JSONObject
            JSONObject requirementJSON = (JSONObject) requirement;

            //Get requirement type
            String requirementType = requirementJSON.optString("type");

            //Check for valid and known requirement type
            if ((requirementType == null) || (!REQUIREMENT_PARSERS.containsKey(requirementType))) {
                continue;
            }

            //Get the fitting parser, parse the requirement and extend the bool query accordingly
            REQUIREMENT_PARSERS.get(requirementType).parse(boolQuery, requirementJSON);
        }

        //Return the resulting bool query
        return boolQuery;
    }
}
