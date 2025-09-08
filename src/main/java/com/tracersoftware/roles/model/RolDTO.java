package com.tracersoftware.roles.model;

public class RolDTO {
    private int id; // maps RolId
    private String nombre;
    private String permisos;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getPermisos() { return permisos; }
    public void setPermisos(String permisos) { this.permisos = permisos; }
}

