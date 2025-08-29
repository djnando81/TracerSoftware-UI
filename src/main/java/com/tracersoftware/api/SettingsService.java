package com.tracersoftware.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class SettingsService {

    private final ApiClient client;

    public SettingsService(ApiClient client) {
        this.client = client;
    }

    public JsonNode getAllSettings() throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        return client.getJson("/api/configuracionsistema");
    }
}
