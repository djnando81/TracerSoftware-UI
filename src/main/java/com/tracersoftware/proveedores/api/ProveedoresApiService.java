package com.tracersoftware.proveedores.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tracersoftware.api.ApiClient;
import com.tracersoftware.proveedores.model.ProveedorItem;
import com.tracersoftware.proveedores.model.MateriaPrimaItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProveedoresApiService {
    private final ApiClient api = new ApiClient();

    public List<ProveedorItem> listAll() throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        JsonNode root = api.getJson("/api/proveedores");
        List<ProveedorItem> out = new ArrayList<>();
        if (root != null && root.isArray()) {
            for (JsonNode n : root) out.add(toItem(n));
        }
        return out;
    }

    public JsonNode getById(int id) throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        return api.getJson("/api/proveedores/" + id);
    }

    public JsonNode create(JsonNode payload) throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        return api.postJson("/api/proveedores", payload.toString());
    }

    public JsonNode update(int id, JsonNode payload) throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        return api.putJson("/api/proveedores/" + id, payload.toString());
    }

    public void delete(int id) throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        api.deleteJson("/api/proveedores/" + id);
    }

    public JsonNode toggleEstado(int id, boolean activo) throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        com.fasterxml.jackson.databind.node.ObjectNode body = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        body.put("activo", activo);
        return api.putJson("/api/proveedores/" + id + "/toggle-estado", body.toString());
    }

    private ProveedorItem toItem(JsonNode n) {
        ProveedorItem p = new ProveedorItem();
        p.setId(n.path("id").asInt(0));
        p.setNombre(n.path("nombre").asText(""));
        p.setCuit(n.path("cuit").asText(""));
        p.setDireccion(n.path("direccion").asText(""));
        p.setTelefono(n.path("telefono").asText(""));
        p.setEmail(n.path("email").asText(""));
        boolean activo = n.has("activo") ? n.path("activo").asBoolean(false) : n.path("activa").asBoolean(false);
        p.setActivo(activo);
        
        // Mapear materias primas
        List<MateriaPrimaItem> materiasPrimas = new ArrayList<>();
        JsonNode mpArray = n.path("materiasPrimas");
        if (mpArray.isArray()) {
            for (JsonNode mp : mpArray) {
                materiasPrimas.add(toMateriaPrimaItem(mp));
            }
        }
        p.setMateriasPrimas(materiasPrimas);
        
        return p;
    }

    private MateriaPrimaItem toMateriaPrimaItem(JsonNode n) {
        MateriaPrimaItem mp = new MateriaPrimaItem();
        mp.setId(n.path("id").asInt(0));
        mp.setNombre(n.path("nombre").asText(""));
        mp.setCodigoInterno(n.path("codigoInterno").asText(""));
        mp.setUnidad(n.path("unidad").asText(""));
        mp.setCategoriaMateriaPrimaId(n.path("categoriaMateriaPrimaId").asInt(0));
        mp.setCategoriaMateriaPrimaNombre(n.path("categoriaMateriaPrimaNombre").asText(""));
        mp.setActiva(n.path("activa").asBoolean(false));
        mp.setMateriaPrimaOrigenId(n.path("materiaPrimaOrigenId").asInt(0));
        mp.setMateriaPrimaOrigenNombre(n.path("materiaPrimaOrigenNombre").asText(""));
        mp.setStockMinimo(n.path("stockMinimo").asDouble(0));
        mp.setStockMaximo(n.path("stockMaximo").asDouble(0));
        mp.setStockActual(n.path("stockActual").asDouble(0));
        mp.setCostoUnitario(n.path("costoUnitario").asDouble(0));
        
        // Mapear derivadas
        List<MateriaPrimaItem.MateriaPrimaDerivada> derivadas = new ArrayList<>();
        JsonNode derivadasArray = n.path("derivadas");
        if (derivadasArray.isArray()) {
            for (JsonNode d : derivadasArray) {
                MateriaPrimaItem.MateriaPrimaDerivada derivada = new MateriaPrimaItem.MateriaPrimaDerivada();
                derivada.setId(d.path("id").asInt(0));
                derivada.setNombre(d.path("nombre").asText(""));
                derivada.setUnidad(d.path("unidad").asText(""));
                derivada.setPorcentajeObtenido(d.path("porcentajeObtenido").asDouble(0));
                derivadas.add(derivada);
            }
        }
        mp.setDerivadas(derivadas);
        
        return mp;
    }
    
    public List<MateriaPrimaItem> obtenerTodasLasMateriasPrimas() throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        JsonNode root = api.getJson("/api/materiasprimas");
        List<MateriaPrimaItem> out = new ArrayList<>();
        if (root != null && root.isArray()) {
            for (JsonNode n : root) {
                out.add(toMateriaPrimaItem(n));
            }
        }
        return out;
    }
}
