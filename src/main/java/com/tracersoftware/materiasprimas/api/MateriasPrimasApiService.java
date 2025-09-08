package com.tracersoftware.materiasprimas.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tracersoftware.api.ApiClient;
import com.tracersoftware.materiasprimas.model.MateriaPrimaItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MateriasPrimasApiService {
    private final ApiClient client = new ApiClient();

    public List<MateriaPrimaItem> listAll() throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        JsonNode root;
        try {
            root = client.getJson("/api/v1/materias-primas");
        } catch (IOException ex) {
            // fallback legacy path
            root = client.getJson("/api/materiasprimas");
        }
        List<MateriaPrimaItem> out = new ArrayList<>();
        if (root != null && root.isArray()) {
            for (JsonNode n : root) out.add(toItem(n));
        }
        return out;
    }

    public JsonNode getById(int id) throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        try { return client.getJson("/api/v1/materias-primas/" + id); }
        catch (IOException ex) { return client.getJson("/api/materiasprimas/" + id); }
    }

    public JsonNode create(JsonNode payload) throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        try { return client.postJson("/api/v1/materias-primas", payload.toString()); }
        catch (IOException ex) { return client.postJson("/api/materiasprimas", payload.toString()); }
    }

    public JsonNode update(int id, JsonNode payload) throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        try { return client.putJson("/api/v1/materias-primas/" + id, payload.toString()); }
        catch (IOException ex) { return client.putJson("/api/materiasprimas/" + id, payload.toString()); }
    }

    public void delete(int id) throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        try { client.deleteJson("/api/v1/materias-primas/" + id); }
        catch (IOException ex) { client.deleteJson("/api/materiasprimas/" + id); }
    }

    public JsonNode toggleEstado(int id, boolean activo) throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        com.fasterxml.jackson.databind.node.ObjectNode body = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        body.put("activo", activo);
        body.put("estado", activo ? 1 : 0);
        try { return client.putJson("/api/v1/materias-primas/" + id + "/toggle-estado", body.toString()); }
        catch (IOException ex1) {
            try { return client.putJson("/api/materiasprimas/" + id + "/toggle-estado", body.toString()); }
            catch (IOException ex2) { return client.putJson("/api/materiasprimas/" + id + "/toggle-activo", body.toString()); }
        }
    }

    public JsonNode dividir(int id, JsonNode listaDerivadas) throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        // API: POST /api/materiasprimas/{id}/dividir con lista de MateriaPrimaDerivadaCreateDTO
        return client.postJson("/api/materiasprimas/" + id + "/dividir", listaDerivadas.toString());
    }

    private MateriaPrimaItem toItem(JsonNode n) {
        MateriaPrimaItem m = new MateriaPrimaItem();
        m.setId(n.path("id").asInt(0));
        m.setFecha(n.path("fecha").asText(""));
        String nombre = n.has("nombre") ? n.path("nombre").asText("") : n.path("materiaPrimaNombre").asText("");
        m.setNombre(nombre);
        m.setMateriaPrimaId(n.path("materiaPrimaId").asInt(0));
        m.setMateriaPrimaNombre(n.path("materiaPrimaNombre").asText(nombre));
        m.setLoteInterno(n.path("loteInterno").asText(""));
        m.setAlmacenId(n.path("almacenId").asInt(0));
        m.setAlmacenNombre(n.path("almacenNombre").asText(""));
        m.setCantidad(n.path("cantidad").asDouble(0));
        m.setUnidad(n.path("unidad").asText(""));
        m.setMotivo(n.path("motivo").asText(""));
        m.setUsuarioCreacion(n.path("usuarioCreacion").asText(""));
        // codigo interno (maestro)
        String cod = n.has("codigoInterno") ? n.path("codigoInterno").asText("")
                    : n.has("CodigoInterno") ? n.path("CodigoInterno").asText("") : "";
        m.setCodigoInterno(cod);
        // maestro: categoria y origen
        m.setCategoriaMateriaPrimaId(n.path("categoriaMateriaPrimaId").asInt(0));
        m.setCategoriaMateriaPrimaNombre(n.path("categoriaMateriaPrimaNombre").asText(""));
        m.setMateriaPrimaOrigenId(n.path("materiaPrimaOrigenId").asInt(0));
        m.setMateriaPrimaOrigenNombre(n.path("materiaPrimaOrigenNombre").asText(""));
        // stocks/costo
        m.setStockMinimo(n.path("stockMinimo").asDouble(0));
        m.setStockMaximo(n.path("stockMaximo").asDouble(0));
        m.setStockActual(n.path("stockActual").asDouble(0));
        m.setCostoUnitario(n.path("costoUnitario").asDouble(0));
        m.setAprobada(n.has("aprobada") ? n.path("aprobada").asBoolean(false) : n.path("Aprobada").asBoolean(false));
        boolean activa = n.has("activa") ? n.path("activa").asBoolean(false)
                        : n.has("Activo") ? n.path("Activo").asBoolean(false)
                        : n.has("activo") ? n.path("activo").asBoolean(false)
                        : n.has("Activa") ? n.path("Activa").asBoolean(false) : false;
        m.setActiva(activa);
        return m;
    }
}
