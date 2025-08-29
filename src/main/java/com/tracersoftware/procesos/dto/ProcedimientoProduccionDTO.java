package com.tracersoftware.procesos.dto;
import java.util.List;

public class ProcedimientoProduccionDTO {
    private int id;
    private String nombre;
    private String descripcion;
    private int orden;
    private boolean usaFormulacion;
    private List<ProcedimientoParametroDTO> parametros;
    // getters y setters
}
