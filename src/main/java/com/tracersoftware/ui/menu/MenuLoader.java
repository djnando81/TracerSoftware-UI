package com.tracersoftware.ui.menu;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class MenuLoader {

    private final ObjectMapper objectMapper;

    public MenuLoader() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Loads a menu manifest from the classpath resource path (e.g. /menu-manifest.json)
     */
    public MenuModel.MenuManifest loadFromResource(String resourcePath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            MenuModel.MenuManifest manifest = objectMapper.readValue(is, MenuModel.MenuManifest.class);
            if (manifest == null) {
                throw new IOException("Parsed manifest is null for: " + resourcePath);
            }
            return manifest;
        }
    }
}
