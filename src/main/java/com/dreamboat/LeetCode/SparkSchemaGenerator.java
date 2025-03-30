package com.dreamboat.LeetCode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SparkSchemaGenerator {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ObjectNode generateSchema(JsonNode jsonNode) {
        ObjectNode schemaNode = objectMapper.createObjectNode();
        schemaNode.put("type", "struct");
        ArrayNode fieldsArray = objectMapper.createArrayNode();

        if (jsonNode.isObject()) {
            Iterator<String> fieldNames = jsonNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldValue = jsonNode.get(fieldName);

                ObjectNode fieldNode = objectMapper.createObjectNode();
                fieldNode.put("name", fieldName);

                if (fieldValue.isObject()) {
                    fieldNode.setAll(generateSchema(fieldValue));
                    fieldNode.set("metadata", objectMapper.createObjectNode());
                    fieldNode.put("nullable", true);
                } else if (fieldValue.isArray()) {
                    fieldNode.set("type", generateArraySchema(fieldValue));
                    fieldNode.set("metadata", objectMapper.createObjectNode());
                    fieldNode.put("nullable", true);
                } else {
                    fieldNode.put("dataType", getDataType(fieldValue));
                    fieldNode.set("metadata", objectMapper.createObjectNode());
                    fieldNode.put("nullable", true);
                }

                fieldsArray.add(fieldNode);
            }
        }

        schemaNode.set("fields", fieldsArray);
        return schemaNode;
    }

    private static ObjectNode generateArraySchema(JsonNode arrayNode) {
        ObjectNode arraySchema = objectMapper.createObjectNode();
        arraySchema.put("type", "array");
        ObjectNode elementType = objectMapper.createObjectNode();
        JsonNode mergedFields = null;
        boolean isStruct = false;
        if(arrayNode.size()>1 && arrayNode.get(0).isObject())
        {
            mergedFields= mergeJsonArray(arrayNode);
            for (JsonNode element : mergedFields) {
                if (element.isObject()) {
                    isStruct = true;
                    elementType = generateSchema(element);
                    break; // Assuming consistent structure within array elements//
                }
            }

            if (!isStruct && !elementType.isEmpty()) {
                elementType.put("dataType", "string"); // Defaulting to string for primitive types
                elementType.set("metadata", objectMapper.createObjectNode());
                elementType.put("nullable", true);
            }
            arraySchema.set("elementType", elementType);
            arraySchema.put("containsNull", true);
        }
        else
        {
            for (JsonNode element : arrayNode) {
                if (element.isObject()) {
                isStruct = true;
                elementType = generateSchema(element);
                break; // Assuming consistent structure within array elements//
                }
            }

        if (!isStruct && !elementType.isEmpty()) {
            elementType.put("dataType", "string"); // Defaulting to string for primitive types
            elementType.set("metadata", objectMapper.createObjectNode());
            elementType.put("nullable", true);
        }
        arraySchema.set("elementType", elementType);
        arraySchema.put("containsNull", true);
        }
        return arraySchema;
    }

    public static JsonNode mergeJsonArray(JsonNode jsonArray) {
        ObjectNode mergedObject = objectMapper.createObjectNode();
        Set<String> processedFields = new HashSet<>();

        for (JsonNode jsonNode : jsonArray) {
            mergeObjects(mergedObject, jsonNode, processedFields);
        }
        return objectMapper.createArrayNode().add(mergedObject);
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

    private static String getDataType(JsonNode node) {
        if (node.isInt()) return "integer";
        if (node.isLong()) return "long";
        if (node.isDouble()) return "double";
        if (node.isBoolean()) return "boolean";
        return "string"; // Default to string
    }

    private static void saveSchemaToFile(ObjectNode schema, String filePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Files.write(Paths.get(filePath), mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(schema));
    }

    public static void main(String[] args) throws IOException {
        String jsonResponse = "{ \"orderId\": \"12345\", \"customer\": { \"customerId\": \"C001\", \"name\": { \"first\": \"John\", \"middle\": \"A\", \"last\": \"Doe\" }, \"contactDetails\": { \"email\": \"john.doe@example.com\", \"phoneNumbers\": [ { \"type\": \"home\", \"number\": \"+1-202-555-0123\" }, { \"type\": \"work\", \"number\": \"+1-202-555-0456\", \"extension\": \"789\" } ] }, \"addresses\": [ { \"type\": \"billing\", \"street\": \"123 Main St\", \"city\": \"New York\", \"state\": \"NY\", \"zipCode\": \"10001\", \"country\": \"USA\" }, { \"type\": \"shipping\", \"street\": \"456 Market St\", \"city\": \"San Francisco\", \"state\": \"CA\", \"zipCode\": \"94105\", \"country\": \"USA\" } ], \"preferences\": { \"newsletterSubscribed\": true, \"preferredLanguage\": \"en\", \"paymentMethods\": [ \"Credit Card\", \"PayPal\" ] } }, \"items\": [ { \"productId\": \"P2002\", \"name\": \"Smartphone\", \"category\": \"Electronics\", \"quantity\": 1, \"price\": 699.99, \"attributes\": { \"brand\": \"XYZ\", \"storage\": \"128GB\", \"color\": \"Blue\", \"features\": [ \"5G\", \"OLED Display\", \"Fast Charging\" ] } }, { \"productId\": \"P1001\", \"name\": \"Wireless Headphones\", \"category\": \"Electronics\", \"quantity\": 2, \"price\": 99.99, \"discounts\": [ { \"type\": \"percentage\", \"value\": 10 }, { \"type\": \"fixed\", \"value\": 5 } ], \"attributes\": { \"color\": \"Black\", \"batteryLife\": \"20 hours\", \"features\": [ \"Noise Cancelling\", \"Bluetooth 5.0\" ] } } ], \"orderStatus\": \"Shipped\", \"shippingDetails\": { \"carrier\": \"UPS\", \"trackingNumber\": \"1Z999AA10123456784\", \"estimatedDelivery\": \"2025-04-02\", \"deliveryAddress\": { \"street\": \"456 Market St\", \"city\": \"San Francisco\", \"state\": \"CA\", \"zipCode\": \"94105\", \"country\": \"USA\" } }, \"payment\": { \"method\": \"Credit Card\", \"transactionId\": \"TXN987654321\", \"amount\": 884.98, \"currency\": \"USD\", \"billingAddress\": { \"street\": \"123 Main St\", \"city\": \"New York\", \"state\": \"NY\", \"zipCode\": \"10001\", \"country\": \"USA\" } }, \"metadata\": { \"createdAt\": \"2025-03-30T14:30:00Z\", \"updatedAt\": \"2025-03-31T08:15:00Z\", \"notes\": [ { \"timestamp\": \"2025-03-30T15:00:00Z\", \"author\": \"Customer Service\", \"message\": \"Customer requested expedited shipping.\" }, { \"timestamp\": \"2025-03-31T07:45:00Z\", \"author\": \"Warehouse\", \"message\": \"Order packed and shipped via UPS.\" } ] } }";

        String outputFilePath = "spark_schema.json";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonResponse);
        ObjectNode schema = generateSchema(jsonNode);
        saveSchemaToFile(schema, outputFilePath);
        System.out.println("Schema saved to: " + outputFilePath);
    }
}
