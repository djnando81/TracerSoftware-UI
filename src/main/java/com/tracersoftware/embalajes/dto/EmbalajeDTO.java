package com.tracersoftware.embalajes.dto;
import java.util.List;

public class EmbalajeDTO {
    private int id;
    private String nombreCombo;
    private String codigoInterno;
    private double costoTotal;
    private List<EmbalajeDetalleDTO> detalles;
    private String producto;
    private String lote;
    private String unidad;
    private double cantidad;
    private double costoUnitario;
    // getters y setters
}
