package com.tracersoftware.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ServicesTest {

    @Test
    public void ordersService_reads_fake_json() throws Exception {
        FakeApiClient fake = new FakeApiClient();
        JsonNode data = fake.getJsonFromTestResource("orders.json");
        assertNotNull(data);
        assertTrue(data.isArray());
        assertEquals(2, data.size());
    }

    @Test
    public void inventoryService_reads_fake_json() throws Exception {
        FakeApiClient fake = new FakeApiClient();
        JsonNode data = fake.getJsonFromTestResource("inventory.json");
        assertNotNull(data);
        assertTrue(data.isArray());
        assertEquals(2, data.size());
    }

    @Test
    public void reportsService_reads_health() throws Exception {
        FakeApiClient fake = new FakeApiClient();
        JsonNode data = fake.getJsonFromTestResource("apihealth.json");
        assertNotNull(data);
        assertTrue(data.has("status"));
        assertEquals("ok", data.get("status").asText());
    }

    @Test
    public void settingsService_reads_settings() throws Exception {
        FakeApiClient fake = new FakeApiClient();
        JsonNode data = fake.getJsonFromTestResource("settings.json");
        assertNotNull(data);
        assertTrue(data.isArray());
    }
}
