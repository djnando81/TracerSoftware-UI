package com.tracersoftware.proveedores.dto;
import java.util.List;
import com.tracersoftware.materiasprimas.dto.MateriaPrimaDTO;

public class ProveedorDTO {
    private int id;
    private String nombre;
    private String cuit;
    private String direccion;
    private String telefono;
    private String email;
    private boolean activo;
    private List<MateriaPrimaDTO> materiasPrimas;
    // getters y setters
}
