package com.tracersoftware.proveedores.controller;

import com.tracersoftware.proveedores.model.MateriaPrimaItem;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class MateriaPrimaModalController {
    @FXML private Label lblTitle;
    @FXML private TextField txtNombre;
    @FXML private ComboBox<String> cmbUnidad;
    @FXML private CheckBox chkActiva;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Label lblError;
    @FXML private ProgressIndicator progress;

    private MateriaPrimaItem result;
    private boolean cancelled = true;

    @FXML
    public void initialize() {
        // OBLIGATORIO: Configuración de botones según estándares
        if (btnSave != null) {
            btnSave.setOnAction(e -> save());
        }
        
        if (btnCancel != null) {
            btnCancel.setOnAction(e -> cancel());
        }
        
        // Validación en tiempo real
        setupValidation();
    }
    
    private void setupValidation() {
        if (txtNombre != null) {
            txtNombre.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        }
        
        validateForm();
    }
    
    private void validateForm() {
        boolean valid = true;
        
        // Nombre obligatorio
        if (txtNombre == null || txtNombre.getText() == null || txtNombre.getText().trim().isEmpty()) {
            valid = false;
        }
        
        if (btnSave != null) {
            btnSave.setDisable(!valid);
        }
    }
    
    @FXML
    private void save() {
        if (!validateFields()) return;
        
        try {
            // Crear nueva materia prima con los datos del formulario
            result = new MateriaPrimaItem();
            result.setNombre(txtNombre.getText().trim());
            result.setCategoriaMateriaPrimaNombre("General"); // Categoría por defecto
            result.setUnidad(cmbUnidad != null && cmbUnidad.getValue() != null ? cmbUnidad.getValue() : "Kg");
            result.setCostoUnitario(0.0); // Valores por defecto
            result.setStockActual(0.0);
            result.setActiva(chkActiva != null ? chkActiva.isSelected() : true);
            
            cancelled = false;
            closeWindow();
            
        } catch (Exception ex) {
            showError("Error al crear materia prima: " + ex.getMessage());
        }
    }
    
    private boolean validateFields() {
        // Validación nombre
        if (txtNombre == null || txtNombre.getText() == null || txtNombre.getText().trim().isEmpty()) {
            showError("El nombre es obligatorio");
            if (txtNombre != null) txtNombre.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private void showError(String message) {
        if (lblError != null) {
            lblError.setText(message);
            lblError.setVisible(true);
            lblError.setManaged(true);
        }
    }
    
    @FXML
    private void cancel() {
        cancelled = true;
        result = null;
        closeWindow();
    }
    
    private void closeWindow() {
        if (btnCancel != null && btnCancel.getScene() != null) {
            Stage stage = (Stage) btnCancel.getScene().getWindow();
            if (stage != null) stage.close();
        }
    }
    
    /**
     * Obtiene el resultado de la creación de materia prima.
     * @return La materia prima creada o null si se canceló
     */
    public MateriaPrimaItem getResult() {
        return result;
    }
    
    /**
     * Indica si el modal fue cancelado.
     * @return true si se canceló, false si se guardó
     */
    public boolean isCancelled() {
        return cancelled;
    }
}
