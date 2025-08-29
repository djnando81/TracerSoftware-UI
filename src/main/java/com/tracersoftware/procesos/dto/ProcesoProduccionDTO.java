package com.tracersoftware.procesos.dto;
import java.util.Date;
import java.util.List;

public class ProcesoProduccionDTO {
    private int id;
    private String nombre;
    private String descripcion;
    private boolean activo;
    private Date fechaCreacion;
    private List<ProcedimientoProduccionDTO> procedimientos;
    // getters y setters
}
