package com.tracersoftware.ordenesproduccion.dto;
import java.util.Date;

public class ProcedimientoMedicionDTO {
    private int id;
    private int procedimientoParametroId;
    private String parametroNombre;
    private String parametroUnidad;
    private boolean enRango;
    private Date fechaMedicion;
    private String medidoPor;
    private String observaciones;
    // getters y setters
}
