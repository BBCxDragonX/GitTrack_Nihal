package com.dreamboat.LeetCode;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class ApiConnector {
    public static void main(String[] args) throws IOException {
        SparkSession spark = SparkSession.builder()
                .appName("APIConnector")
                .master("local[*]")
                .getOrCreate();

        String apiUrl = "https://your-api-endpoint"; // Replace with your API endpoint
        String curlCommand = "curl -X GET " + apiUrl;

        // Execute the cURL command using Runtime
        Process process = Runtime.getRuntime().exec(curlCommand);

        // Read the response from the process
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder responseBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            responseBuilder.append(line);

        }
        reader.close();

        // Parse the JSON response using Jackson
        ObjectMapper mapper = new ObjectMapper();
        Object responseObject = mapper.readValue(responseBuilder.toString(), Object.class);

        // Convert the response to a DataFrame
        Dataset<Row> df = spark.read().json(mapper.writeValueAsString(responseObject));

        // Print the schema
        df.printSchema();

        spark.stop();
    }
}
