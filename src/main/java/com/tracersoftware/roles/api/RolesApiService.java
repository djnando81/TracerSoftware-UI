package com.tracersoftware.roles.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tracersoftware.api.ApiClient;

public class RolesApiService {
    private final ApiClient api = new ApiClient();

    public JsonNode listAll() throws Exception {
        return api.getJson("/api/roles");
    }

    public JsonNode getById(int id) throws Exception {
        return api.getJson("/api/roles/" + id);
    }

    public JsonNode create(JsonNode payload) throws Exception {
        return api.postJson("/api/roles", payload.toString());
    }

    public JsonNode update(int id, JsonNode payload) throws Exception {
        return api.putJson("/api/roles/" + id, payload.toString());
    }

    public void delete(int id) throws Exception {
        api.deleteJson("/api/roles/" + id);
    }
}
