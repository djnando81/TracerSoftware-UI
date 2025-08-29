package com.tracersoftware.usuarios.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tracersoftware.api.ApiClient;

import java.util.ArrayList;
import java.util.List;

public class RolesApiService {
    private final ApiClient client = new ApiClient();

    public static class RoleItem {
        public int id;
        public String nombre;
        public RoleItem(int id, String nombre) { this.id = id; this.nombre = nombre; }
        public String toString() { return id + ":" + nombre; }
    }

    public List<RoleItem> listRoles() throws Exception {
        JsonNode root = client.getJson("/api/roles");
        List<RoleItem> out = new ArrayList<>();
        if (root.isArray()) {
            for (JsonNode n : root) {
                int id = n.has("id") ? n.path("id").asInt() : n.path("rolId").asInt(0);
                String nombre = n.has("nombre") ? n.path("nombre").asText() : n.path("name").asText("");
                out.add(new RoleItem(id, nombre));
            }
        }
        return out;
    }
}
