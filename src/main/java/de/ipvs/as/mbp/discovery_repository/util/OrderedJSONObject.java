package de.ipvs.as.mbp.discovery_repository.util;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;

/**
 * Subclass of {@link JSONObject} that in contrast to {@link JSONObject} uses a linked hash map in order to keep
 * and maintain the order of JSON keys.
 */
public class OrderedJSONObject extends JSONObject {

    /**
     * Construct a JSONObject from a JSONTokener.
     *
     * @param x A JSONTokener object containing the source string.
     * @throws JSONException If there is a syntax error in the source string or a
     *                       duplicated key.
     */
    public OrderedJSONObject(JSONTokener x) throws JSONException {
        //Delegate to the super constructor
        super(x);
    }

    /**
     * Put a key/value pair in the JSONObject. If the value is <code>null</code>, then the
     * key will be removed from the JSONObject if it is present.
     *
     * @param key   A key string.
     * @param value An object which is the value. It should be of one of these
     *              types: Boolean, Double, Integer, JSONArray, JSONObject, Long,
     *              String, or the JSONObject.NULL object.
     * @return this.
     * @throws JSONException        If the value is non-finite number.
     * @throws NullPointerException If the key is <code>null</code>.
     */
    @Override
    public JSONObject put(String key, Object value) throws JSONException {
        try {
            //Get the usually used map of the JSONObject
            Field previousMap = JSONObject.class.getDeclaredField("map");
            previousMap.setAccessible(true);
            Object mapValue = previousMap.get(this);

            //Replace the map with a linked hash map if not already done
            if (!(mapValue instanceof LinkedHashMap)) {
                previousMap.set(this, new LinkedHashMap<>());
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            //Delegate exceptions
            throw new RuntimeException(e);
        }
        //Add key value pair
        return super.put(key, value);
    }
}
