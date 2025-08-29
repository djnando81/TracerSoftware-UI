package com.tracersoftware.procesos.dto;
import java.util.List;

public class ProcesoProduccionCreateDTO {
    private String nombre;
    private String descripcion;
    private boolean activo;
    private List<ProcedimientoProduccionCreateDTO> procedimientos;
    // getters y setters
}
