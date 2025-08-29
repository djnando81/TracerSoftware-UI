package com.tracersoftware.usuarios;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracersoftware.usuarios.api.UsuariosApiService;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UsuariosApiServiceTest {

    @Test
    public void testListUsersFromFake() throws Exception {
        ObjectMapper m = new ObjectMapper();
        InputStream is = getClass().getResourceAsStream("/fake-responses/usuarios.json");
        assertNotNull(is, "El recurso de prueba debe existir");
        JsonNode arr = m.readTree(is);
        assertTrue(arr.isArray());
        assertEquals(2, arr.size());

        // Basic sanity mapping
        JsonNode a = arr.get(0);
        assertEquals(1, a.path("id").asInt());
        assertEquals("Alice", a.path("nombre").asText());
    }
}
