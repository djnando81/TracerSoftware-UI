package com.tracersoftware.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class ReportsService {

    private final ApiClient client;

    public ReportsService(ApiClient client) {
        this.client = client;
    }

    public JsonNode getHealth() throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        return client.getJson("/api/apiinfo/health");
    }
}
