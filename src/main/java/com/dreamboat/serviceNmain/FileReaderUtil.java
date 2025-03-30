package com.dreamboat.serviceNmain;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FileReaderUtil {

    public static String readInputStream(InputStream inputStream) throws IOException {
        // Use BufferedReader to read the stream
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n"); // Adding new line character if required
            }
            return stringBuilder.toString();
        }
    }
}

