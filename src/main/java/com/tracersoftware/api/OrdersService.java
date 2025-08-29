package com.tracersoftware.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class OrdersService {

    private final ApiClient client;

    public OrdersService(ApiClient client) {
        this.client = client;
    }

    public JsonNode listProductionOrders() throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        return client.getJson("/api/ordenesproduccion");
    }
}
