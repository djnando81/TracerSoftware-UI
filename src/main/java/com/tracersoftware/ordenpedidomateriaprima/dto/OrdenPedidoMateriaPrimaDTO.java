package com.tracersoftware.ordenpedidomateriaprima.dto;
import java.util.Date;
import java.util.List;

public class OrdenPedidoMateriaPrimaDTO {
    private int id;
    private int proveedorId;
    private String proveedorNombre;
    private Date fecha;
    private boolean confirmada;
    private List<OrdenPedidoDetalleDTO> detalles;
    // getters y setters
}
