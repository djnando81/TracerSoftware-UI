package com.tracersoftware.ui.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

public class MenuLoaderTest {

    @Test
    public void load_valid_manifest_shouldReturnManifest() throws IOException {
        MenuLoader loader = new MenuLoader();
        MenuModel.MenuManifest manifest = loader.loadFromResource("/menu-manifest.json");
        assertNotNull(manifest);
        assertEquals("Tracer Dashboard", manifest.getTitle());
        assertNotNull(manifest.getItems());
        assertTrue(manifest.getItems().size() >= 1);
    }

    @Test
    public void load_missing_resource_shouldThrow() {
        MenuLoader loader = new MenuLoader();
        assertThrows(IOException.class, () -> loader.loadFromResource("/non-existent.json"));
    }
}
