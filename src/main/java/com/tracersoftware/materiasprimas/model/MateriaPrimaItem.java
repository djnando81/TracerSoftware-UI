package com.tracersoftware.materiasprimas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MateriaPrimaItem {
    private int id;
    private String fecha; // ISO string for display
    private int materiaPrimaId;
    private String materiaPrimaNombre;
    private String loteInterno;
    private int almacenId;
    private String almacenNombre;
    private double cantidad;
    private String unidad;
    private String motivo;
    private String usuarioCreacion;
    private boolean aprobada;
    private boolean activa;
    private String nombre;
    private String codigoInterno;
    private int categoriaMateriaPrimaId;
    private String categoriaMateriaPrimaNombre;
    private int materiaPrimaOrigenId;
    private String materiaPrimaOrigenNombre;
    private double stockMinimo;
    private double stockMaximo;
    private double stockActual;
    private double costoUnitario;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    @JsonIgnore public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    @JsonIgnore public int getMateriaPrimaId() { return materiaPrimaId; }
    public void setMateriaPrimaId(int materiaPrimaId) { this.materiaPrimaId = materiaPrimaId; }
    @JsonIgnore public String getMateriaPrimaNombre() { return materiaPrimaNombre; }
    public void setMateriaPrimaNombre(String materiaPrimaNombre) { this.materiaPrimaNombre = materiaPrimaNombre; }
    @JsonIgnore public String getLoteInterno() { return loteInterno; }
    public void setLoteInterno(String loteInterno) { this.loteInterno = loteInterno; }
    @JsonIgnore public int getAlmacenId() { return almacenId; }
    public void setAlmacenId(int almacenId) { this.almacenId = almacenId; }
    @JsonIgnore public String getAlmacenNombre() { return almacenNombre; }
    public void setAlmacenNombre(String almacenNombre) { this.almacenNombre = almacenNombre; }
    @JsonIgnore public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    @JsonIgnore public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    @JsonIgnore public String getUsuarioCreacion() { return usuarioCreacion; }
    public void setUsuarioCreacion(String usuarioCreacion) { this.usuarioCreacion = usuarioCreacion; }
    @JsonIgnore public boolean isAprobada() { return aprobada; }
    public void setAprobada(boolean aprobada) { this.aprobada = aprobada; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCodigoInterno() { return codigoInterno; }
    public void setCodigoInterno(String codigoInterno) { this.codigoInterno = codigoInterno; }
    public int getCategoriaMateriaPrimaId() { return categoriaMateriaPrimaId; }
    public void setCategoriaMateriaPrimaId(int categoriaMateriaPrimaId) { this.categoriaMateriaPrimaId = categoriaMateriaPrimaId; }
    public String getCategoriaMateriaPrimaNombre() { return categoriaMateriaPrimaNombre; }
    public void setCategoriaMateriaPrimaNombre(String categoriaMateriaPrimaNombre) { this.categoriaMateriaPrimaNombre = categoriaMateriaPrimaNombre; }
    public int getMateriaPrimaOrigenId() { return materiaPrimaOrigenId; }
    public void setMateriaPrimaOrigenId(int materiaPrimaOrigenId) { this.materiaPrimaOrigenId = materiaPrimaOrigenId; }
    public String getMateriaPrimaOrigenNombre() { return materiaPrimaOrigenNombre; }
    public void setMateriaPrimaOrigenNombre(String materiaPrimaOrigenNombre) { this.materiaPrimaOrigenNombre = materiaPrimaOrigenNombre; }
    public double getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(double stockMinimo) { this.stockMinimo = stockMinimo; }
    public double getStockMaximo() { return stockMaximo; }
    public void setStockMaximo(double stockMaximo) { this.stockMaximo = stockMaximo; }
    public double getStockActual() { return stockActual; }
    public void setStockActual(double stockActual) { this.stockActual = stockActual; }
    public double getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(double costoUnitario) { this.costoUnitario = costoUnitario; }
}
