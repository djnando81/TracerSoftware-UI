package com.tracersoftware.ordenesproduccion.dto;
import java.util.List;

public class OrdenProduccionCreateDTO {
    private int procesoProduccionId;
    private double cantidadPlanificada;
    private int unidadMedidaId;
    private String notas;
    private List<OrdenProduccionItemPlanDTO> itemsEsperados;
    private int productoTerminadoId;
    // getters y setters
}
