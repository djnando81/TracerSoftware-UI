package com.tracersoftware.entradasmateriaprima.dto;
import java.util.Date;
import java.util.List;

public class EntradaMateriaPrimaDTO {
    private int id;
    private int ordenPedidoDetalleId;
    private String materiaPrimaNombre;
    private String proveedorNombre;
    private double cantidadRecibida;
    private String unidad;
    private String loteInterno;
    private Date fechaRecepcion;
    private String ubicacion;
    private boolean validada;
    private String observaciones;
    private String almacenNombre;
    private List<EntradaMPDistribucionItemDTO> distribucion;
    // getters y setters
}
