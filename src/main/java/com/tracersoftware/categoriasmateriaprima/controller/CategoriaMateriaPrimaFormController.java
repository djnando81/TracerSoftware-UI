package com.tracersoftware.categoriasmateriaprima.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tracersoftware.categoriasmateriaprima.api.CategoriasMateriaPrimaApiService;
import com.tracersoftware.categoriasmateriaprima.model.CategoriaMateriaPrimaItem;
import com.tracersoftware.common.controls.MessageToast;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class CategoriaMateriaPrimaFormController {
    @FXML private TextField txtNombre;
    @FXML private CheckBox chkActiva;
    @FXML private Label lblError;
    @FXML private Label lblTitle;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private ProgressIndicator progress;
    @FXML private javafx.scene.layout.AnchorPane overlay;
    @FXML private ProgressIndicator overlayProgress;

    private final CategoriasMateriaPrimaApiService service = new CategoriasMateriaPrimaApiService();
    private final ObjectMapper mapper = new ObjectMapper();
    private Integer editingId = null;
    private Runnable onSaved;

    @FXML public void initialize() {
        if (btnCancel != null) btnCancel.setOnAction(e->close());
        if (btnSave != null) btnSave.setOnAction(e->save());
        
        // Configurar elementos visuales por defecto
        if (chkActiva != null) chkActiva.setSelected(true);
        if (progress != null) progress.setVisible(false);
        if (overlay != null) overlay.setVisible(false);
        if (overlayProgress != null) {
            overlayProgress.setLayoutX(32);
            overlayProgress.setLayoutY(32);
        }
    }

    public void setOnSaved(Runnable r) { this.onSaved = r; }

    public void edit(CategoriaMateriaPrimaItem item) {
        if (item == null) return; 
        editingId = item.getId();
        if (txtNombre != null) txtNombre.setText(item.getNombre()); 
        if (chkActiva != null) chkActiva.setSelected(item.isActiva());
        
        // Cambiar título y texto del botón según modo
        if (lblTitle != null) lblTitle.setText("Editar Categoría");
        if (btnSave != null) {
            btnSave.setText("Actualizar");
            btnSave.getStyleClass().removeAll("btn-action-create");
            btnSave.getStyleClass().add("btn-action-update");
        }
    }

    private void save() {
        // Validación básica
        String nombre = txtNombre != null ? txtNombre.getText() : "";
        if (nombre == null || nombre.trim().isEmpty()) {
            showError("El nombre es requerido");
            return;
        }

        // Mostrar overlay de carga
        setLoading(true);
        hideError();

        Task<Void> saveTask = new Task<>() {
            @Override protected Void call() throws Exception {
                ObjectNode body = mapper.createObjectNode();
                body.put("nombre", nombre.trim());
                body.put("activa", chkActiva != null ? chkActiva.isSelected() : true);
                
                if (editingId == null) {
                    service.create(body);
                } else {
                    service.update(editingId, body);
                }
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            setLoading(false);
            if (onSaved != null) onSaved.run();
            MessageToast.show(null, editingId == null ? "Categoría creada exitosamente" : "Categoría actualizada exitosamente", MessageToast.ToastType.SUCCESS);
            close();
        });

        saveTask.setOnFailed(e -> {
            setLoading(false);
            Throwable ex = saveTask.getException();
            String message = ex != null ? ex.getMessage() : "Error desconocido";
            showError("Error al guardar: " + message);
            MessageToast.showSystemError(null, "No se pudo guardar la categoría: " + message);
        });

        new Thread(saveTask, "catmp-save").start();
    }

    private void setLoading(boolean loading) {
        if (progress != null) progress.setVisible(loading);
        if (overlay != null) overlay.setVisible(loading);
        if (btnSave != null) btnSave.setDisable(loading);
        if (btnCancel != null) btnCancel.setDisable(loading);
        if (txtNombre != null) txtNombre.setDisable(loading);
        if (chkActiva != null) chkActiva.setDisable(loading);
    }

    private void showError(String message) {
        if (lblError != null) {
            lblError.setText(message);
            lblError.setVisible(true);
            lblError.setManaged(true);
        }
    }

    private void hideError() {
        if (lblError != null) {
            lblError.setVisible(false);
            lblError.setManaged(false);
        }
    }

    private void close() { 
        try { 
            ((Stage) btnCancel.getScene().getWindow()).close(); 
        } catch (Exception ignored) {} 
    }
}

