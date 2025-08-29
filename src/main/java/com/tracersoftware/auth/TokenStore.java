package com.tracersoftware.auth;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class TokenStore {
    private static final String DIR = System.getProperty("user.home") + File.separator + ".tracersoftware";
    private static final String FILE = DIR + File.separator + "keys.properties";
    private static final String KEY_ACCESS = "api.accessToken";
    private static final String KEY_REFRESH = "api.refreshToken";

    public static void saveTokens(String accessToken, String refreshToken) throws IOException {
        Files.createDirectories(Paths.get(DIR));
        Properties p = load();
        if (accessToken != null) p.setProperty(KEY_ACCESS, accessToken.trim());
        if (refreshToken != null) p.setProperty(KEY_REFRESH, refreshToken.trim());
        try (var out = new FileOutputStream(FILE)) { p.store(out, "TracerSoftware keys"); }
    }

    public static String loadAccessToken() {
        Properties p = load();
    String t = p.getProperty(KEY_ACCESS);
    if (t == null) return null;
    String trimmed = t.trim();
    // Defensive: avoid returning placeholders or refresh token by mistake
    if (trimmed.isEmpty() || "refreshToken".equalsIgnoreCase(trimmed)) return null;
    return isLikelyJwt(trimmed) ? trimmed : null;
    }

    public static String loadRefreshToken() {
        Properties p = load();
        String t = p.getProperty(KEY_REFRESH);
        return (t != null && !t.trim().isEmpty() && !"refreshToken".equalsIgnoreCase(t.trim())) ? t : null;
    }

    public static void clearAccessToken() throws IOException {
        Properties p = load();
        p.remove(KEY_ACCESS);
        persist(p);
    }

    public static void clearAll() throws IOException {
        Files.deleteIfExists(Paths.get(FILE));
    }

    private static Properties load() {
        Properties p = new Properties();
        File f = new File(FILE);
        if (f.exists()) {
            try (var in = new FileInputStream(f)) { p.load(in); } catch (IOException ignored) {}
        }
        return p;
    }

    private static void persist(Properties p) throws IOException {
        Files.createDirectories(Paths.get(DIR));
        try (var out = new FileOutputStream(FILE)) { p.store(out, "TracerSoftware keys"); }
    }

    private static boolean isLikelyJwt(String t) {
        if (t == null) return false;
        String s = t.trim();
        if (s.isEmpty() || "refreshToken".equalsIgnoreCase(s)) return false;
        return s.split("\\.").length == 3 && s.length() > 40;
    }
}
