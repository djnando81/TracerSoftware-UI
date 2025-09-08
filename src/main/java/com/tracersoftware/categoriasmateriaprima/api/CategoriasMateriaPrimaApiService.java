package com.tracersoftware.categoriasmateriaprima.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tracersoftware.api.ApiClient;
import com.tracersoftware.categoriasmateriaprima.model.CategoriaMateriaPrimaItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CategoriasMateriaPrimaApiService {
    private final ApiClient api = new ApiClient();

    public List<CategoriaMateriaPrimaItem> listAll() throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        JsonNode root;
        try { root = api.getJson("/api/v1/categorias-materia-prima"); }
        catch (IOException ex) { 
            try { root = api.getJson("/api/CategoriasMateriaPrima"); }
            catch (IOException ex2) { root = api.getJson("/api/categoriasmateriaprima"); }
        }
        List<CategoriaMateriaPrimaItem> out = new ArrayList<>();
        if (root != null && root.isArray()) {
            for (JsonNode n : root) out.add(toItem(n));
        }
        return out;
    }

    public JsonNode getById(int id) throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        try { return api.getJson("/api/v1/categorias-materia-prima/" + id); }
        catch (IOException ex) { 
            try { return api.getJson("/api/CategoriasMateriaPrima/" + id); }
            catch (IOException ex2) { return api.getJson("/api/categoriasmateriaprima/" + id); }
        }
    }

    public JsonNode create(JsonNode payload) throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        try { return api.postJson("/api/v1/categorias-materia-prima", payload.toString()); }
        catch (IOException ex) { 
            try { return api.postJson("/api/CategoriasMateriaPrima", payload.toString()); }
            catch (IOException ex2) { return api.postJson("/api/categoriasmateriaprima", payload.toString()); }
        }
    }

    public JsonNode update(int id, JsonNode payload) throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        try { return api.putJson("/api/v1/categorias-materia-prima/" + id, payload.toString()); }
        catch (IOException ex) { 
            try { return api.putJson("/api/CategoriasMateriaPrima/" + id, payload.toString()); }
            catch (IOException ex2) { return api.putJson("/api/categoriasmateriaprima/" + id, payload.toString()); }
        }
    }

    public void delete(int id) throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        try { api.deleteJson("/api/v1/categorias-materia-prima/" + id); }
        catch (IOException ex) { 
            try { api.deleteJson("/api/CategoriasMateriaPrima/" + id); }
            catch (IOException ex2) { api.deleteJson("/api/categoriasmateriaprima/" + id); }
        }
    }

    public JsonNode toggleEstado(int id, boolean activa) throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        com.fasterxml.jackson.databind.node.ObjectNode body = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        body.put("activa", activa);
        try { return api.putJson("/api/v1/categorias-materia-prima/" + id + "/toggle-estado", body.toString()); }
        catch (IOException ex) { 
            try { return api.putJson("/api/CategoriasMateriaPrima/" + id + "/toggle-estado", body.toString()); }
            catch (IOException ex2) { return api.putJson("/api/categoriasmateriaprima/" + id + "/toggle-estado", body.toString()); }
        }
    }

    private CategoriaMateriaPrimaItem toItem(JsonNode n) {
        CategoriaMateriaPrimaItem c = new CategoriaMateriaPrimaItem();
        c.setId(n.path("id").asInt(0));
        c.setNombre(n.path("nombre").asText(""));
        boolean a = n.has("activa") ? n.path("activa").asBoolean(false) : n.path("activo").asBoolean(false);
        c.setActiva(a);
        return c;
    }
}

