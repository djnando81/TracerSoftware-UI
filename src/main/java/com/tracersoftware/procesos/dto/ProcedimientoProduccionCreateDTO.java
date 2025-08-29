package com.tracersoftware.procesos.dto;
import java.util.List;

public class ProcedimientoProduccionCreateDTO {
    private String nombre;
    private String descripcion;
    private int orden;
    private boolean usaFormulacion;
    private List<ProcedimientoParametroCreateDTO> parametros;
    // getters y setters
}
