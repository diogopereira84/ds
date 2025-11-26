package fox.fmc.partner.delivery.test.utils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fox.fmc.partner.delivery.test.constants.BaseConstants;
import fox.fmc.partner.delivery.test.model.map.MapOfObjects;
import groovy.lang.MissingPropertyException;
import io.restassured.path.json.JsonPath;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;

public class JsonObjectMapper {
    /**
     * Method that iterates the jsonObject map and replaces values based on keywords
     */
    public static MapOfObjects evaluateMapForKeywords(MapOfObjects jsonObject) {
        Iterator<Map.Entry<String, Object>> it = jsonObject.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> pair = it.next();
            // Only evaluate non-null values (null is valid)
            if (pair.getValue() != null) {
                if (pair.getValue().toString().contains("NOT-PRESENT")) {
                    it.remove();
                } else if (pair.getValue().toString().equalsIgnoreCase("NULL")) {
                    pair.setValue(null);
                } else if (pair.getValue().toString().equalsIgnoreCase("EMPTY")) {
                    pair.setValue("");
                } else if (pair.getValue().toString().equalsIgnoreCase("TRUE")) {
                    pair.setValue(true);
                } else if (pair.getValue().toString().equalsIgnoreCase("FALSE")) {
                    pair.setValue(false);
                } else if (pair.getValue().toString().contains("SPACES:")) {
                    int spaceValue = Integer.parseInt(pair.getValue().toString().split(":")[1]);
                    pair.setValue(StringUtils.repeat(" ", spaceValue));
                } else if (pair.getValue().toString().replaceAll("\\s+", "").startsWith("{\"") && pair.getValue().toString().endsWith("}")) {
                    // Detect and handle JSON input strings
                    JsonPath json = new JsonPath(pair.getValue().toString());
                    Map jsonMap = json.getMap("$");
                    pair.setValue(jsonMap);
                } else if (pair.getValue().toString().contains("PADSTRING:") || pair.getValue().toString().contains("PADNUM:")) {
                    String[] strArr = pair.getValue().toString().split(":");
                    int repValue = Integer.parseInt(strArr[2]);
                    int divValue = repValue / strArr[1].length();
                    int truncValue = repValue % strArr[1].length();
                    String finalValue = StringUtils.repeat(strArr[1], divValue) + strArr[1].subSequence(0, truncValue);
                    if (pair.getValue().toString().contains("PADSTRING:")) {
                        pair.setValue(finalValue);
                    } else if (pair.getValue().toString().contains("PADNUM:")) {
                        pair.setValue(Long.parseLong(finalValue));
                    }
                } else if (pair.getValue().toString().contains("LITERAL")) {
                    if (pair.getValue() instanceof Integer) {
                        pair.setValue(new ArrayList<String>());
                    } else if (pair.getValue() instanceof String) {
                        pair.setValue(new ArrayList<Integer>());
                    }
                } else if (pair.getValue().toString().contains("NUMBER:")) {
                    String[] strArr = pair.getValue().toString().split(":");
                    pair.setValue(Integer.parseInt(strArr[1]));
                } else if (pair.getValue().toString().contains("EMPTYOBJECT")) {
                    MapOfObjects m = new MapOfObjects();
                    pair.setValue(m);
                }
            }
        }
        return jsonObject;
    }

    /**
     * Method that updates a list based on the presence of keywords
     */
    public static List<Object> evaluateListForKeywords(List<Object> list) {
        if (list != null) {
            for (ListIterator<Object> iter = list.listIterator(); iter.hasNext(); ) {
                Object o = iter.next();
                if (o == null) {
                    iter.remove();
                } else if (o.toString().equalsIgnoreCase("NULL")) {
                    iter.set(null);
                } else if (o.toString().equalsIgnoreCase("EMPTY")) {
                    iter.set("");
                } else if (o.toString().equalsIgnoreCase("NOT-PRESENT")) {
                    iter.remove();
                } else if (o.toString().equalsIgnoreCase("TRUE")) {
                    iter.set(true);
                } else if (o.toString().equalsIgnoreCase("FALSE")) {
                    iter.set(false);
                } else if (o.toString().contains("SPACES:")) {
                    int spaceValue = Integer.parseInt(o.toString().split(":")[1]);
                    iter.set(StringUtils.repeat(" ", spaceValue));
                } else if (o.toString().contains("NUMBER:")) {
                    String[] strArr = o.toString().split(":");
                    iter.set(Integer.parseInt(strArr[1]));
                } else if (o.toString().contains("PAD")) {
                    String[] strArr = o.toString().split(":");
                    int repValue = Integer.parseInt(strArr[2]);
                    int divValue = repValue / strArr[1].length();
                    int truncValue = repValue % strArr[1].length();
                    String finalValue = StringUtils.repeat(strArr[1], divValue) + strArr[1].subSequence(0, truncValue);
                    if (o.toString().contains("PADSTRING:")) {
                        iter.set(finalValue);
                    } else if (o.toString().contains("PADNUM:")) {
                        iter.set(Long.parseLong(finalValue));
                    }
                }
            }
        }
        return list;
    }

    /**
     * Method that iterates the a response JSON outRequest/outResponse map and replaces values with keywords
     */
    public static MapOfObjects checkOutputMapForKeywords(MapOfObjects responseMap) {
        for (Map.Entry<String, Object> pair : responseMap.entrySet()) {
            // Replace null values with the string "NOT-PRESENT"
            if (pair.getValue() == null || pair.getValue().toString().equalsIgnoreCase("null")) {
                pair.setValue(BaseConstants.NOT_PRESENT);
            }
        }
        return responseMap;
    }

    /**
     * Object Mapper used to build the JSON string
     */
    public static String writeMapAsJson(Map jsonObject) {
        return writeMapAsJson(jsonObject, true);
    }

    public static String writeMapAsJson(Map jsonObject, Boolean writeNullMapValuesBool) {
        try {
            return new ObjectMapper()
                    .configure(SerializationFeature.WRITE_NULL_MAP_VALUES, writeNullMapValuesBool)
                    .configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, false)
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(jsonObject);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static MapOfObjects jsonStringToMapConverter(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static MapOfObjects jsonToMapFlattener(String json) {
        // Used to convert JSON into a flat map. Note this handles arrays, but it DOES NOT handle duplicate tags
        // that occur at the same level.
        JsonNode jsonNode;
        try {
            jsonNode = new ObjectMapper().readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        MapOfObjects map = new MapOfObjects();
        String currentPath = "";
        return jsonMapperAddKeys(currentPath, jsonNode, map);
    }

    public static MapOfObjects jsonToMapFlattener(String json, String prefixKey) {
        // Used to convert JSON into a flat map. Note this handles arrays, but it DOES NOT handle duplicate tags
        // that occur at the same level.
        JsonNode jsonNode;
        try {
            jsonNode = new ObjectMapper().readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        MapOfObjects map = new MapOfObjects();
        return jsonMapperAddKeys(prefixKey, jsonNode, map);
    }

    //the following jsonToMapFlattener_v2 methods are the same as the jsonToMapFlattener methods above except that
    //they call the jsonMapperAddKeys_v2 instead of jsonMapperAddKeys.
    public static MapOfObjects jsonToMapFlattener_v2(String json) {
        // Used to convert JSON into a flat map. Note this handles arrays, but it DOES NOT handle duplicate tags
        // that occur at the same level.
        JsonNode jsonNode;
        try {
            jsonNode = new ObjectMapper().readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        MapOfObjects map = new MapOfObjects();
        return jsonMapperAddKeys_v2("", jsonNode, map);
    }

    public static MapOfObjects jsonToMapFlattener_v2(String json, String prefixKey) {
        // Used to convert JSON into a flat map. Note this handles arrays, but it DOES NOT handle duplicate tags
        // that occur at the same level.
        JsonNode jsonNode;
        try {
            jsonNode = new ObjectMapper().readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        MapOfObjects map = new MapOfObjects();
        return jsonMapperAddKeys_v2(prefixKey, jsonNode, map);
    }

    private static MapOfObjects jsonMapperAddKeys(String currentPath, JsonNode jsonNode, MapOfObjects map) {
        if (jsonNode.isObject()) {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();
            String pathPrefix = currentPath.isEmpty() ? "" : currentPath + ".";
            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> entry = iter.next();
                jsonMapperAddKeys(pathPrefix + entry.getKey(), entry.getValue(), map);
            }
        } else if (jsonNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) jsonNode;
            for (int i = 0; i < arrayNode.size(); i++) {
                jsonMapperAddKeys(currentPath + "[" + i + "]", arrayNode.get(i), map);
            }
        } else if (jsonNode.isValueNode()) {
            map.put(currentPath, jsonNode.asText());
        }
        return map;
    }

    //This jsonMapperAddKeys_v2 is the same as the jsonMapperAddKeys method above except that it will add
    //entries to the map for object level tags.
    private static MapOfObjects jsonMapperAddKeys_v2(String currentPath, JsonNode jsonNode, MapOfObjects map) {
        if (jsonNode.isObject()) {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();
            String pathPrefix = currentPath.isEmpty() ? "" : currentPath + ".";
            if (!currentPath.isEmpty()) {
                map.put(currentPath, BaseConstants.PRESENT);
            }
            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> entry = iter.next();
                jsonMapperAddKeys_v2(pathPrefix + entry.getKey(), entry.getValue(), map);
            }
        } else if (jsonNode.isArray()) {
            map.put(currentPath, BaseConstants.PRESENT);
            ArrayNode arrayNode = (ArrayNode) jsonNode;
            map.put(currentPath + BaseConstants.KEY_SUFFIX_ARRAY_SIZE, arrayNode.size());
            for (int i = 0; i < arrayNode.size(); i++) {
                jsonMapperAddKeys_v2(currentPath + "[" + i + "]", arrayNode.get(i), map);
            }
        } else if (jsonNode.isValueNode()) {
            map.put(currentPath, jsonNode.asText());
        }
        return map;
    }

    /*
     *  Convert an ArrayList to a delimited string using the standard delimiter
     */
    public static String createStringFromArrayList(List<Object> rawList) {
        if (rawList != null) {
            return StringUtils.join(rawList, BaseConstants.DELIMITER);
        } else {
            return BaseConstants.NOT_PRESENT;
        }
    }

    public static String createStringFromArrayList(JsonPath jsonPath, String path) {
        List<Objects> rawList = jsonPath.getList(path);
        List<String> editedList = new ArrayList<>();
        if (rawList != null) {
            for (int i = 0; i < rawList.size(); i++) {
                String value = BaseConstants.NOT_PRESENT;
                if (rawList.get(i) != null) {
                    value = String.valueOf(rawList.get(i));
                }
                editedList.add(i, value);
            }
            return StringUtils.join(editedList, BaseConstants.DELIMITER);
        } else {
            return BaseConstants.NOT_PRESENT;
        }
    }

    public static String get(JsonPath responseJson, String fieldName) {
        String returnString;
        try {
            returnString = String.valueOf(responseJson.get(fieldName));
        } catch (Exception e) {
            returnString = BaseConstants.NOT_PRESENT;
        }
        return returnString;
    }

    public static String checkObject(JsonPath responseJson, String objectName) {
        // Check for the presence of an object/field in the JSON.
        // Return PRESENT if the value exists and is non-null.
        // Return NOT-PRESENT if value is null or if it causes and exception (not found).
        try {
            return responseJson.getString(objectName) == null ? BaseConstants.NOT_PRESENT : BaseConstants.PRESENT;
        } catch (MissingPropertyException | IllegalArgumentException e) {
            return BaseConstants.NOT_PRESENT;
        }
    }

    public static boolean isValidJson(String json) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(json);
            return true;
        } catch (JsonParseException e) {
            return false;
        } catch (Exception e2) {
            throw new RuntimeException(e2);
        }
    }
}
