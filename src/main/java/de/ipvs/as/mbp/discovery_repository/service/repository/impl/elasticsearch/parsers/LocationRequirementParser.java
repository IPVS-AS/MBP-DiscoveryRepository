package de.ipvs.as.mbp.discovery_repository.service.repository.impl.elasticsearch.parsers;

import de.ipvs.as.mbp.discovery_repository.service.repository.impl.elasticsearch.util.ParserUtils;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.geometry.LinearRing;
import org.elasticsearch.geometry.Polygon;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.GeoDistanceQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for description requirements.
 */
public class LocationRequirementParser implements RequirementParser {

    //Type name of the requirements that are supported by this parser
    private static final String TYPE_NAME = "location";

    //Tolerance to use for at_location queries
    private static final double DISTANCE_TOLERANCE = 20.0; //meters

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
        //Retrieve operator and details
        String operator = requirement.optString("operator");
        JSONObject details = requirement.optJSONObject("details");

        //Sanity checks
        if ((operator == null) || (details == null) || (details.isEmpty())) {
            return;
        }

        //Switch by the operator
        switch (operator.toLowerCase()) {
            case "described_by":
                ParserUtils.createWildcardQuery(boolQuery, details, "location.description");
                break;
            case "at_location":
                createAtLocationQuery(boolQuery, details);
                break;
            case "in_area":
                createInAreaQuery(boolQuery, details);
            default:
                return;
        }
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

    private void createAtLocationQuery(BoolQueryBuilder boolQuery, JSONObject details) {
        //Sanity check
        if ((!details.has("lat")) || (!details.has("lon"))) {
            return;
        }

        //Retrieve latitude and longitude from the details
        double latitude = details.optDouble("lat");
        double longitude = details.optDouble("lon");

        //Create distance query
        GeoDistanceQueryBuilder query = QueryBuilders.geoDistanceQuery("location.coordinates")
                .point(latitude, longitude)
                .distance(20, DistanceUnit.METERS);

        //Add query to the bool query
        boolQuery.filter(query);
    }

    private void createInAreaQuery(BoolQueryBuilder boolQuery, JSONObject details) {
        //Query to create
        QueryBuilder query;
        try {
            //Check whether circle or polygon
            if (details.has("radius") && details.has("lat") && details.has("lon")) {
                //Circle area, retrieve parameters
                double latitude = details.optDouble("lat");
                double longitude = details.optDouble("lon");
                double radius = details.optDouble("radius");

                //Create distance query
                query = QueryBuilders.geoDistanceQuery("location.coordinates").point(latitude, longitude)
                        .distance(radius, DistanceUnit.METERS);
            } else if (details.has("polygon")) {
                //Get polygon points
                JSONArray points = details.getJSONArray("polygon");

                //Create list of doubles for lon/lat
                List<Double> longitudes = new ArrayList<>();
                List<Double> latitudes = new ArrayList<>();

                //Populate the coordinate lists
                points.forEach(p -> {
                    longitudes.add(((JSONArray) p).getDouble(0));
                    latitudes.add(((JSONArray) p).getDouble(1));
                });

                //Create query for polygon
                query = QueryBuilders.geoWithinQuery("location.coordinates", new Polygon(new LinearRing(
                        longitudes.stream().mapToDouble(Double::doubleValue).toArray(),
                        latitudes.stream().mapToDouble(Double::doubleValue).toArray())));

            } else {
                //Unknown area
                return;
            }

            //Add query to the bool query
            boolQuery.filter(query);
        } catch (Exception ignored) {
        }
    }
}
