package com.tracersoftware.usuarios.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracersoftware.usuarios.api.UsuariosApiService;
import com.tracersoftware.usuarios.api.RolesApiService;
import java.util.HashMap;

public class UsuarioFormController {

    @FXML private TextField txtNombre;
    @FXML private TextField txtEmail;
    @FXML private CheckBox chkActivo;
    @FXML private javafx.scene.control.ComboBox<String> cboRol;
    @FXML private javafx.scene.layout.AnchorPane overlay;
    @FXML private javafx.scene.control.PasswordField txtPassword;
    @FXML private javafx.scene.control.PasswordField txtPassword2;
    @FXML private ProgressIndicator overlayProgress;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Label lblError;
    @FXML private ProgressIndicator progress;
    @FXML private Label lblTitle;

    private final UsuariosApiService service = new UsuariosApiService();
    private final ObjectMapper mapper = new ObjectMapper();
    private final RolesApiService rolesService = new RolesApiService();
    // map name->id and id->name
    private final java.util.Map<String, Integer> roleNameToId = new HashMap<>();
    private final java.util.Map<Integer, String> roleIdToName = new HashMap<>();
    private Integer editingId = null;
    // test hooks
    public void setService(UsuariosApiService service) {
        // used in tests to inject a mock
        try {
            java.lang.reflect.Field f = this.getClass().getDeclaredField("service");
            f.setAccessible(true);
            f.set(this, service);
        } catch (Exception ignored) {}
    }

    // helper to allow synchronous testing of submission logic
    public void submitPayloadForTest(com.fasterxml.jackson.databind.node.ObjectNode payload) throws Exception {
        // directly call service (synchronous) for unit testing
        this.service.createUser(payload);
    }

    @FXML 
    public void initialize() {
        btnSave.setOnAction(evt -> onSave());
        btnCancel.setOnAction(evt -> onCancel());
        try {
            if (btnSave != null) {
                btnSave.setText("Crear");
                btnSave.getStyleClass().removeAll("btn-action-update");
                if (!btnSave.getStyleClass().contains("btn-action-create")) btnSave.getStyleClass().add("btn-action-create");
            }
            if (btnCancel != null) {
                if (!btnCancel.getStyleClass().contains("btn-action-cancel")) btnCancel.getStyleClass().add("btn-action-cancel");
            }
            if (lblTitle != null) lblTitle.setText("Nuevo Usuario");
        } catch (Exception ignored) {}
        lblError.setText("");
        progress.setVisible(false);
        // populate roles from API (fallback static list)
        Task<Void> rl = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    var list = rolesService.listRoles();
                    javafx.application.Platform.runLater(() -> {
                        cboRol.getItems().clear();
                        for (RolesApiService.RoleItem r : list) {
                            // show only name but keep id mapping
                            roleNameToId.put(r.nombre, r.id);
                            roleIdToName.put(r.id, r.nombre);
                            cboRol.getItems().add(r.nombre);
                        }
                        if (!cboRol.getItems().isEmpty()) cboRol.getSelectionModel().select(0);
                        // if editing and editingId present, try select corresponding name
                        try { if (editingId != null && roleIdToName.containsKey(editingId)) cboRol.getSelectionModel().select(roleIdToName.get(editingId)); } catch (Exception ignored) {}
                    });
                } catch (Exception ex) {
                    // fallback static
                    javafx.application.Platform.runLater(() -> {
                        cboRol.getItems().clear();
                        cboRol.getItems().addAll("Admin","Usuario","Invitado");
                        // fallback mapping
                        roleNameToId.put("Admin", 1); roleNameToId.put("Usuario",2); roleNameToId.put("Invitado",3);
                        roleIdToName.put(1, "Admin"); roleIdToName.put(2, "Usuario"); roleIdToName.put(3, "Invitado");
                        cboRol.getSelectionModel().select(1);
                        try { if (editingId != null && roleIdToName.containsKey(editingId)) cboRol.getSelectionModel().select(roleIdToName.get(editingId)); } catch (Exception ignored) {}
                    });
                }
                return null;
            }
        };
        new Thread(rl, "roles-load").start();
    }

    private boolean validateInputs() {
        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
        String email = txtEmail.getText() == null ? "" : txtEmail.getText().trim();
        if (nombre.isEmpty()) {
            lblError.setText("El nombre es obligatorio.");
            return false;
        }
        if (email.isEmpty() || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            lblError.setText("Email inválido.");
            return false;
        }
        lblError.setText("");
        return true;
    }

    private void setLoading(boolean loading) {
        progress.setVisible(loading);
        btnSave.setDisable(loading);
        btnCancel.setDisable(loading);
        txtNombre.setDisable(loading);
        txtEmail.setDisable(loading);
        chkActivo.setDisable(loading);
    try { cboRol.setDisable(loading); } catch (Exception ignored) {}
    try { overlay.setVisible(loading); overlay.toFront(); } catch (Exception ignored) {}
    }

    private void onSave() {
        if (!validateInputs()) return;
        // Validación de contraseña (crear: obligatorio; editar: opcional pero debe coincidir si se informa)
        String p1 = null, p2 = null;
        try { p1 = txtPassword == null ? null : txtPassword.getText(); } catch (Exception ignored) {}
        try { p2 = txtPassword2 == null ? null : txtPassword2.getText(); } catch (Exception ignored) {}
        final boolean isEdit = (editingId != null && editingId > 0);
        if (!isEdit) {
            if (p1 == null || p1.trim().isEmpty()) { lblError.setText("La contraseña es obligatoria al crear."); return; }
            if (p1.length() < 6) { lblError.setText("La contraseña debe tener al menos 6 caracteres."); return; }
            if (p1 == null || !p1.equals(p2)) { lblError.setText("Las contraseñas no coinciden."); return; }
        } else {
            boolean any = (p1 != null && !p1.isBlank()) || (p2 != null && !p2.isBlank());
            if (any) {
                if (p1 == null || p2 == null || p1.isBlank() || p2.isBlank()) { lblError.setText("Debe completar y confirmar la contraseña."); return; }
                if (p1.length() < 6) { lblError.setText("La contraseña debe tener al menos 6 caracteres."); return; }
                if (!p1.equals(p2)) { lblError.setText("Las contraseñas no coinciden."); return; }
            }
        }
        setLoading(true);
        ObjectNode payload = mapper.createObjectNode();
    payload.put("nombre", txtNombre.getText().trim());
    payload.put("email", txtEmail.getText().trim());
        payload.put("activo", chkActivo.isSelected());
    // include role id as 'rolId' when available
    try {
        String selName = cboRol.getSelectionModel().getSelectedItem();
        if (selName != null) {
            Integer rid = roleNameToId.get(selName);
            if (rid != null) {
                // send PascalCase field expected by server
                payload.put("RolId", rid.intValue());
            } else payload.put("rol", selName);
        }
    } catch (Exception ignored) {}
        // incluir password en payload según reglas
        try {
            if (editingId == null || editingId <= 0) {
                if (p1 != null) payload.put("password", p1);
            } else {
                if (p1 != null && !p1.isBlank()) payload.put("password", p1);
            }
        } catch (Exception ignored) {}

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (isEdit) {
                    service.updateUser(editingId, payload);
                } else {
                    service.createUser(payload);
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            setLoading(false);
            try {
                com.tracersoftware.common.controls.MessageToast.show(null,
                        isEdit ? "Usuario actualizado correctamente" : "Usuario creado correctamente",
                        com.tracersoftware.common.controls.MessageToast.ToastType.SUCCESS);
            } catch (Exception ignored) {}
            closeWindow();
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            ex.printStackTrace();
            Platform.runLater(() -> {
                lblError.setText("Error al crear usuario: " + ex.getMessage());
                try {
                    com.tracersoftware.common.controls.MessageToast.show(null,
                            "Error guardando usuario: " + (ex == null ? "desconocido" : ex.getMessage()),
                            com.tracersoftware.common.controls.MessageToast.ToastType.ERROR);
                } catch (Exception ignored) {}
                setLoading(false);
            });
        });
        Thread t = new Thread(task, "usuario-create");
        t.setDaemon(true);
        t.start();
    }

    // Called by list controller when editing an existing user
    public void setUsuario(com.tracersoftware.usuarios.model.UsuarioDTO u) {
        if (u == null) return;
        this.editingId = u.getId();
        try {
            if (btnSave != null) {
                btnSave.setText("Actualizar");
                btnSave.getStyleClass().removeAll("btn-action-create");
                if (!btnSave.getStyleClass().contains("btn-action-update")) btnSave.getStyleClass().add("btn-action-update");
            }
            if (lblTitle != null) lblTitle.setText("Editar Usuario");
        } catch (Exception ignored) {}
        txtNombre.setText(u.getNombre());
        txtEmail.setText(u.getEmail());
        chkActivo.setSelected(u.isActivo());
    try {
        if (u.getRol() != null) {
            // if we have mapping name->id, select by name; else store editingId so async loader can select
            if (roleNameToId.containsKey(u.getRol())) cboRol.getSelectionModel().select(u.getRol());
            else {
                // we may have received only name or the DTO carries rol as name; try parse int
                try { this.editingId = Integer.parseInt(u.getRol()); } catch (Exception ex) { /* store name */ cboRol.getSelectionModel().select(u.getRol()); }
            }
        }
    } catch (Exception ignored) {}
    }

    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Platform.runLater(() -> {
            Stage s = (Stage) btnCancel.getScene().getWindow();
            s.close();
        });
    }
}
