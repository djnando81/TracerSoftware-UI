package com.tracersoftware.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "config.ini";
    private static Properties properties = new Properties();
    private static boolean loaded = false;

    public static void loadConfig() throws IOException {
        if (!loaded) {
            try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
                properties.load(fis);
                loaded = true;
            }
        }
    }

    public static String get(String key) {
        try {
            if (!loaded) {
                loadConfig();
            }
        } catch (IOException e) {
            // If config can't be loaded, print and return null so callers can handle gracefully
            e.printStackTrace();
            return null;
        }
        return properties.getProperty(key);
    }

    public static String getUrlBase() {
        String url = get("api.urlBase");
        if (url == null) return null;
        // sanitize common accidental escaping (e.g. "http\://localhost\:5000")
        return url.replace("\\", "");
    }

    /**
     * Set a property in memory and persist to disk (config.ini).
     * Returns true if the file was written successfully.
     */
    public static boolean setAndSave(String key, String value) {
        // Ensure we have the current file contents loaded so we don't overwrite other keys (like api.urlBase)
        try {
            if (!loaded) {
                loadConfig();
            }
        } catch (IOException e) {
            // If we can't load, continue — we'll still write the provided key, but preserve what we have in memory
            e.printStackTrace();
        }

        properties.setProperty(key, value);
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "TracerSoftware config");
            loaded = true;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
