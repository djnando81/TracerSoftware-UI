package com.tracersoftware.common;

import com.tracersoftware.almacenes.model.AlmacenDTO;
import com.tracersoftware.usuarios.model.UsuarioDTO;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public class ApiClient {
    public static List<AlmacenDTO> getAlmacenes() {
        List<AlmacenDTO> result = new ArrayList<>();
        try {
            String urlBase = ConfigManager.getUrlBase();
            URL url = new URL(urlBase + "/api/almacenes");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                result = mapper.readValue(conn.getInputStream(), new TypeReference<List<AlmacenDTO>>(){});
            }
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static List<UsuarioDTO> getUsuarios() {
        List<UsuarioDTO> result = new ArrayList<>();
        try {
            String urlBase = ConfigManager.getUrlBase();
            URL url = new URL(urlBase + "/api/usuarios");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                result = mapper.readValue(conn.getInputStream(), new TypeReference<List<UsuarioDTO>>(){});
            }
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
