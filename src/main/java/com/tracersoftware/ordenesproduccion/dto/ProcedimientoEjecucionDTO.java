package com.tracersoftware.ordenesproduccion.dto;
import java.util.List;
import java.util.Date;

public class ProcedimientoEjecucionDTO {
    private int id;
    private int procedimientoProduccionId;
    private String procedimientoNombre;
    private String estadoPaso;
    private String observaciones;
    private String operadorResponsable;
    private List<ProcedimientoMedicionDTO> mediciones;
    // getters y setters
}
