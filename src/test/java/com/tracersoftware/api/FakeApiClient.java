package com.tracersoftware.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class FakeApiClient {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonNode getJsonFromTestResource(String name) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/fake-responses/" + name)) {
            if (is == null) throw new IOException("Test resource not found: " + name);
            return objectMapper.readTree(is);
        }
    }
}
