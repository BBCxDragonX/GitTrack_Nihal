package com.dreamboat.LeetCode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class JsonMerger {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static JsonNode mergeJsonArray(JsonNode jsonArray) {
        ObjectNode mergedObject = mapper.createObjectNode();
        Set<String> processedFields = new HashSet<>();

        for (JsonNode jsonNode : jsonArray) {
            mergeObjects(mergedObject, jsonNode, processedFields);
        }
        return mapper.createArrayNode().add(mergedObject);
    }

    private static void mergeObjects(ObjectNode target, JsonNode source, Set<String> processedFields) {
        Iterator<String> fieldNames = source.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode sourceValue = source.get(fieldName);

            if (target.has(fieldName)) {
                JsonNode targetValue = target.get(fieldName);
                if (sourceValue.isObject() && targetValue.isObject()) {
                    mergeObjects((ObjectNode) targetValue, sourceValue, processedFields);
                } else if (sourceValue.isArray() && targetValue.isArray()) {
                    mergeArrays((ArrayNode) targetValue, (ArrayNode) sourceValue);
                }
            } else {
                target.set(fieldName, sourceValue);
                processedFields.add(fieldName);
            }
        }
    }

    private static void mergeArrays(ArrayNode targetArray, ArrayNode sourceArray) {
        Set<String> existingEntries = new HashSet<>();
        for (JsonNode node : targetArray) {
            existingEntries.add(node.toString());
        }
        for (JsonNode node : sourceArray) {
            if (!existingEntries.contains(node.toString())) {
                targetArray.add(node);
                existingEntries.add(node.toString());
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String jsonResponse = "[{\"product_id\": \"PROD002\", \"name\": \"Wireless Mouse\", \"description\": \"Ergonomic wireless mouse.\", \"price\": 25.00, \"attributes\": {\"connectivity\": \"Bluetooth\", \"color\": \"Black\", \"dpi\": \"1600\"}, \"reviews\": []},{\"product_id\": \"PROD001\", \"name\": \"Laptop X\", \"description\": \"A high-performance laptop.\", \"price\": 1200.00, \"attributes\": {\"processor\": \"Intel i7\", \"ram\": \"16GB\", \"storage\": \"512GB SSD\", \"screen_size\": \"15.6 inch\"}, \"reviews\": [{\"user_id\": \"USR67890\", \"rating\": 4, \"comment\": \"Great laptop, fast and reliable.\"}, {\"user_id\": \"USR13579\", \"rating\": 5, \"comment\": \"Excellent value for the price.\"}]}]";

        JsonNode jsonArray = mapper.readTree(jsonResponse);
        JsonNode mergedJson = mergeJsonArray(jsonArray);

        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mergedJson));
    }
}

