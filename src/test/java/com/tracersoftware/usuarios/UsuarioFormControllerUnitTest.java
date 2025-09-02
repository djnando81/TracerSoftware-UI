package com.tracersoftware.usuarios;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracersoftware.usuarios.api.UsuariosApiService;
import com.tracersoftware.usuarios.controller.UsuarioFormController;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UsuarioFormControllerUnitTest {

    @Test
    public void testSubmitInvokesService() throws Exception {
        UsuarioFormController ctrl = new UsuarioFormController();
        UsuariosApiService mock = Mockito.mock(UsuariosApiService.class);
        ctrl.setService(mock);

        ObjectMapper m = new ObjectMapper();
        ObjectNode payload = m.createObjectNode();
        payload.put("nombre", "Test User");
        payload.put("email", "test@example.com");
        payload.put("activo", true);

        // call the synchronous test helper
        ctrl.submitPayloadForTest(payload);

        verify(mock, times(1)).createUser(payload);
    }
}
