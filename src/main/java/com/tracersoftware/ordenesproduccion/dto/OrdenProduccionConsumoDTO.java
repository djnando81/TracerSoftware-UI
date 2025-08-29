package com.tracersoftware.ordenesproduccion.dto;
import java.util.Date;

public class OrdenProduccionConsumoDTO {
    private int id;
    private int productoId;
    private String productoNombre;
    private boolean esAditivo;
    private String lote;
    private double cantidad;
    private String unidadMedida;
    private Date fechaConsumo;
    private String creadoPor;
    // getters y setters
}
