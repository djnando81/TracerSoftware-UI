package com.tracersoftware.proveedores.model;

import java.util.List;

public class MateriaPrimaItem {
    private int id;
    private String nombre;
    private String codigoInterno;
    private String unidad;
    private int categoriaMateriaPrimaId;
    private String categoriaMateriaPrimaNombre;
    private boolean activa;
    private Integer materiaPrimaOrigenId;
    private String materiaPrimaOrigenNombre;
    private List<MateriaPrimaDerivada> derivadas;
    private double stockMinimo;
    private double stockMaximo;
    private double stockActual;
    private double costoUnitario;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCodigoInterno() { return codigoInterno; }
    public void setCodigoInterno(String codigoInterno) { this.codigoInterno = codigoInterno; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    public int getCategoriaMateriaPrimaId() { return categoriaMateriaPrimaId; }
    public void setCategoriaMateriaPrimaId(int categoriaMateriaPrimaId) { this.categoriaMateriaPrimaId = categoriaMateriaPrimaId; }

    public String getCategoriaMateriaPrimaNombre() { return categoriaMateriaPrimaNombre; }
    public void setCategoriaMateriaPrimaNombre(String categoriaMateriaPrimaNombre) { this.categoriaMateriaPrimaNombre = categoriaMateriaPrimaNombre; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    public Integer getMateriaPrimaOrigenId() { return materiaPrimaOrigenId; }
    public void setMateriaPrimaOrigenId(Integer materiaPrimaOrigenId) { this.materiaPrimaOrigenId = materiaPrimaOrigenId; }

    public String getMateriaPrimaOrigenNombre() { return materiaPrimaOrigenNombre; }
    public void setMateriaPrimaOrigenNombre(String materiaPrimaOrigenNombre) { this.materiaPrimaOrigenNombre = materiaPrimaOrigenNombre; }

    public List<MateriaPrimaDerivada> getDerivadas() { return derivadas; }
    public void setDerivadas(List<MateriaPrimaDerivada> derivadas) { this.derivadas = derivadas; }

    public double getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(double stockMinimo) { this.stockMinimo = stockMinimo; }

    public double getStockMaximo() { return stockMaximo; }
    public void setStockMaximo(double stockMaximo) { this.stockMaximo = stockMaximo; }

    public double getStockActual() { return stockActual; }
    public void setStockActual(double stockActual) { this.stockActual = stockActual; }

    public double getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(double costoUnitario) { this.costoUnitario = costoUnitario; }

    public static class MateriaPrimaDerivada {
        private int id;
        private String nombre;
        private String unidad;
        private double porcentajeObtenido;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }

        public String getUnidad() { return unidad; }
        public void setUnidad(String unidad) { this.unidad = unidad; }

        public double getPorcentajeObtenido() { return porcentajeObtenido; }
        public void setPorcentajeObtenido(double porcentajeObtenido) { this.porcentajeObtenido = porcentajeObtenido; }
    }
}
