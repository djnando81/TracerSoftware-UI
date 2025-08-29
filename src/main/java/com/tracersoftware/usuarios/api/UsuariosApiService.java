package com.tracersoftware.usuarios.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracersoftware.api.ApiClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UsuariosApiService {

    private final ApiClient apiClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public UsuariosApiService() {
        this.apiClient = new ApiClient();
    }

    // Lista todos los usuarios
    // Server-side paged list: requests /api/usuarios?page={page}&size={size}
    public PagedResult listUsersPaged(int page, int size) throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        String path = String.format("/api/usuarios?page=%d&size=%d", page, size);
        JsonNode root = apiClient.getJson(path);
        // If backend still returns an array (legacy), convert to PagedResult simple wrapper
        if (root.isArray()) {
            List<JsonNode> out = new ArrayList<>();
            root.forEach(out::add);
            return new PagedResult(0, out.size(), out.size(), 1, out);
        }

        int p = root.has("page") ? root.path("page").asInt(0) : 0;
        int s = root.has("size") ? root.path("size").asInt(size) : size;
        int total = root.has("totalItems") ? root.path("totalItems").asInt(0) : 0;
        int totalPages = root.has("totalPages") ? root.path("totalPages").asInt((total + s - 1) / Math.max(1, s)) : ((total + s - 1) / Math.max(1, s));
        List<JsonNode> items = new ArrayList<>();
        if (root.has("items") && root.path("items").isArray()) {
            root.path("items").forEach(items::add);
        }
        return new PagedResult(p, s, total, totalPages, items);
    }

    public JsonNode getUser(int id) throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        return apiClient.getJson("/api/usuarios/" + id);
    }

    public JsonNode createUser(JsonNode payload) throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
    String json = payload.toString();
    return apiClient.postJson("/api/usuarios", json);
    }

    public JsonNode updateUser(int id, JsonNode payload) throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        String json = payload.toString();
    // Use PUT to update user by id
    return apiClient.putJson("/api/usuarios/" + id, json);
    }

    public void deleteUser(int id) throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        apiClient.deleteJson("/api/usuarios/" + id);
    }
}
