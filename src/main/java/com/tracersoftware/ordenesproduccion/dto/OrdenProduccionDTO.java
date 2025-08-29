package com.tracersoftware.ordenesproduccion.dto;
import java.util.Date;
import java.util.List;

public class OrdenProduccionDTO {
    private int id;
    private String codigo;
    private int procesoProduccionId;
    private String procesoNombre;
    private String estado;
    private double cantidadPlanificada;
    private String unidadMedida;
    private String notas;
    private Date fechaCreacion;
    private String creadoPor;
    private List<OrdenProduccionItemDTO> items;
    private List<OrdenProduccionConsumoDTO> consumos;
    private List<ProcedimientoEjecucionDTO> ejecucionesProcedimientos;
    // getters y setters
}
