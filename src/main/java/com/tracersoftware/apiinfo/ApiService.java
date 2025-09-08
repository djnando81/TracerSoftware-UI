
package com.tracersoftware.apiinfo;

import com.tracersoftware.common.SessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class ApiService {
    private String urlBase;
    private String authToken; // Token JWT o similar
    private String lastLoginResponseJson;
    private final ObjectMapper mapper = new ObjectMapper();
    private String lastPingError;
    private String lastPingUrl;

    public ApiService(String urlBase) {
        this.urlBase = urlBase;
    }

    public boolean pingApi() {
        try {
            String target = normalize(urlBase) + "/api/apiinfo/health";
            this.lastPingUrl = target;
            URL url = new URL(target);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(3000);
            // Si hay token, agregarlo
            String token = SessionManager.getAuthToken();
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
            conn.connect();
            int code = conn.getResponseCode();
            if (code == 200) {
                this.lastPingError = null;
                return true;
            } else {
                String body = null;
                try {
                    java.io.InputStream es = conn.getErrorStream();
                    body = es != null ? new String(es.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8) : "";
                } catch (Exception ignored) {}
                this.lastPingError = "HTTP " + code + (body != null && !body.isBlank() ? (": " + body) : "");
                return false;
            }
        } catch (IOException e) {
            this.lastPingError = e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "(sin mensaje)" : e.getMessage());
            return false;
        }
    }

    public boolean pingApiAgainst(String base) {
        String prev = this.urlBase;
        try {
            this.urlBase = base;
            return pingApi();
        } finally {
            this.urlBase = prev;
        }
    }

    public void setUrlBase(String base) { this.urlBase = base; }
    public String getUrlBase() { return this.urlBase; }
    public String getLastPingError() { return lastPingError; }
    public String getLastPingUrl() { return lastPingUrl; }

    /**
     * Realiza login contra la API. Si es exitoso, guarda el token para futuras llamadas.
     * @param username nombre de usuario
     * @param password contraseña
     * @return true si login exitoso, false si no
     */
    public boolean login(String username, String password) {
        try {
            // Usar estrictamente la base del config (inyectada al construir ApiService)
            String base = normalize(this.urlBase);
            return doLoginAgainst(base, username, password);
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean doLoginAgainst(String base, String username, String password) throws IOException {
        // endpoint de login configurable (default: /api/Auth/login)
        String configured = null;
        try { configured = com.tracersoftware.common.ConfigManager.get("api.loginEndpoint"); } catch (Exception ignored) {}
        String[] endpoints = (configured != null && !configured.isBlank())
                ? new String[] { configured.trim() }
                : new String[] { "/api/Auth/login" };
        IOException lastEx = null;
        for (String ep : endpoints) {
            URL url = new URL(base + ep);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // If HTTPS, install permissive trust manager for dev environments
            if (conn instanceof javax.net.ssl.HttpsURLConnection https) {
                try {
                    javax.net.ssl.TrustManager[] trustAll = new javax.net.ssl.TrustManager[]{ new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers(){return new java.security.cert.X509Certificate[0];}
                    public void checkClientTrusted(java.security.cert.X509Certificate[] c,String a){}
                    public void checkServerTrusted(java.security.cert.X509Certificate[] c,String a){}
                }};
                javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS");
                sc.init(null, trustAll, new java.security.SecureRandom());
                https.setSSLSocketFactory(sc.getSocketFactory());
                https.setHostnameVerifier((h, s) -> true);
            } catch (Exception ignored) {}
        }
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "*/*");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);
            // Console: show request and headers similar a ApiClient
            try {
                System.out.println("[Login] Request: POST " + url.toString());
                System.out.println("[Login] Headers:");
                System.out.println("  content-type: application/json");
                System.out.println("  accept: */*");
            } catch (Exception ignored) {}
            // Payload EXACTO como en tu cURL: claves en minúscula
            com.fasterxml.jackson.databind.node.ObjectNode obj = mapper.createObjectNode();
            obj.put("nombre", username);
            obj.put("password", password);
            String json = obj.toString();
            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            int code = conn.getResponseCode();
            // Debug log
            try {
                java.nio.file.Path dbgReq = java.nio.file.Paths.get("debug_auth.log");
                String reqLine = java.time.ZonedDateTime.now().toString() + " | LOGIN POST " + url.toString() + " | Payload: " + json + " | Code: " + code + System.lineSeparator();
                java.nio.file.Files.write(dbgReq, reqLine.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception ignored) {}
            // Console: response status + headers
            try {
                System.out.println("[Login] Response: " + code);
                System.out.println("[Login] Resp Headers:");
                for (Map.Entry<String, List<String>> e : conn.getHeaderFields().entrySet()) {
                    String k = e.getKey();
                    if (k == null) k = "";
                    String v = String.join(",", e.getValue());
                    System.out.println("  " + k.toLowerCase() + ": " + v);
                }
            } catch (Exception ignored) {}
            if (code == 200 || code == 201) {
                try (java.io.InputStream is = conn.getInputStream()) {
                    String response = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    this.lastLoginResponseJson = response;
                    try {
                        JsonNode root = mapper.readTree(response);
                        String token = null;
                        if (root.has("accessToken")) token = root.path("accessToken").asText(null);
                        if (token == null && root.has("token")) token = root.path("token").asText(null);
                        if (token == null && root.has("access_token")) token = root.path("access_token").asText(null);
                        // Also accept nested: { data: { accessToken: ... } }
                        if (token == null && root.has("data") && root.path("data").isObject()) {
                            JsonNode data = root.path("data");
                            if (data.has("accessToken")) token = data.path("accessToken").asText(null);
                            if (token == null && data.has("token")) token = data.path("token").asText(null);
                        }
                        if (token != null && !token.isBlank()) {
                            this.authToken = token.trim().replaceFirst("(?i)^Bearer\\s+", "");
                            SessionManager.setAuthToken(this.authToken);
                            try { com.tracersoftware.auth.SessionManager.get().setSession(this.authToken, null, username, new String[0], null); } catch (Exception ignored) {}
                            return true;
                        }
                    } catch (Exception ex) {
                        String token = extractTokenFromJson(response);
                        if (token != null) {
                            this.authToken = token;
                            SessionManager.setAuthToken(token);
                            try { com.tracersoftware.auth.SessionManager.get().setSession(token, null, username, new String[0], null); } catch (Exception ignored) {}
                            return true;
                        }
                    }
                }
            } else {
                try {
                    java.io.InputStream es = conn.getErrorStream();
                    String errBody = es != null ? new String(es.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8) : "";
                    this.lastLoginResponseJson = errBody;
                } catch (Exception ignored) {}
            }
            // if we reached here and not returned true, fallthrough to next endpoint
        }
        return false;
    }

    // Fallbacks eliminados: se usa la URL de config.ini

    private String normalize(String base) {
        if (base == null) return "";
        String b = base.trim();
        if (b.endsWith("/")) b = b.substring(0, b.length()-1);
        return b;
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
