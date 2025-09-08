package com.tracersoftware.proveedores.model;

import java.util.List;

public class ProveedorItem {
    private int id;
    private String nombre;
    private String cuit;
    private String direccion;
    private String telefono;
    private String email;
    private boolean activo;
    private List<MateriaPrimaItem> materiasPrimas;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCuit() { return cuit; }
    public void setCuit(String cuit) { this.cuit = cuit; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public List<MateriaPrimaItem> getMateriasPrimas() { return materiasPrimas; }
    public void setMateriasPrimas(List<MateriaPrimaItem> materiasPrimas) { this.materiasPrimas = materiasPrimas; }

    @Override
    public String toString() {
        String n = (nombre == null || nombre.isBlank()) ? ("Proveedor #" + id) : nombre;
        return id + " - " + n;
    }
}
