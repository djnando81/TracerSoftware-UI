package com.tracersoftware.auth;

import java.time.Instant;
import java.util.Optional;

public class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();

    private volatile String accessToken;
    private volatile Instant expiresAt;
    private volatile String username;
    private volatile String[] roles;

    private SessionManager() {}

    public static SessionManager get() { return INSTANCE; }

    public synchronized void setSession(String token, Instant expiresAt, String username, String[] roles) {
        this.accessToken = token;
        this.expiresAt = expiresAt;
        this.username = username;
        this.roles = roles;
    }

    // New overload to include refresh token
    public synchronized void setSession(String token, Instant expiresAt, String username, String[] roles, String refreshToken) {
        this.accessToken = token;
        this.expiresAt = expiresAt;
        this.username = username;
        this.roles = roles;
        try { com.tracersoftware.auth.TokenStore.saveTokens(token, refreshToken); } catch (Exception ignored) {}
    }

    public synchronized void clear() {
        this.accessToken = null;
        this.expiresAt = null;
        this.username = null;
        this.roles = null;
        try { com.tracersoftware.auth.TokenStore.clearAll(); } catch (Exception ignored) {}
    }

    public synchronized void loadFromDiskIfAny() {
        String t = com.tracersoftware.auth.TokenStore.loadAccessToken();
        if (t != null) this.accessToken = t;
    }

    public Optional<String> getBearerToken() {
        return Optional.ofNullable(accessToken);
    }

    public Optional<String> getBearerTokenSanitized() {
        if (accessToken == null || accessToken.isBlank()) return Optional.empty();
        String t = accessToken.trim();
        if (t.toLowerCase().startsWith("bearer ")) t = t.substring(7).trim();
        return Optional.of(t);
    }

    public boolean isExpired() {
        if (expiresAt == null) return false;
        return Instant.now().isAfter(expiresAt.minusSeconds(30));
    }

    public String[] getRoles() { return roles; }
    public String getUsername() { return username; }
}
