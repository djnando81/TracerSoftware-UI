package com.tracersoftware.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class InventoryService {

    private final ApiClient client;

    public InventoryService(ApiClient client) {
        this.client = client;
    }

    public JsonNode getStock() throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        return client.getJson("/api/inventariomateriaprima/stock");
    }
}
