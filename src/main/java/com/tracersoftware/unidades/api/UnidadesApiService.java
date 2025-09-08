package com.tracersoftware.unidades.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tracersoftware.api.ApiClient;
import com.tracersoftware.unidades.model.UnidadMedidaItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UnidadesApiService {
    private final ApiClient api = new ApiClient();

    public List<UnidadMedidaItem> listAll() throws IOException, InterruptedException, com.tracersoftware.api.UnauthorizedException {
        JsonNode root = api.getJson("/api/UnidadesMedida");
        List<UnidadMedidaItem> out = new ArrayList<>();
        if (root != null && root.isArray()) {
            for (JsonNode n : root) out.add(toItem(n));
        }
        return out;
    }

    private UnidadMedidaItem toItem(JsonNode n) {
        UnidadMedidaItem u = new UnidadMedidaItem();
        u.setId(n.path("id").asInt(0));
        u.setNombre(n.path("nombre").asText(""));
        u.setAbreviatura(n.path("abreviatura").asText(""));
        u.setActiva(n.path("activa").asBoolean(false));
        u.setTipo(n.path("tipo").asText(""));
        u.setEsBasica(n.path("esBasica").asBoolean(false));
        return u;
    }
}

