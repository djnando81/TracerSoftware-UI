package com.tracersoftware.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class KeyManager {
    // Use an explicit path anchored at the current working directory so read/write is predictable
    private static final java.io.File KEY_FILE = new java.io.File(System.getProperty("user.dir"), "key.ini");
    private static Properties props = new Properties();
    private static boolean loaded = false;

    private static void ensureLoaded() {
        if (loaded) return;
        if (KEY_FILE.exists()) {
            try (FileInputStream fis = new FileInputStream(KEY_FILE)) {
                props.load(fis);
            } catch (IOException ignored) { }
        }
        loaded = true;
    }

    public static String getToken() {
        ensureLoaded();
        String t = props.getProperty("api.token");
        return t == null ? "" : t;
    }

    public static boolean saveToken(String token) {
        ensureLoaded();
        props.setProperty("api.token", token == null ? "" : token);
        try (FileOutputStream fos = new FileOutputStream(KEY_FILE)) {
            props.store(fos, "TracerSoftware keys");
            loaded = true;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean clearToken() {
        ensureLoaded();
        props.setProperty("api.token", "");
        try (FileOutputStream fos = new FileOutputStream(KEY_FILE)) {
            props.store(fos, "TracerSoftware keys");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
