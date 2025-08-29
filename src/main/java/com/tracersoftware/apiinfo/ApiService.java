
package com.tracersoftware.apiinfo;

import com.tracersoftware.common.SessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApiService {
    private String urlBase;
    private String authToken; // Token JWT o similar
    private String lastLoginResponseJson;
    private final ObjectMapper mapper = new ObjectMapper();

    public ApiService(String urlBase) {
        this.urlBase = urlBase;
    }

    public boolean pingApi() {
        try {
            URL url = new URL(urlBase + "/api/health/ping");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            // Si hay token, agregarlo
            String token = SessionManager.getAuthToken();
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
            conn.connect();
            int code = conn.getResponseCode();
            return code == 200;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Realiza login contra la API. Si es exitoso, guarda el token para futuras llamadas.
     * @param username nombre de usuario
     * @param password contraseña
     * @return true si login exitoso, false si no
     */
    public boolean login(String username, String password) {
        try {
            URL url = new URL(urlBase + "/api/auth/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            String json = String.format("{\"Nombre\":\"%s\",\"Password\":\"%s\"}", username, password);
            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            int code = conn.getResponseCode();
            // Log the login request payload for debugging
            try {
                java.nio.file.Path dbgReq = java.nio.file.Paths.get("debug_auth.log");
                String reqLine = java.time.ZonedDateTime.now().toString() + " | LOGIN POST " + url.toString() + " | Payload: " + json + System.lineSeparator();
                java.nio.file.Files.write(dbgReq, reqLine.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception ignored) {}

            if (code == 200) {
                // Leer el token del body (se espera un JSON con el token)
                try (java.io.InputStream is = conn.getInputStream()) {
                    String response = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    this.lastLoginResponseJson = response;
                    try {
                        JsonNode root = mapper.readTree(response);
                        String token = null;
                        if (root.has("accessToken")) token = root.path("accessToken").asText(null);
                        if (token == null && root.has("token")) token = root.path("token").asText(null);
                        if (token == null && root.has("access_token")) token = root.path("access_token").asText(null);
                        if (token != null && !token.isBlank()) {
                            this.authToken = token.trim().replaceFirst("(?i)^Bearer\\s+", "");
                            SessionManager.setAuthToken(this.authToken);
                            return true;
                        }
                    } catch (Exception ex) {
                        // fallback to simple extractor
                        String token = extractTokenFromJson(response);
                        if (token != null) {
                            this.authToken = token;
                            SessionManager.setAuthToken(token);
                            return true;
                        }
                    }
                }
            }
            // If we reach here login failed (non-200). Capture error body if present for debugging.
            try {
                java.io.InputStream es = conn.getErrorStream();
                String errBody = "";
                if (es != null) {
                    errBody = new String(es.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                }
                java.nio.file.Path dbg = java.nio.file.Paths.get("debug_auth.log");
                String line = java.time.ZonedDateTime.now().toString() + " | LOGIN RESPONSE " + url.toString() + " | Code: " + code + " | Body: " + (errBody == null || errBody.isBlank() ? "(empty)" : errBody) + System.lineSeparator();
                java.nio.file.Files.write(dbg, line.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception ignored) {}
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    public String getLastLoginResponseJson() { return lastLoginResponseJson; }

    private String extractTokenFromJson(String json) {
        // Busca accessToken primero, luego token
        String[] keys = {"accessToken", "token"};
        for (String key : keys) {
            int idx = json.indexOf('"' + key + '"');
            if (idx == -1) continue;
            int start = json.indexOf(':', idx);
            int quote1 = json.indexOf('"', start);
            int quote2 = json.indexOf('"', quote1 + 1);
            if (quote1 != -1 && quote2 != -1) {
                return json.substring(quote1 + 1, quote2);
            }
        }
        return null;
    }

    public String getAuthToken() {
        return authToken;
    }
    
    /**
     * Solicita al backend la creación de una API key para el usuario autenticado.
     * Si el backend no soporta esta operación, devuelve null.
     */
    public String createApiKey() {
        try {
            URL url = new URL(urlBase + "/api/auth/apikey");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(5000);
            conn.setDoOutput(true);
            String token = SessionManager.getAuthToken();
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                try (java.io.InputStream is = conn.getInputStream()) {
                    String response = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    String key = extractTokenFromJson(response);
                    return key;
                }
            }
        } catch (IOException e) {
            // ignore and return null
        }
        return null;
    }
    // Métodos futuros: para cada request protegida, usar el token de SessionManager
}
