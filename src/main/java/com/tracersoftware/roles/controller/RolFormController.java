package com.tracersoftware.roles.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tracersoftware.common.controls.MessageToast;
import com.tracersoftware.roles.api.RolesApiService;
import com.tracersoftware.roles.model.RolDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class RolFormController {
    @FXML private TextField txtNombre;
    @FXML private TextArea txtPermisos;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Label lblError;

    private final RolesApiService service = new RolesApiService();
    private Integer editingId = null;

    @FXML
    public void initialize() {
        try { btnSave.getStyleClass().addAll("button-green","btn-small"); btnCancel.getStyleClass().addAll("button-red","btn-small"); } catch (Exception ignored) {}
        btnSave.setOnAction(e -> onSave());
        btnCancel.setOnAction(e -> ((javafx.stage.Stage)btnCancel.getScene().getWindow()).close());
    }

    public void setRol(RolDTO r) {
        if (r == null) return;
        editingId = r.getId();
        txtNombre.setText(r.getNombre());
        txtPermisos.setText(r.getPermisos());
        try { btnSave.getStyleClass().remove("button-green"); btnSave.getStyleClass().add("button-blue"); } catch (Exception ignored) {}
    }

    private void onSave() {
        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
        if (nombre.isEmpty()) { lblError.setText("El nombre es obligatorio"); return; }
        String permisos = txtPermisos.getText() == null ? "" : txtPermisos.getText().trim();
        ObjectNode json = new ObjectMapper().createObjectNode();
        json.put("nombre", nombre);
        json.put("permisos", permisos);
        javafx.concurrent.Task<Void> t = new javafx.concurrent.Task<>() {
            @Override protected Void call() throws Exception {
                if (editingId == null) service.create(json); else service.update(editingId, json);
                return null;
            }
        };
        t.setOnSucceeded(ev -> {
            MessageToast.show(null, editingId == null ? "Rol creado" : "Rol actualizado", MessageToast.ToastType.SUCCESS);
            ((javafx.stage.Stage)btnCancel.getScene().getWindow()).close();
        });
        t.setOnFailed(ev -> {
            Throwable ex = t.getException(); if (ex != null) ex.printStackTrace();
            MessageToast.show(null, "Error guardando rol: " + (t.getException()==null?"":t.getException().getMessage()), MessageToast.ToastType.ERROR);
        });
        new Thread(t, "rol-save").start();
    }
}

