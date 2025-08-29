package com.tracersoftware.common;

public class SessionManager {
    private static String authToken;
    private static String username;
    private static String avatarUrl;

    public static void setAuthToken(String token) {
        authToken = token;
    }

    public static String getAuthToken() {
        return authToken;
    }

    public static void setUsername(String user) {
        username = user;
    }

    public static String getUsername() {
        return username;
    }

    public static void setAvatarUrl(String url) {
        avatarUrl = url;
    }

    public static String getAvatarUrl() {
        return avatarUrl;
    }

    public static void clear() {
        authToken = null;
        username = null;
    }
}
