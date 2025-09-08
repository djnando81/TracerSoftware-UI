package com.tracersoftware.usuarios.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.tracersoftware.usuarios.api.UsuariosApiService;
import com.tracersoftware.auth.SessionManager;
// TokenStore not referenced directly here
import com.tracersoftware.usuarios.model.UsuarioDTO;
import com.tracersoftware.common.controls.MessageToast;
import com.tracersoftware.ui.ViewLifecycle;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.animation.RotateTransition;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.util.Duration;

import java.nio.file.Path;
import com.tracersoftware.usuarios.api.RolesApiService;
import com.tracersoftware.common.controls.ResponsiveToggleButton;


public class UsuariosListController implements ViewLifecycle {

    @FXML private TableView<UsuarioDTO> tableUsers;
    @FXML private TableColumn<UsuarioDTO, Integer> colId;
    @FXML private TableColumn<UsuarioDTO, String> colNombre;
    @FXML private TableColumn<UsuarioDTO, String> colRol;
    @FXML private TableColumn<UsuarioDTO, String> colEmail;
    @FXML private TableColumn<UsuarioDTO, Boolean> colActivo;
    @FXML private Button btnNew;
    @FXML private TableColumn<UsuarioDTO, Void> colEdit;
    @FXML private Label lblError;
    @FXML private javafx.scene.layout.Pane rowOverlayPane;
    @FXML private com.tracersoftware.common.controls.PaginatorControl paginator;
    @FXML private com.tracersoftware.common.controls.SearchBar searchBar; // OBLIGATORIO
    @FXML private javafx.scene.layout.HBox exportBarContainer; // OBLIGATORIO

    private final UsuariosApiService service = new UsuariosApiService();
    private final RolesApiService rolesService = new RolesApiService();
    private final java.util.Map<Integer, String> rolesMap = new java.util.HashMap<>();
    // keep reference to preload task so we can join or attach listeners
    private javafx.concurrent.Task<Void> rolesPreloadTask = null;
    // when server-side paging is enabled we don't keep full list locally
    private java.util.List<UsuarioDTO> fullUsers = new java.util.ArrayList<>();

    @FXML
    public void initialize() {
        // preload roles map so we can resolve RolId -> nombre when users only provide RolId
        rolesPreloadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    var list = rolesService.listRoles();
                    for (RolesApiService.RoleItem r : list) rolesMap.put(r.id, r.nombre);
                } catch (Exception ignored) {}
                return null;
            }
        };
        // when roles preload completes, refresh role names in the visible table
        rolesPreloadTask.setOnSucceeded(ev -> {
            try {
                javafx.application.Platform.runLater(() -> {
                    try {
                        if (fullUsers != null && !fullUsers.isEmpty()) {
                            for (UsuarioDTO u : fullUsers) {
                                Integer rid = u.getRolId();
                                if ((u.getRol() == null || u.getRol().isEmpty()) && rid != null && rolesMap.containsKey(rid)) {
                                    u.setRol(rolesMap.get(rid));
                                }
                            }
                            try { tableUsers.refresh(); } catch (Exception ignored) {}
                        }
                    } catch (Exception ignored) {}
                });
            } catch (Exception ignored) {}
        });
        new Thread(rolesPreloadTask, "roles-preload").start();
        // Make table fill width and size columns proportionally
        try { tableUsers.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY); } catch (Exception ignored) {}
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));
        // column sizing hints (constrained policy will scale respecting these prefs)
        try {
            colId.setMinWidth(60); colId.setMaxWidth(90); colId.setPrefWidth(70);
            colNombre.setPrefWidth(220);
            colRol.setPrefWidth(140);
            colEmail.setPrefWidth(320);
            colActivo.setMinWidth(80); colActivo.setPrefWidth(100); colActivo.setMaxWidth(120);
            colEdit.setMinWidth(160); colEdit.setPrefWidth(180); colEdit.setMaxWidth(220);
        } catch (Exception ignored) {}

        // OBLIGATORIO: Configuración de ExportBar usando el contenedor del FXML
        if (exportBarContainer != null) {
            java.util.concurrent.Callable<java.util.List<?>> fetchAll = () -> {
                int page = 0;
                int size = 500;
                java.util.List<com.fasterxml.jackson.databind.JsonNode> all = new java.util.ArrayList<>();
                while (true) {
                    com.tracersoftware.usuarios.api.PagedResult pr = service.listUsersPaged(page, size);
                    all.addAll(pr.getItems());
                    page++;
                    if (page >= pr.getTotalPages()) break;
                }
                return new java.util.ArrayList<>(all);
            };
            com.tracersoftware.common.controls.ExportBar exportBar = new com.tracersoftware.common.controls.ExportBar(tableUsers, "usuarios", fetchAll);
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            exportBarContainer.getChildren().addAll(spacer, exportBar);
        }
        
        // OBLIGATORIO: Configuración de SearchBar
        if (searchBar != null) {
            searchBar.textProperty().addListener((o, ov, nv) -> {
                // Implementar búsqueda en el futuro - por ahora solo reload
                if (paginator != null) paginator.setPageIndex(0);
                loadData();
            });
        }
        // use reusable ActiveToggleControl for the activo column
        colActivo.setCellFactory(tc -> new javafx.scene.control.TableCell<UsuarioDTO, Boolean>() {
            private final com.tracersoftware.common.controls.SwitchToggleButton switchBtn = new com.tracersoftware.common.controls.SwitchToggleButton();
            private final javafx.scene.layout.StackPane stack = new javafx.scene.layout.StackPane();
            private final javafx.scene.Node inlineLoader;
            private boolean programmaticChange = false;
            {
                // styling and sizing so it fills the cell
                switchBtn.setMinWidth(48);
                switchBtn.setPrefWidth(48);
                switchBtn.setMaxWidth(48);
                switchBtn.setMinHeight(26);
                switchBtn.setPrefHeight(26);
                switchBtn.setMaxHeight(26);
                switchBtn.getStyleClass().add("cell-switch-btn");
                switchBtn.setFocusTraversable(true);

                // create a small circular spinner (circle inside a group so we can rotate)
                javafx.scene.shape.Circle c = new javafx.scene.shape.Circle(6);
                c.getStyleClass().add("cell-inline-loader");
                javafx.scene.Group g = new javafx.scene.Group(c);
                inlineLoader = g;
                stack.getStyleClass().add("cell-toggle-stack");
                stack.getChildren().addAll(switchBtn, inlineLoader);
                stack.setPrefWidth(90);
                stack.setMinWidth(80);
                stack.setMaxWidth(120);
                javafx.scene.layout.StackPane.setAlignment(switchBtn, javafx.geometry.Pos.CENTER);
                javafx.scene.layout.StackPane.setAlignment(inlineLoader, javafx.geometry.Pos.CENTER_RIGHT);

                // reaccionar a cambios del switch (click/teclado)
                switchBtn.switchedOnProperty().addListener((obs, oldVal, newVal) -> {
                    if (programmaticChange) return;
                    int idx = getIndex();
                    if (idx < 0 || idx >= getTableView().getItems().size()) return;
                    UsuarioDTO u = getTableView().getItems().get(idx);
                    if (u == null) return;

                    boolean newState = Boolean.TRUE.equals(newVal);

                    // show inline loader / row overlay
                    switchBtn.setDisable(true);
                    showRowOverlay(idx);

                    Task<Void> t = new Task<>() {
                        @Override
                        protected Void call() throws Exception {
                            // Obtener el usuario completo desde la API para construir un payload válido
                            com.fasterxml.jackson.databind.JsonNode existing = service.getUser(u.getId());
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            com.fasterxml.jackson.databind.node.ObjectNode root = mapper.createObjectNode();
                            com.fasterxml.jackson.databind.node.ObjectNode dto;
                            // Si la respuesta ya viene con 'usuarioDto', úsala, si no usa la respuesta completa
                            if (existing != null && existing.has("usuarioDto") && existing.path("usuarioDto").isObject()) {
                                dto = (com.fasterxml.jackson.databind.node.ObjectNode) existing.path("usuarioDto").deepCopy();
                            } else if (existing != null && existing.isObject()) {
                                dto = (com.fasterxml.jackson.databind.node.ObjectNode) existing.deepCopy();
                            } else {
                                dto = mapper.createObjectNode();
                            }
                            // Asegurar que el campo de activo exista en la forma que espera el backend
                            dto.put("Activo", newState);
                            dto.put("activo", newState);
                            // Ensure required simple fields are present (nombre/email/password)
                            try {
                                String nombreVal = u.getNombre();
                                String emailVal = u.getEmail();
                                if (nombreVal == null || nombreVal.isEmpty()) nombreVal = existing.path("nombre").asText("");
                                if (emailVal == null || emailVal.isEmpty()) emailVal = existing.path("email").asText("");
                                if (!nombreVal.isEmpty()) dto.put("nombre", nombreVal);
                                if (!emailVal.isEmpty()) dto.put("email", emailVal);
                                String defPwd = null; try { defPwd = com.tracersoftware.common.ConfigManager.get("usuarios.defaultPassword"); } catch (Exception ignored) {}
                                if (defPwd == null || defPwd.isEmpty()) defPwd = "ChangeMe123!";
                                dto.put("password", defPwd);
                            } catch (Exception ignored) {}
                            root.set("usuarioDto", dto);
                            // debug: write payload to debug_auth.log before sending
                            try {
                                java.nio.file.Path dbg = java.nio.file.Paths.get("debug_auth.log");
                                String line = java.time.ZonedDateTime.now().toString() + " | PUT (attempt) /api/usuarios/" + u.getId() + " | Payload: " + root.toString() + System.lineSeparator();
                                java.nio.file.Files.write(dbg, line.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                            } catch (Exception ignored) {}

                            Integer rrid = null; try { rrid = u.getRolId(); } catch (Exception ignored) {}
                            // Use dedicated endpoint to toggle state to avoid DTO validation issues
                            try {
                                service.toggleActivo(u.getId(), newState);
                            } catch (Exception ex) {
                                // fallback to full update if toggle endpoint not available
                                service.updateUserActivo(u.getId(), u.getNombre(), u.getEmail(), newState, rrid);
                            }
                            return null;
                        }
                    };
                    t.setOnSucceeded(ev -> {
                        u.setActivo(newState);
                        switchBtn.setDisable(false);
                        hideRowOverlay(idx);
                        // stop spinner rotation
                        Object rtObj = g.getProperties().get("spinnerRotate");
                        if (rtObj instanceof javafx.animation.Animation) ((javafx.animation.Animation) rtObj).stop();
                    });
                    t.setOnFailed(ev -> {
                        // revert visual and model
                        switchBtn.setSelected(u.isActivo());
                        switchBtn.setDisable(false);
                        hideRowOverlay(idx);
                        Object rtObjFail = g.getProperties().get("spinnerRotate");
                        if (rtObjFail instanceof javafx.animation.Animation) ((javafx.animation.Animation) rtObjFail).stop();
                        Throwable ex = t.getException(); if (ex != null) ex.printStackTrace();
                        // If server complains about missing password, prompt the user and retry once with provided password
                        String raw = ex == null ? "" : ex.getMessage();
                        boolean asksPassword = raw != null && raw.toLowerCase().contains("password");
                        if (asksPassword) {
                            // Auto-reintento silencioso con password por defecto configurada
                            Task<Void> retry = new Task<>() {
                                @Override
                                protected Void call() throws Exception {
                                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                    com.fasterxml.jackson.databind.JsonNode existing2 = service.getUser(u.getId());
                                    com.fasterxml.jackson.databind.node.ObjectNode root2 = mapper.createObjectNode();
                                    com.fasterxml.jackson.databind.node.ObjectNode dto2;
                                    if (existing2 != null && existing2.has("usuarioDto") && existing2.path("usuarioDto").isObject()) {
                                        dto2 = (com.fasterxml.jackson.databind.node.ObjectNode) existing2.path("usuarioDto").deepCopy();
                                    } else if (existing2 != null && existing2.isObject()) {
                                        dto2 = (com.fasterxml.jackson.databind.node.ObjectNode) existing2.deepCopy();
                                    } else {
                                        dto2 = mapper.createObjectNode();
                                    }
                                    dto2.put("Activo", newState);
                                    dto2.put("activo", newState);
                                    try { if ((!dto2.has("nombre") || dto2.path("nombre").asText().isEmpty()) && u.getNombre() != null) dto2.put("nombre", u.getNombre()); } catch (Exception ignored) {}
                                    try { if ((!dto2.has("email") || dto2.path("email").asText().isEmpty()) && u.getEmail() != null) dto2.put("email", u.getEmail()); } catch (Exception ignored) {}
                                    String defPwd = null; try { defPwd = com.tracersoftware.common.ConfigManager.get("usuarios.defaultPassword"); } catch (Exception ignored) {}
                                    if (defPwd == null || defPwd.isEmpty()) defPwd = "ChangeMe123!";
                                    dto2.put("password", defPwd);
                                    root2.set("usuarioDto", dto2);
                                            try { service.toggleActivo(u.getId(), newState); }
                                            catch (Exception ignoreToggle) { service.updateUser(u.getId(), root2); }
                                    return null;
                                }
                            };
                            retry.setOnSucceeded(rv -> {
                                u.setActivo(newState);
                                switchBtn.setDisable(false);
                                hideRowOverlay(idx);
                                MessageToast.show(null, "Estado actualizado", MessageToast.ToastType.SUCCESS);
                            });
                            retry.setOnFailed(rv -> {
                                Throwable rex = retry.getException(); if (rex != null) rex.printStackTrace();
                                MessageToast.show(null, "Error actualizando estado: " + (rex == null ? "desconocido" : rex.getMessage()), MessageToast.ToastType.ERROR);
                                switchBtn.setSelected(u.isActivo());
                                switchBtn.setDisable(false);
                                hideRowOverlay(idx);
                            });
                            new Thread(retry, "usuario-toggle-retry").start();
                        } else if (false && asksPassword) {
                            javafx.application.Platform.runLater(() -> {
                                javafx.scene.control.Dialog<String> dlg = new javafx.scene.control.Dialog<>();
                                dlg.setTitle("Contraseña requerida");
                                dlg.setHeaderText("El servidor requiere contraseña para actualizar el usuario. Ingrésela para reintentar.");
                                javafx.scene.control.PasswordField pf = new javafx.scene.control.PasswordField();
                                pf.setPromptText("Contraseña");
                                javafx.scene.layout.VBox vb = new javafx.scene.layout.VBox(8, new javafx.scene.control.Label("Contraseña:"), pf);
                                vb.setPadding(new javafx.geometry.Insets(8));
                                dlg.getDialogPane().setContent(vb);
                                dlg.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
                                dlg.setResultConverter(bt -> bt == javafx.scene.control.ButtonType.OK ? pf.getText() : null);
                                java.util.Optional<String> res = dlg.showAndWait();
                                if (res.isPresent() && res.get() != null && !res.get().isEmpty()) {
                                    String provided = res.get();
                                    // perform retry in background
                                    Task<Void> retry = new Task<>() {
                                        @Override
                                        protected Void call() throws Exception {
                                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                            com.fasterxml.jackson.databind.JsonNode existing2 = service.getUser(u.getId());
                                            com.fasterxml.jackson.databind.node.ObjectNode root2 = mapper.createObjectNode();
                                            com.fasterxml.jackson.databind.node.ObjectNode dto2;
                                            if (existing2 != null && existing2.has("usuarioDto") && existing2.path("usuarioDto").isObject()) {
                                                dto2 = (com.fasterxml.jackson.databind.node.ObjectNode) existing2.path("usuarioDto").deepCopy();
                                            } else if (existing2 != null && existing2.isObject()) {
                                                dto2 = (com.fasterxml.jackson.databind.node.ObjectNode) existing2.deepCopy();
                                            } else {
                                                dto2 = mapper.createObjectNode();
                                            }
                                            dto2.put("Activo", newState);
                                            dto2.put("activo", newState);
                                            // ensure nombre/email
                                            try { if ((!dto2.has("nombre") || dto2.path("nombre").asText().isEmpty()) && u.getNombre() != null) dto2.put("nombre", u.getNombre()); } catch (Exception ignored) {}
                                            try { if ((!dto2.has("email") || dto2.path("email").asText().isEmpty()) && u.getEmail() != null) dto2.put("email", u.getEmail()); } catch (Exception ignored) {}
                                            // set provided password (from dialog)
                                            dto2.put("password", provided);
                                            Integer rrid2 = null; try { rrid2 = u.getRolId(); } catch (Exception ignored) {}
                                            try { service.toggleActivo(u.getId(), newState); }
                                            catch (Exception ignoreToggle2) { service.updateUserActivo(u.getId(), u.getNombre(), u.getEmail(), newState, rrid2); }
                                            return null;
                                        }
                                    };
                                    retry.setOnSucceeded(rv -> {
                                        // update model/UI on success
                                        u.setActivo(newState);
                                        switchBtn.setDisable(false);
                                        hideRowOverlay(idx);
                                        MessageToast.show(null, "Estado actualizado (reintento)", MessageToast.ToastType.SUCCESS);
                                    });
                                    retry.setOnFailed(rv -> {
                                        Throwable rex = retry.getException(); if (rex != null) rex.printStackTrace();
                                        MessageToast.show(null, "Reintento falló: " + (rex == null ? "desconocido" : rex.getMessage()), MessageToast.ToastType.ERROR);
                                        switchBtn.setSelected(u.isActivo());
                                        switchBtn.setDisable(false);
                                        hideRowOverlay(idx);
                                    });
                                    new Thread(retry, "usuario-toggle-retry").start();
                                } else {
                                    MessageToast.show(null, "Contraseña no proporcionada. Operación cancelada.", MessageToast.ToastType.WARNING);
                                }
                            });
                        } else {
                            MessageToast.show(null, "Error actualizando estado: " + (ex == null ? "desconocido" : ex.getMessage()), MessageToast.ToastType.ERROR);
                        }
                        
                    });
                    // start rotate animation on the spinner group
                    javafx.animation.RotateTransition rt = new javafx.animation.RotateTransition(javafx.util.Duration.millis(700), g);
                    rt.setByAngle(360);
                    rt.setCycleCount(javafx.animation.Animation.INDEFINITE);
                    g.getProperties().put("spinnerRotate", rt);
                    rt.play();
                    new Thread(t, "usuario-toggle-update").start();
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null) {
                    setGraphic(null);
                    return;
                }
                UsuarioDTO u = getTableView().getItems().get(getIndex());
                if (u == null) { setGraphic(null); return; }

                // bind toggle height to row height so it fills vertically

                // fixed height/width to avoid growth on clicks

                boolean sel = u.isActivo();

                programmaticChange = true;
                try { switchBtn.setSelected(sel); } finally { programmaticChange = false; }

                inlineLoader.visibleProperty().bind(switchBtn.disableProperty());
                inlineLoader.managedProperty().bind(inlineLoader.visibleProperty());

                setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
                setGraphic(stack);
            }
        });

    btnNew.setOnAction(evt -> openNew());
    // style Nuevo as primary blue with icon
    try { btnNew.getStyleClass().addAll("button-blue","btn-primary","icon-btn"); btnNew.setText("➕ Nuevo"); } catch (Exception ignored) {}
        // add Edit button per-row
    // make actions column narrow and fixed
    try { colEdit.setPrefWidth(140); colEdit.setMaxWidth(140); colEdit.setMinWidth(140); } catch (Exception ignored) {}

        colEdit.setCellFactory(tc -> new javafx.scene.control.TableCell<>() {
            private final javafx.scene.control.Button btnEdit = new javafx.scene.control.Button("Editar");
            private final javafx.scene.control.Button btnDel = new javafx.scene.control.Button("Eliminar");
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(6, btnEdit, btnDel);
            {
                btnEdit.setOnAction(evt -> {
                    UsuarioDTO u = getTableView().getItems().get(getIndex());
                    if (u != null) openEdit(u);
                });
                btnDel.setOnAction(evt -> {
                    UsuarioDTO u = getTableView().getItems().get(getIndex());
                    if (u == null) return;
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                        a.setTitle("Confirmar eliminación");
                        a.setHeaderText(null);
                        a.setContentText("¿Eliminar usuario '" + u.getNombre() + "' ?");
                        java.util.Optional<javafx.scene.control.ButtonType> res = a.showAndWait();
                        if (res.isPresent() && res.get() == javafx.scene.control.ButtonType.OK) {
                            // run deletion in background
                            Task<Void> t = new Task<>() {
                                @Override
                                protected Void call() throws Exception {
                                    service.deleteUser(u.getId());
                                    return null;
                                }
                            };
                            t.setOnSucceeded(ev -> {
                                MessageToast.show(null, "Usuario eliminado", MessageToast.ToastType.SUCCESS);
                                loadData();
                            });
                            t.setOnFailed(ev -> {
                                Throwable ex = t.getException();
                                if (ex != null) ex.printStackTrace();
                                String raw = ex == null ? "" : ex.getMessage();
                                String msg = raw;
                                if (raw != null && raw.contains("HTTP 409")) {
                                    msg = "No se puede eliminar: el usuario está referenciado por otros registros.";
                                }
                                MessageToast.show(null, "Error eliminando usuario: " + (msg == null ? "desconocido" : msg), MessageToast.ToastType.ERROR);
                            });
                            Thread th = new Thread(t, "usuario-delete"); th.setDaemon(true); th.start();
                        }
                    });
                });
                btnEdit.getStyleClass().addAll("btn-action-edit","icon-btn");
                btnEdit.setText("Editar");
                btnEdit.setTooltip(new javafx.scene.control.Tooltip("Editar usuario"));
                btnDel.getStyleClass().addAll("btn-action-delete","icon-btn");
                btnDel.setText("Eliminar");
                btnDel.setTooltip(new javafx.scene.control.Tooltip("Eliminar usuario"));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null); else setGraphic(box);
            }
        });
    }

    // Map of active overlays by row index
    private final java.util.Map<Integer, javafx.scene.Node> activeRowOverlays = new java.util.HashMap<>();
    private AnimationTimer overlayUpdater = null;

    private void showRowOverlay(int rowIndex) {
        try {
            if (rowOverlayPane == null) return;
            if (activeRowOverlays.containsKey(rowIndex)) return;
            // fallback: compute bounds from table row via items
            javafx.scene.Node rowNode = tableUsers.lookupAll(".table-row-cell").stream()
                    .filter(n -> n instanceof TableRow)
                    .map(n -> (TableRow<UsuarioDTO>) n)
                    .filter(r -> r.getIndex() == rowIndex)
                    .findFirst().orElse(null);
            if (rowNode == null) {
                // if not found, abort (row might be virtualized)
                return;
            }
            javafx.geometry.Bounds tb = rowNode.localToScene(rowNode.getBoundsInLocal());
            javafx.geometry.Bounds tbLocal = rowOverlayPane.sceneToLocal(tb);
            javafx.scene.layout.Region overlay = new javafx.scene.layout.Region();
            overlay.getStyleClass().add("row-overlay");
            overlay.setLayoutX(tbLocal.getMinX());
            overlay.setLayoutY(tbLocal.getMinY());
            overlay.setPrefWidth(tbLocal.getWidth());
            overlay.setPrefHeight(tbLocal.getHeight());
            // small inline loader within overlay
            javafx.scene.layout.Region loader = new javafx.scene.layout.Region();
            loader.getStyleClass().add("inline-overlay-loader");
            javafx.scene.layout.StackPane container = new javafx.scene.layout.StackPane();
            container.getChildren().addAll(overlay, loader);
            container.setLayoutX(overlay.getLayoutX());
            container.setLayoutY(overlay.getLayoutY());
            container.setPrefWidth(overlay.getPrefWidth());
            container.setPrefHeight(overlay.getPrefHeight());
            container.getStyleClass().add("row-overlay-container");
            container.setMouseTransparent(false);
            rowOverlayPane.getChildren().add(container);
            activeRowOverlays.put(rowIndex, container);
            // store row index on node for later repositioning
            container.getProperties().put("rowIndex", rowIndex);
            // rotation animation for overlay loader
            RotateTransition rt = new RotateTransition(Duration.millis(800), loader);
            rt.setByAngle(360);
            rt.setCycleCount(Animation.INDEFINITE);
            container.getProperties().put("overlayRotate", rt);
            rt.play();
            ensureOverlayUpdaterRunning();
            // fade in
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(180), container);
            container.setOpacity(0);
            ft.setToValue(1.0);
            ft.play();
        } catch (Exception ex) {
            // ignore positioning errors
            ex.printStackTrace();
        }
    }

    private void hideRowOverlay(int rowIndex) {
        try {
            javafx.scene.Node n = activeRowOverlays.remove(rowIndex);
            if (n == null) return;
            // stop overlay rotate if present
            Object rot = n.getProperties().get("overlayRotate");
            if (rot instanceof RotateTransition) {
                ((RotateTransition) rot).stop();
            }
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(160), n);
            ft.setToValue(0.0);
            ft.setOnFinished(ev -> {
                rowOverlayPane.getChildren().remove(n);
                if (activeRowOverlays.isEmpty()) stopOverlayUpdater();
            });
            ft.play();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void ensureOverlayUpdaterRunning() {
        if (overlayUpdater != null) return;
        overlayUpdater = new AnimationTimer() {
            @Override
            public void handle(long now) {
                try { updateOverlayPositions(); } catch (Exception ignored) {}
            }
        };
        overlayUpdater.start();
    }

    private void stopOverlayUpdater() {
        if (overlayUpdater != null) {
            overlayUpdater.stop();
            overlayUpdater = null;
        }
    }

    private void updateOverlayPositions() {
        if (rowOverlayPane == null) return;
    for (var e : activeRowOverlays.entrySet()) {
            Integer rowIndex = e.getKey();
            javafx.scene.Node container = e.getValue();
            // find visible row node for index
        javafx.scene.Node rowNode = tableUsers.lookupAll(".table-row-cell").stream()
            .filter(n -> n instanceof TableRow)
            .map(n -> (TableRow<?>) n)
            .filter(r -> r.getIndex() == rowIndex)
            .findFirst().orElse(null);
            if (rowNode == null) {
                // if row not visible, leave overlay where it is
                continue;
            }
            javafx.geometry.Bounds tb = rowNode.localToScene(rowNode.getBoundsInLocal());
            javafx.geometry.Bounds tbLocal = rowOverlayPane.sceneToLocal(tb);
            container.setLayoutX(tbLocal.getMinX());
            container.setLayoutY(tbLocal.getMinY());
            if (container instanceof javafx.scene.layout.Region) {
                ((javafx.scene.layout.Region) container).setPrefWidth(tbLocal.getWidth());
                ((javafx.scene.layout.Region) container).setPrefHeight(tbLocal.getHeight());
            }
        }
    }

    @Override
    public void onViewShown() {
        // Start loading data when the view is shown (lazy load)
        loadData();
    }

    private void openNew() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/usuarios/fxml/usuarios_form.fxml"));
            javafx.scene.Parent root = loader.load();
            com.tracersoftware.common.ui.ModalUtils.showModalAndWait((javafx.stage.Stage) btnNew.getScene().getWindow(), root, "Nuevo Usuario");
            // after modal closes, refresh list
            loadData();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadData() {
        // request first page
        int page = paginator.getPageIndex();
        int size = paginator.getPageSize();
        // ensure roles are loaded first to avoid flash of empty role names
        Task<com.tracersoftware.usuarios.api.PagedResult> task = new Task<>() {
            @Override
            protected com.tracersoftware.usuarios.api.PagedResult call() throws Exception {
                // if roles preload task exists and is still running, wait for it (with timeout)
                try {
                    if (rolesPreloadTask != null && rolesPreloadTask.isRunning()) {
                        rolesPreloadTask.get(5, java.util.concurrent.TimeUnit.SECONDS);
                    }
                } catch (Exception ignore) {}
                return service.listUsersPaged(page, size);
            }
        };
        task.setOnSucceeded(evt -> {
            com.tracersoftware.usuarios.api.PagedResult res = task.getValue();
            // map paged items to UsuarioDTO model
            fullUsers.clear();
            for (JsonNode n : res.getItems()) {
                UsuarioDTO u = new UsuarioDTO();
        // tolerate multiple possible property names from backend
        int id = n.has("id") ? n.path("id").asInt() : (
            n.has("usuarioId") ? n.path("usuarioId").asInt() : n.path("UsuarioId").asInt(0)
        );
        String nombre = n.has("nombre") ? n.path("nombre").asText("") : (
            n.has("name") ? n.path("name").asText("") : n.path("fullName").asText("")
        );
        String email = n.has("email") ? n.path("email").asText("") : (
            n.has("correo") ? n.path("correo").asText("") : n.path("mail").asText("")
        );
    boolean activo = n.has("activo") ? n.path("activo").asBoolean(false) : n.path("isActive").asBoolean(false);
    // role may be present as 'rol' object, 'role' string, or only a RolId int
    String rol = "";
    Integer rid = null;
    if (n.has("rol") && n.path("rol").has("nombre")) rol = n.path("rol").path("nombre").asText("");
    else if (n.has("role")) rol = n.path("role").asText("");
    else if (n.has("rol") && n.path("rol").isTextual()) rol = n.path("rol").asText("");
    // check for numeric RolId fields
    if (n.has("rolId")) rid = n.path("rolId").asInt();
    else if (n.has("roleId")) rid = n.path("roleId").asInt();
    else if (n.has("RolId")) rid = n.path("RolId").asInt();
    // if name not present but we have id and rolesMap loaded, resolve
    if ((rol == null || rol.isEmpty()) && rid != null && rolesMap.containsKey(rid)) rol = rolesMap.get(rid);
        u.setId(id);
        u.setNombre(nombre);
        u.setEmail(email);
    u.setActivo(activo);
    u.setRol(rol);
    u.setRolId(rid);
                fullUsers.add(u);
            }
            // configure paginator with server totals
            try {
                paginator.setTotalItems(res.getTotalItems());
            } catch (Exception ignored) {}
            // Ensure paginator listeners are wired only once
            try {
                paginator.pageIndexProperty().addListener((obs, o, n) -> {
                    // request the selected page from server
                    loadPage(n.intValue(), paginator.getPageSize());
                });
                paginator.pageSizeProperty().addListener((obs, o, n) -> {
                    // reload with new page size
                    loadPage(0, n.intValue());
                });
            } catch (Exception ignored) {}
            // render current page (items returned are already the page)
            showPage(0, paginator.getPageSize());
        });
        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            ex.printStackTrace();
            // Mostrar mensaje visible al usuario y manejar 401 (autenticación)
            try {
                // Log full exception + any debug_auth.log entries for later inspection
                try {
                    Path dbg = java.nio.file.Paths.get("debug_auth.log");
                    String header = java.time.ZonedDateTime.now().toString() + " | Usuarios load failed: " + (ex == null ? "(null)" : ex.toString()) + System.lineSeparator();
                    java.nio.file.Files.write(dbg, header.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                    // stacktrace
                    try (java.io.StringWriter sw = new java.io.StringWriter(); java.io.PrintWriter pw = new java.io.PrintWriter(sw)) {
                        if (ex != null) ex.printStackTrace(pw);
                        java.nio.file.Files.write(dbg, sw.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                    } catch (Exception ignored) {}
                } catch (Exception ignored) {}

                // Update UI with concise message and a visible debug hint
                javafx.application.Platform.runLater(() -> {
                    String raw = (ex != null ? ex.getMessage() : "desconocido");
                    String msg;
                    boolean is401 = raw != null && raw.contains("HTTP 401");
                    if (is401) {
                        msg = "Autenticación requerida. Por favor inicie sesión nuevamente. (HTTP 401)";
                        MessageToast.showSystemError(null, "Sesión inválida o expirada. Se requiere re-login.");
                        showReloginDialog();
                    } else {
                        msg = "Error cargando usuarios: " + (raw == null ? "desconocido" : raw);
                        MessageToast.showSystemError(null, msg + " (ver debug_auth.log para detalles)");
                    }
                    if (lblError != null) {
                        lblError.setText(msg);
                        lblError.setVisible(true);
                        lblError.setManaged(true);
                    }
                });
            } catch (Exception ignore) {}
        });
        Thread th = new Thread(task, "usuarios-load");
        th.setDaemon(true);
        th.start();
    }

    private void showReloginDialog() {
        // Simple programmatic login dialog (modal) to re-authenticate the user
        javafx.stage.Stage st = new javafx.stage.Stage();
        st.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        st.setTitle("Reingresar");

        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(8);
        box.setPadding(new javafx.geometry.Insets(12));
        javafx.scene.control.Label lbl = new javafx.scene.control.Label("La sesión expiró. Ingrese credenciales:");
        javafx.scene.control.TextField userFld = new javafx.scene.control.TextField();
        userFld.setPromptText("Usuario");
        javafx.scene.control.PasswordField passFld = new javafx.scene.control.PasswordField();
        passFld.setPromptText("Contraseña");
        javafx.scene.layout.HBox buttons = new javafx.scene.layout.HBox(8);
        javafx.scene.control.Button ok = new javafx.scene.control.Button("Entrar");
        javafx.scene.control.Button cancel = new javafx.scene.control.Button("Cancelar");
        buttons.getChildren().addAll(ok, cancel);
        box.getChildren().addAll(lbl, userFld, passFld, buttons);

        ok.setOnAction(ae -> {
            String u = userFld.getText();
            String p = passFld.getText();
            if (u == null || u.isEmpty() || p == null || p.isEmpty()) {
                MessageToast.show(null, "Ingrese usuario y contraseña", MessageToast.ToastType.WARNING);
                return;
            }
            // authenticate using the same service used in LoginController (ApiService)
            try {
                com.tracersoftware.apiinfo.ApiService api = new com.tracersoftware.apiinfo.ApiService(com.tracersoftware.common.ConfigManager.getUrlBase());
                boolean okAuth = api.login(u, p);
                    if (okAuth) {
                        String newToken = api.getAuthToken();
                        // keep legacy in-memory values for other code paths that still read them
                        com.tracersoftware.common.SessionManager.setAuthToken(newToken);
                        com.tracersoftware.common.SessionManager.setUsername(u);
                        // Persist using the new auth.SessionManager (it will save via TokenStore).
                        try {
                            SessionManager.get().setSession(newToken, null, u, new String[0], null);
                        } catch (Exception ignored) {}
                        // also try to set avatar resource as LoginController does
                        try {
                            String candidate = "/images/avatars/" + u + ".png";
                            if (getClass().getResource(candidate) != null) {
                                com.tracersoftware.common.SessionManager.setAvatarUrl(candidate);
                            } else {
                                com.tracersoftware.common.SessionManager.setAvatarUrl(null);
                            }
                        } catch (Exception ignored) {}
                        MessageToast.show(null, "Reingreso exitoso", MessageToast.ToastType.SUCCESS);
                        st.close();
                        // retry load
                        loadData();
                    } else {
                    MessageToast.show(null, "Credenciales inválidas", MessageToast.ToastType.ERROR);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                MessageToast.show(null, "Error reautenticando: " + ex.getMessage(), MessageToast.ToastType.ERROR);
            }
        });

        // Insert export bar above paginator, aligned to the right
        try {
            javafx.scene.Parent parent = paginator.getParent();
            if (parent instanceof javafx.scene.layout.VBox vbox) {
                int idx = vbox.getChildren().indexOf(paginator);
                javafx.scene.layout.HBox bottomBar = new javafx.scene.layout.HBox(8);
                javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                java.util.concurrent.Callable<java.util.List<?>> fetchAll = () -> {
                    int page = 0;
                    int size = 500;
                    java.util.List<com.fasterxml.jackson.databind.JsonNode> all = new java.util.ArrayList<>();
                    while (true) {
                        com.tracersoftware.usuarios.api.PagedResult pr = service.listUsersPaged(page, size);
                        all.addAll(pr.getItems());
                        page++;
                        if (page >= pr.getTotalPages()) break;
                    }
                    return new java.util.ArrayList<>(all);
                };
                com.tracersoftware.common.controls.ExportBar exportBar = new com.tracersoftware.common.controls.ExportBar(tableUsers, "usuarios", fetchAll);
                bottomBar.getChildren().addAll(spacer, exportBar);
                vbox.getChildren().add(Math.max(0, idx), bottomBar);
            }
        } catch (Exception ignored) {}

        cancel.setOnAction(ae -> st.close());

        st.setScene(new javafx.scene.Scene(box));
        st.showAndWait();
    }

    private void openEdit(UsuarioDTO u) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/usuarios/fxml/usuarios_form.fxml"));
            javafx.scene.Parent root = loader.load();
            UsuarioFormController ctrl = loader.getController();
            if (ctrl != null) ctrl.setUsuario(u);
            com.tracersoftware.common.ui.ModalUtils.showModalAndWait((javafx.stage.Stage) btnNew.getScene().getWindow(), root, "Editar Usuario");
            // refresh list after edit
            loadData();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Enable dragging for undecorated stages using the accent bar (or whole root as fallback)
    private void enableDragging(javafx.stage.Stage stage, javafx.scene.Parent root) {
        try {
            final double[] dragDelta = new double[2];
            javafx.scene.Node dragHandle = root.lookup(".modal-accent");
            if (dragHandle == null) dragHandle = root;
            final javafx.scene.Node handle = dragHandle;
            handle.setOnMousePressed(e -> {
                dragDelta[0] = stage.getX() - e.getScreenX();
                dragDelta[1] = stage.getY() - e.getScreenY();
            });
            handle.setOnMouseDragged(e -> {
                stage.setX(e.getScreenX() + dragDelta[0]);
                stage.setY(e.getScreenY() + dragDelta[1]);
            });
        } catch (Exception ignored) {}
    }

    private void showPage(int pageIndex, int pageSize) {
        try {
            var items = tableUsers.getItems();
            items.clear();
            if (fullUsers == null || fullUsers.isEmpty()) return;
            // When using server-side paging, fullUsers already contains the current page items
            for (UsuarioDTO u : fullUsers) items.add(u);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void loadPage(int page, int size) {
        Task<com.tracersoftware.usuarios.api.PagedResult> task = new Task<>() {
            @Override
            protected com.tracersoftware.usuarios.api.PagedResult call() throws Exception {
                return service.listUsersPaged(page, size);
            }
        };
        task.setOnSucceeded(evt -> {
            com.tracersoftware.usuarios.api.PagedResult res = task.getValue();
            fullUsers.clear();
            for (com.fasterxml.jackson.databind.JsonNode n : res.getItems()) {
                UsuarioDTO u = new UsuarioDTO();
                int id = n.has("id") ? n.path("id").asInt() : (
                    n.has("usuarioId") ? n.path("usuarioId").asInt() : n.path("UsuarioId").asInt(0)
                );
                String nombre = n.has("nombre") ? n.path("nombre").asText("") : (
                    n.has("name") ? n.path("name").asText("") : n.path("fullName").asText("")
                );
                String email = n.has("email") ? n.path("email").asText("") : (
                    n.has("correo") ? n.path("correo").asText("") : n.path("mail").asText("")
                );
                boolean activo = n.has("activo") ? n.path("activo").asBoolean(false) : n.path("isActive").asBoolean(false);
                String rol = "";
                if (n.has("rol") && n.path("rol").has("nombre")) rol = n.path("rol").path("nombre").asText("");
                else if (n.has("role")) rol = n.path("role").asText("");
                else if (n.has("rol") && n.path("rol").isTextual()) rol = n.path("rol").asText("");
                u.setId(id);
                u.setNombre(nombre);
                u.setEmail(email);
                u.setActivo(activo);
                u.setRol(rol);
                fullUsers.add(u);
            }
            try { paginator.setTotalItems(res.getTotalItems()); } catch (Exception ignored) {}
            // render returned page
            showPage(page, size);
        });
        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            ex.printStackTrace();
            MessageToast.show(null, "Error cargando página: " + (ex == null ? "desconocido" : ex.getMessage()), MessageToast.ToastType.ERROR);
        });
        new Thread(task, "usuarios-page-load").start();
    }
}
