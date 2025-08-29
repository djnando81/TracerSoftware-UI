package com.tracersoftware.usuarios.model;

public class UsuarioDTO {
    private int id;
    private String nombre;
    private String email;
    private boolean activo;
    private String rol;
    private Integer rolId;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public Integer getRolId() { return rolId; }
    public void setRolId(Integer rolId) { this.rolId = rolId; }
}
