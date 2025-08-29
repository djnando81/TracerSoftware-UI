package com.tracersoftware.embalajes.dto;
import java.util.List;

public class EmbalajeCreateDTO {
    private String nombreCombo;
    private String codigoInterno;
    private List<EmbalajeDetalleCreateDTO> detalles;
    private int inventarioProductoTerminadoId;
    private double cantidad;
    private String unidad;
    // getters y setters
}
