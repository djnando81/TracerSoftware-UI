package com.tracersoftware.roles.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.tracersoftware.roles.api.RolesApiService;
import com.tracersoftware.roles.model.RolDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class RolesListController implements com.tracersoftware.ui.ViewLifecycle {

    @FXML private TableView<RolDTO> tableRoles;
    @FXML private TableColumn<RolDTO, Integer> colId;
    @FXML private TableColumn<RolDTO, String> colNombre;
    @FXML private TableColumn<RolDTO, String> colPermisos;
    @FXML private TableColumn<RolDTO, Void> colActions;
    @FXML private javafx.scene.control.Button btnNew;
    @FXML private com.tracersoftware.common.controls.PaginatorControl paginator;

    private final RolesApiService service = new RolesApiService();
    private final ObservableList<RolDTO> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        tableRoles.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPermisos.setCellValueFactory(new PropertyValueFactory<>("permisos"));
        tableRoles.setItems(data);
        try { if (colActions != null) { colActions.setMinWidth(160); colActions.setPrefWidth(180); colActions.setMaxWidth(220); } } catch (Exception ignored) {}
        wireActionsColumn();

        // Style "Nuevo" botón
        try { btnNew.getStyleClass().addAll("button-blue","btn-small","icon-btn"); } catch (Exception ignored) {}
        btnNew.setOnAction(evt -> openNew());

        // Add ExportBar above the table (simple layout assumption: parent VBox)
        try {
            javafx.scene.Parent parent = tableRoles.getParent();
            while (parent != null && !(parent instanceof javafx.scene.layout.VBox)) parent = parent.getParent();
            if (parent instanceof javafx.scene.layout.VBox vbox) {
                com.tracersoftware.common.controls.ExportBar exportBar = new com.tracersoftware.common.controls.ExportBar(tableRoles, "roles", () -> {
                    JsonNode arr = service.listAll();
                    java.util.List<JsonNode> list = new java.util.ArrayList<>();
                    if (arr != null && arr.isArray()) arr.forEach(list::add);
                    return list;
                });
                javafx.scene.layout.HBox hb = new javafx.scene.layout.HBox(8);
                javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                hb.getChildren().addAll(spacer, exportBar);
                vbox.getChildren().add(1, hb); // after title row
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onViewShown() {
        loadPage(0, paginator != null ? paginator.getPageSize() : 10);
    }

    private void wireActionsColumn() {
        colActions.setCellFactory(tc -> new javafx.scene.control.TableCell<>() {
            private final javafx.scene.control.Button btnEdit = new javafx.scene.control.Button("Editar");
            private final javafx.scene.control.Button btnDel = new javafx.scene.control.Button("Eliminar");
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(6, btnEdit, btnDel);
            {
                try { btnEdit.getStyleClass().addAll("btn-small","btn-warning","icon-btn"); } catch (Exception ignored) {}
                try { btnDel.getStyleClass().addAll("btn-small","btn-danger","icon-btn"); } catch (Exception ignored) {}
                btnEdit.setOnAction(evt -> {
                    RolDTO r = getTableView().getItems().get(getIndex());
                    if (r != null) openEdit(r);
                });
                btnDel.setOnAction(evt -> {
                    RolDTO r = getTableView().getItems().get(getIndex());
                    if (r == null) return;
                    javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                    a.setTitle("Confirmar eliminación");
                    a.setHeaderText(null);
                    a.setContentText("¿Eliminar rol '" + r.getNombre() + "' ?");
                    var res = a.showAndWait();
                    if (res.isPresent() && res.get() == javafx.scene.control.ButtonType.OK) {
                        Task<Void> t = new Task<>() {
                            @Override protected Void call() throws Exception { service.delete(r.getId()); return null; }
                        };
                        t.setOnSucceeded(ev -> { com.tracersoftware.common.controls.MessageToast.show(null, "Rol eliminado", com.tracersoftware.common.controls.MessageToast.ToastType.SUCCESS); loadPage(paginator.getPageIndex(), paginator.getPageSize()); });
                        t.setOnFailed(ev -> { Throwable ex = t.getException(); if (ex != null) ex.printStackTrace(); com.tracersoftware.common.controls.MessageToast.show(null, "Error eliminando: " + (t.getException()==null?"":t.getException().getMessage()), com.tracersoftware.common.controls.MessageToast.ToastType.ERROR); });
                        new Thread(t, "rol-del").start();
                    }
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private java.util.List<RolDTO> fullData = new java.util.ArrayList<>();

    private void loadPage(int page, int size) {
        Task<Void> t = new Task<>() {
            @Override protected Void call() throws Exception {
                JsonNode arr = service.listAll();
                fullData.clear();
                if (arr != null && arr.isArray()) {
                    for (JsonNode n : arr) {
                        RolDTO r = new RolDTO();
                        int id = n.has("rolId") ? n.path("rolId").asInt() : n.path("RolId").asInt(0);
                        r.setId(id);
                        r.setNombre(n.path("nombre").asText(""));
                        r.setPermisos(n.path("permisos").asText(""));
                        fullData.add(r);
                    }
                }
                javafx.application.Platform.runLater(() -> {
                    int total = fullData.size();
                    int from = Math.max(0, Math.min(page * size, total));
                    int to = Math.max(from, Math.min(from + size, total));
                    data.clear();
                    data.addAll(fullData.subList(from, to));
                    try { paginator.setTotalItems(total); } catch (Exception ignored) {}
                });
                return null;
            }
        };
        t.setOnSucceeded(ev -> {});
        new Thread(t, "roles-load").start();
    }

    private void openNew() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/roles/fxml/roles_form.fxml"));
            javafx.scene.Parent root = loader.load();
            com.tracersoftware.common.ui.ModalUtils.showModalAndWait((javafx.stage.Stage) btnNew.getScene().getWindow(), root, "Nuevo Rol");
            loadPage(paginator.getPageIndex(), paginator.getPageSize());
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void openEdit(RolDTO r) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/roles/fxml/roles_form.fxml"));
            javafx.scene.Parent root = loader.load();
            var ctrl = (com.tracersoftware.roles.controller.RolFormController) loader.getController();
            if (ctrl != null) ctrl.setRol(r);
            com.tracersoftware.common.ui.ModalUtils.showModalAndWait((javafx.stage.Stage) btnNew.getScene().getWindow(), root, "Editar Rol");
            loadPage(paginator.getPageIndex(), paginator.getPageSize());
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}
