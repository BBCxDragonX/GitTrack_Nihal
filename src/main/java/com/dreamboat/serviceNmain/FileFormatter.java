package com.dreamboat.serviceNmain;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

 class FileFormatterAPI {

    public static void main(String[] args) throws Exception {
        // Start HTTP server
        HttpServer server = HttpServer.create(new java.net.InetSocketAddress(8000), 0);
        server.createContext("/format-file", new FormatFileHandler());
        server.setExecutor(null); // Creates a default executor
        server.start();
        System.out.println("Server is running on http://localhost:8000/format-file");
    }

    static class FormatFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Only handle POST requests
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream inputStream = exchange.getRequestBody();
                String inputFilePath = FileReaderUtil.readInputStream(inputStream);
                System.out.println("Received file path: " + inputFilePath);




                // Perform file formatting
                String result = formatFile(inputFilePath);
                System.out.println("Formatting complete, sending response...");

                // Send response
                exchange.sendResponseHeaders(200, result.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(result.getBytes());
                os.close();
            } else {
                // Only support POST method
                String response = "Only POST method is supported";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    private static String formatFile(String inputFilePath) {
        // Spark session and context initialization
        SparkSession spark = SparkSession.builder()
                .appName("Fixed Width Formatter")
                .master("local[*]") // For local testing; use appropriate cluster setup in production
                .config("spark.executor.memory", "4g")
                .config("spark.driver.memory", "4g")
                .getOrCreate();
        JavaSparkContext sc = new JavaSparkContext(spark.sparkContext());

        // Output file path
        String outputFilePath = "D:\\AppFiles\formatted_data.txt";

        // Fixed column widths and delimiter
        List<Integer> columnWidths = Arrays.asList(10, 15, 20); // Define fixed widths for each column
        String delimiter = ",";

        System.out.println("Starting to read the file...");

        // Read the input file as an RDD
        JavaRDD<String> inputRDD = sc.textFile("D:\\AppFiles\\textInput.txt");

        System.out.println("File read successfully. Starting formatting...");

        // Process each row to format the columns to fixed widths
        JavaRDD<String> formattedRDD = inputRDD.map(line -> {
            // Split the line by the delimiter
            String[] columns = line.split(delimiter);

            // Format each column to the specified width
            StringBuilder formattedLine = new StringBuilder();
            for (int i = 0; i < columns.length; i++) {
                String column = columns[i];
                int width = columnWidths.get(i);

                // Truncate or pad the column to fit the width
                String formattedColumn = column.length() > width
                        ? column.substring(0, width)  // Truncate if it's too long
                        : String.format("%-" + width + "s", column); // Pad with spaces if it's too short

                formattedLine.append(formattedColumn);

                // Add the delimiter after each column except the last
                if (i < columns.length - 1) {
                    formattedLine.append(delimiter);
                }
            }
            return formattedLine.toString();
        });

        // Save the formatted data to the output file
        formattedRDD.saveAsTextFile(outputFilePath);

        // Stop the Spark context
        sc.close();

        return "File formatting completed successfully!";
    }
}
