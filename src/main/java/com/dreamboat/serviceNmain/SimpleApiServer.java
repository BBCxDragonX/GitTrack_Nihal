package com.dreamboat.serviceNmain;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class SimpleApiServer {
    public static void main(String[] args) throws IOException {
        // Create an HTTP server bound to port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Define the "/hello" endpoint
        server.createContext("/hello", new HelloHandler());

        // Define the "/status" endpoint
        server.createContext("/status", exchange -> {
            String response = "{\"status\":\"API is running\"}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        // Start the server
        server.setExecutor(null); // Default executor
        System.out.println("Server is running on http://localhost:8080/");
        server.start();
    }

    // Handler for "/hello" endpoint
    static class HelloHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = "Hello, World!";
                exchange.getResponseHeaders().add("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method not allowed
            }
        }
    }
}

