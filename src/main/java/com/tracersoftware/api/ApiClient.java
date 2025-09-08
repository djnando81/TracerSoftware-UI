package com.tracersoftware.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.tracersoftware.auth.SessionManager;
import java.nio.file.StandardOpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import com.tracersoftware.auth.TokenStore;
import com.tracersoftware.common.controls.MessageToast;
import com.tracersoftware.common.ConfigManager;
import javafx.stage.Stage;

public class ApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String baseUrl;

    public ApiClient() {
        // never follow redirects so Authorization headers aren't lost
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.objectMapper = new ObjectMapper();
        // Prefer explicit value from config.ini (api.urlBase), then env/prop, then local fallback
        try {
            String fromConfig = ConfigManager.getUrlBase();
            if (fromConfig != null && !fromConfig.isBlank()) {
                this.baseUrl = fromConfig.trim();
            }
        } catch (Exception ignored) {}
        if (this.baseUrl == null || this.baseUrl.isBlank()) {
            String env = System.getProperty("api.baseUrl", System.getenv("API_BASE_URL"));
            if (env != null && !env.isBlank()) this.baseUrl = env.trim();
        }
        if (this.baseUrl == null || this.baseUrl.isBlank()) {
            this.baseUrl = readBaseUrlFromConfig();
        }
        if (this.baseUrl == null || this.baseUrl.isBlank()) {
            this.baseUrl = "https://localhost:5001";
        }
    }

    private String readBaseUrlFromConfig() {
        try {
            File cfg = new File("config.ini");
            if (cfg.exists()) {
                List<String> lines = Files.readAllLines(cfg.toPath());
                Map<String, String> map = lines.stream()
                        .map(String::trim)
                        .filter(l -> l.contains("="))
                        .map(l -> l.split("=", 2))
                        .collect(Collectors.toMap(a -> a[0].trim(), a -> a[1].trim(), (a, b) -> b));
                String v = map.get("api.urlBase");
                if (v != null && !v.isEmpty()) return v;
            }
        } catch (IOException ignored) {
        }
        return "http://localhost:5000";
    }

    // auth tokens are no longer read directly from config.ini here. KeyManager or SessionManager
    // provide tokens at request time.

    public JsonNode getJson(String path) throws IOException, InterruptedException, UnauthorizedException {
        String target = path.startsWith("http") ? path : normalizeUrl(baseUrl, path);
    HttpRequest.Builder rb = HttpRequest.newBuilder()
        .uri(URI.create(target))
        .timeout(Duration.ofSeconds(8))
        .header("Accept", "application/json")
        .GET();
    // Prefer sanitized token from auth.SessionManager, fallback to persisted TokenStore access token
    String token = null;
    try { token = SessionManager.get().getBearerTokenSanitized().orElse(null); } catch (Exception ignore) {}
    if (token == null || token.isEmpty()) {
        try { String k = TokenStore.loadAccessToken(); if (k != null && !k.isBlank()) token = k.trim(); } catch (Exception ignore) {}
    }
    // Defensive: if token looks invalid (literal 'refreshToken' or not JWT-like), attempt migration/cleanup
    try { token = sanitizeAndMigrateToken(token); } catch (Exception ignore) {}
    // Final defensive check: ensure token is proper JWT-like string before using
    if (token != null) token = token.trim();
    if (token != null && (token.equalsIgnoreCase("refreshToken") || token.split("\\.").length != 3)) {
        // try again to read from store cleanly
        try { String k = TokenStore.loadAccessToken(); if (k != null && !k.isBlank()) token = k.trim(); else token = null; } catch (Exception ignore) { token = null; }
    }
    // Fallback: allow a dev token from KeyManager for seamless navigation without relogin
    if (token == null || token.isEmpty()) {
        try { String dev = com.tracersoftware.common.KeyManager.getToken(); if (dev != null && !dev.isBlank()) token = dev.trim(); } catch (Exception ignore) {}
    }
    // Also write exact Authorization header + target to debug file for off-screen inspection
    try {
    String authHeader = (token != null && !token.isEmpty()) ? "Bearer " + token : "(none)";
        Path dbg = Paths.get("debug_auth.log");
        String line = java.time.ZonedDateTime.now().toString() + " | GET " + target + " | Authorization: " + authHeader + System.lineSeparator();
        java.nio.file.Files.write(dbg, line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (Exception ignored) {}
    if (token != null && !token.isEmpty()) rb.header("Authorization", "Bearer " + token);
    HttpRequest req = rb.build();
        logRequest(req);
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        logResponse(resp);
        if (isRedirect(resp.statusCode())) {
            throw new IOException("Unexpected redirect (" + resp.statusCode() + ") from " + target + ". Use correct baseUrl (HTTPS).");
        }
    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            return objectMapper.readTree(resp.body());
        } else {
            // Log response body for debugging
            try {
                Path dbg = Paths.get("debug_auth.log");
                String line = java.time.ZonedDateTime.now().toString() + " | RESPONSE " + resp.statusCode() + " " + target + " | Body: " + (resp.body() == null ? "(empty)" : resp.body()) + System.lineSeparator();
                java.nio.file.Files.write(dbg, line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ignored) {}
            if (resp.statusCode() == 401) {
                String www = resp.headers().firstValue("WWW-Authenticate").orElse("");
                try { com.tracersoftware.auth.TokenStore.clearAccessToken(); } catch (Exception ignored) {}
                throw new UnauthorizedException("HTTP 401 from " + target + (www.isBlank() ? (":" + resp.body()) : " | " + www));
            }
            throw new IOException("HTTP " + resp.statusCode() + " from " + target + ": " + resp.body());
        }
    }

    public JsonNode postJson(String path, String jsonPayload) throws IOException, InterruptedException, UnauthorizedException {
        String target = path.startsWith("http") ? path : normalizeUrl(baseUrl, path);
    HttpRequest.Builder rb = HttpRequest.newBuilder()
        .uri(URI.create(target))
        .timeout(Duration.ofSeconds(10))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));
    String token2 = null;
    try { token2 = SessionManager.get().getBearerTokenSanitized().orElse(null); } catch (Exception ignore) {}
    if (token2 == null || token2.isEmpty()) {
        try {
            String k2 = TokenStore.loadAccessToken();
            if (k2 != null && !k2.isBlank()) token2 = k2.trim();
        } catch (Exception ignore) {}
    }
    if (token2 != null) token2 = token2.trim();
    if (token2 != null && (token2.equalsIgnoreCase("refreshToken") || token2.split("\\.").length != 3)) {
        try { String k2 = TokenStore.loadAccessToken(); if (k2 != null && !k2.isBlank()) token2 = k2.trim(); else token2 = null; } catch (Exception ignore) { token2 = null; }
    }
    // Fallback dev token to avoid relogin requirements in navigation
    if (token2 == null || token2.isEmpty()) {
        try { String dev = com.tracersoftware.common.KeyManager.getToken(); if (dev != null && !dev.isBlank()) token2 = dev.trim(); } catch (Exception ignore) {}
    }
    try {
        String authHeader2 = (token2 != null && !token2.isEmpty()) ? "Bearer " + token2 : "(none)";
        Path dbg2 = Paths.get("debug_auth.log");
        String line2 = java.time.ZonedDateTime.now().toString() + " | POST " + target + " | Authorization: " + authHeader2 + System.lineSeparator();
        java.nio.file.Files.write(dbg2, line2.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (Exception ignored) {}
    if (token2 != null && !token2.isEmpty()) rb.header("Authorization", "Bearer " + token2);
    HttpRequest req = rb.build();
        logRequest(req);
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        logResponse(resp);
        if (isRedirect(resp.statusCode())) {
            throw new IOException("Unexpected redirect (" + resp.statusCode() + ") from " + target + ". Use correct baseUrl (HTTPS).");
        }
    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            if (resp.body() == null || resp.body().isEmpty()) return objectMapper.createObjectNode();
            return objectMapper.readTree(resp.body());
        } else {
            try {
                Path dbg = Paths.get("debug_auth.log");
                String line = java.time.ZonedDateTime.now().toString() + " | RESPONSE " + resp.statusCode() + " " + target + " | Body: " + (resp.body() == null ? "(empty)" : resp.body()) + System.lineSeparator();
                java.nio.file.Files.write(dbg, line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ignored) {}
            if (resp.statusCode() == 401) {
                String www = resp.headers().firstValue("WWW-Authenticate").orElse("");
                throw new UnauthorizedException("HTTP 401 from " + target + (www.isBlank() ? (":" + resp.body()) : " | " + www));
            }
            throw new IOException("HTTP " + resp.statusCode() + " from " + target + ": " + resp.body());
        }
    }

    // Binary POST: returns response bytes (for file downloads)
    public byte[] postBinary(String path, String jsonPayload) throws IOException, InterruptedException, UnauthorizedException {
        String target = path.startsWith("http") ? path : normalizeUrl(baseUrl, path);
        HttpRequest.Builder rb = HttpRequest.newBuilder()
                .uri(URI.create(target))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload == null ? "[]" : jsonPayload));
        String token2 = null;
        try { token2 = SessionManager.get().getBearerTokenSanitized().orElse(null); } catch (Exception ignore) {}
        if (token2 == null || token2.isEmpty()) {
            try { String k2 = TokenStore.loadAccessToken(); if (k2 != null && !k2.isBlank()) token2 = k2.trim(); } catch (Exception ignore) {}
        }
        if (token2 != null) token2 = token2.trim();
        if (token2 != null && (token2.equalsIgnoreCase("refreshToken") || token2.split("\\.").length != 3)) {
            try { String k2 = TokenStore.loadAccessToken(); if (k2 != null && !k2.isBlank()) token2 = k2.trim(); else token2 = null; } catch (Exception ignore) { token2 = null; }
        }
        try {
            String authHeader2 = (token2 != null && !token2.isEmpty()) ? "Bearer " + token2 : "(none)";
            Path dbg2 = Paths.get("debug_auth.log");
            String line2 = java.time.ZonedDateTime.now().toString() + " | POSTBIN " + target + " | Authorization: " + authHeader2 + System.lineSeparator();
            java.nio.file.Files.write(dbg2, line2.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
        if (token2 != null && !token2.isEmpty()) rb.header("Authorization", "Bearer " + token2);
        HttpRequest req = rb.build();
        logRequest(req);
        HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
        logResponse(resp);
        if (isRedirect(resp.statusCode())) {
            throw new IOException("Unexpected redirect (" + resp.statusCode() + ") from " + target + ". Use correct baseUrl (HTTPS).");
        }
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            return resp.body() == null ? new byte[0] : resp.body();
        } else {
            try {
                Path dbg = Paths.get("debug_auth.log");
                String line = java.time.ZonedDateTime.now().toString() + " | RESPONSE " + resp.statusCode() + " " + target + " | BodyBytes: " + (resp.body() == null ? 0 : resp.body().length) + System.lineSeparator();
                java.nio.file.Files.write(dbg, line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ignored) {}
            if (resp.statusCode() == 401) {
                String www = resp.headers().firstValue("WWW-Authenticate").orElse("");
                throw new UnauthorizedException("HTTP 401 from " + target + (www.isBlank() ? "" : (" | " + www)));
            }
            throw new IOException("HTTP " + resp.statusCode() + " from " + target);
        }
    }

        public JsonNode putJson(String path, String jsonPayload) throws IOException, InterruptedException, UnauthorizedException {
            String target = path.startsWith("http") ? path : normalizeUrl(baseUrl, path);
            HttpRequest.Builder rb = HttpRequest.newBuilder()
                    .uri(URI.create(target))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload));
            String token2 = null;
            try { token2 = SessionManager.get().getBearerTokenSanitized().orElse(null); } catch (Exception ignore) {}
            if (token2 == null || token2.isEmpty()) {
                try {
                    String k2 = TokenStore.loadAccessToken();
                    if (k2 != null && !k2.isBlank()) token2 = k2.trim();
                } catch (Exception ignore) {}
            }
            if (token2 != null) token2 = token2.trim();
            if (token2 != null && (token2.equalsIgnoreCase("refreshToken") || token2.split("\\.").length != 3)) {
                try { String k2 = TokenStore.loadAccessToken(); if (k2 != null && !k2.isBlank()) token2 = k2.trim(); else token2 = null; } catch (Exception ignore) { token2 = null; }
            }
            // no UI toasts for token in POST/PUT
            try {
                String authHeader2 = (token2 != null && !token2.isEmpty()) ? "Bearer " + token2 : "(none)";
                Path dbg2 = Paths.get("debug_auth.log");
                String line2 = java.time.ZonedDateTime.now().toString() + " | PUT " + target + " | Authorization: " + authHeader2 + System.lineSeparator();
                java.nio.file.Files.write(dbg2, line2.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ignored) {}
            if (token2 != null && !token2.isEmpty()) rb.header("Authorization", "Bearer " + token2);
            HttpRequest req = rb.build();
            logRequest(req);
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            logResponse(resp);
            if (isRedirect(resp.statusCode())) {
                throw new IOException("Unexpected redirect (" + resp.statusCode() + ") from " + target + ". Use correct baseUrl (HTTPS).");
            }
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                if (resp.body() == null || resp.body().isEmpty()) return objectMapper.createObjectNode();
                return objectMapper.readTree(resp.body());
            } else {
                try {
                    Path dbg = Paths.get("debug_auth.log");
                    String line = java.time.ZonedDateTime.now().toString() + " | RESPONSE " + resp.statusCode() + " " + target + " | Body: " + (resp.body() == null ? "(empty)" : resp.body()) + System.lineSeparator();
                    java.nio.file.Files.write(dbg, line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (Exception ignored) {}
                if (resp.statusCode() == 401) {
                    String www = resp.headers().firstValue("WWW-Authenticate").orElse("");
                    throw new UnauthorizedException("HTTP 401 from " + target + (www.isBlank() ? (":" + resp.body()) : " | " + www));
                }
                throw new IOException("HTTP " + resp.statusCode() + " from " + target + ": " + resp.body());
            }
        }

    public JsonNode deleteJson(String path) throws IOException, InterruptedException, UnauthorizedException {
        String target = path.startsWith("http") ? path : normalizeUrl(baseUrl, path);
        HttpRequest.Builder rb = HttpRequest.newBuilder()
                .uri(URI.create(target))
                .timeout(Duration.ofSeconds(8))
                .DELETE();
        String token = null;
        try { token = SessionManager.get().getBearerTokenSanitized().orElse(null); } catch (Exception ignore) {}
        if (token == null || token.isEmpty()) {
            try { String k = com.tracersoftware.auth.TokenStore.loadAccessToken(); if (k != null && !k.isBlank()) token = k.trim(); } catch (Exception ignore) {}
        }
        if (token == null || token.isEmpty()) {
            try { String dev = com.tracersoftware.common.KeyManager.getToken(); if (dev != null && !dev.isBlank()) token = dev.trim(); } catch (Exception ignore) {}
        }
        try {
            String authHeader = (token != null && !token.isEmpty()) ? "Bearer " + token : "(none)";
            Path dbg = Paths.get("debug_auth.log");
            String line = java.time.ZonedDateTime.now().toString() + " | DELETE " + target + " | Authorization: " + authHeader + System.lineSeparator();
            java.nio.file.Files.write(dbg, line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
        if (token != null && !token.isEmpty()) rb.header("Authorization", "Bearer " + token);
        HttpRequest req = rb.build();
        logRequest(req);
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        logResponse(resp);
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            if (resp.body() == null || resp.body().isEmpty()) return objectMapper.createObjectNode();
            return objectMapper.readTree(resp.body());
        } else {
            try {
                Path dbg = Paths.get("debug_auth.log");
                String line = java.time.ZonedDateTime.now().toString() + " | RESPONSE " + resp.statusCode() + " " + target + " | Body: " + (resp.body() == null ? "(empty)" : resp.body()) + System.lineSeparator();
                java.nio.file.Files.write(dbg, line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ignored) {}
            if (resp.statusCode() == 401) {
                String www = resp.headers().firstValue("WWW-Authenticate").orElse("");
                throw new UnauthorizedException("HTTP 401 from " + target + (www.isBlank() ? (":" + resp.body()) : " | " + www));
            }
            throw new IOException("HTTP " + resp.statusCode() + " from " + target + ": " + resp.body());
        }
    }

    private static String normalizeUrl(String base, String path) {
        String b = base.endsWith("/") ? base.substring(0, base.length()-1) : base;
        String p = path.startsWith("/") ? path : "/" + path;
        return b + p;
    }

    private static boolean isRedirect(int code) { return code == 301 || code == 302 || code == 307 || code == 308; }

    private static void logRequest(HttpRequest req) {
        try {
            System.out.println("[ApiClient] Request: " + req.method() + " " + req.uri());
            System.out.println("[ApiClient] Headers:");
            req.headers().map().forEach((k,v)-> System.out.println("  " + k + ": " + String.join(",", v)));
        } catch (Exception ignored) {}
    }

    private static void logResponse(HttpResponse<?> resp) {
        try {
            System.out.println("[ApiClient] Response: " + resp.statusCode());
            System.out.println("[ApiClient] Resp Headers:");
            resp.headers().map().forEach((k,v)-> System.out.println("  " + k + ": " + String.join(",", v)));
        } catch (Exception ignored) {}
    }

    /**
     * If the provided token is clearly invalid (e.g. literal "refreshToken"), try to
     * load a valid access token from TokenStore or clear legacy key.ini if needed.
     */
    private String sanitizeAndMigrateToken(String token) {
        try {
            if (token == null) token = "";
            String t = token.trim();
            if (t.isEmpty() || t.equalsIgnoreCase("refreshToken") || t.split("\\.").length != 3) {
                // Try TokenStore first
                try {
                    String fromStore = com.tracersoftware.auth.TokenStore.loadAccessToken();
                    if (fromStore != null && !fromStore.isBlank()) return fromStore.trim();
                } catch (Exception ignored) {}
                // If still bad and legacy key.ini exists, delete it (backup already handled by MainApp)
                try {
                    java.nio.file.Path keyFile = java.nio.file.Paths.get(System.getProperty("user.dir"), "key.ini");
                    if (java.nio.file.Files.exists(keyFile)) {
                        java.nio.file.Files.deleteIfExists(keyFile);
                        // log cleanup
                        java.nio.file.Path dbg = java.nio.file.Paths.get("debug_auth.log");
                        String line = java.time.ZonedDateTime.now().toString() + " | Removed legacy key.ini due to invalid token" + System.lineSeparator();
                        java.nio.file.Files.write(dbg, line.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                    }
                } catch (Exception ignored) {}
                return null;
            }
            return t;
        } catch (Exception ex) {
            return token;
        }
    }

    /**
     * Simple static helper to verify whether the currently set token (SessionManager or KeyManager)
     * is accepted by the backend. Returns true when the ping endpoint responds with 2xx.
     */
    public static boolean pingAuth() {
        try {
            ApiClient client = new ApiClient();
            try {
                client.getJson("/api/apiinfo/health");
                return true;
            } catch (Exception ex) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

}
