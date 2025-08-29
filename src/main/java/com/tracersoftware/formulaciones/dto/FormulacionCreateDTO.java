package com.tracersoftware.formulaciones.dto;
import java.util.List;

public class FormulacionCreateDTO {
    private String nombre;
    private String codigo;
    private boolean esSoloAditivos;
    private List<FormulacionDetalleCreateDTO> detalles;
    // getters y setters
}
