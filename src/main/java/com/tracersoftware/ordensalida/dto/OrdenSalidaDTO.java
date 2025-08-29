package com.tracersoftware.ordensalida.dto;
import java.util.Date;
import java.util.List;

public class OrdenSalidaDTO {
    private int id;
    private String cliente;
    private Date fecha;
    private boolean confirmada;
    private List<OrdenSalidaDetalleDTO> detalles;
    // getters y setters
}
