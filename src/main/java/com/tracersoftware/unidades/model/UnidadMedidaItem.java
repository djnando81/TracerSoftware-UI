package com.tracersoftware.unidades.model;

public class UnidadMedidaItem {
    private int id;
    private String nombre;
    private String abreviatura;
    private boolean activa;
    private String tipo;
    private boolean esBasica;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getAbreviatura() { return abreviatura; }
    public void setAbreviatura(String abreviatura) { this.abreviatura = abreviatura; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public boolean isEsBasica() { return esBasica; }
    public void setEsBasica(boolean esBasica) { this.esBasica = esBasica; }

    @Override
    public String toString() { return (abreviatura == null?"":abreviatura) + (nombre!=null? (" - " + nombre):""); }
}

