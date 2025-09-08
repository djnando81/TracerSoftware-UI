package com.tracersoftware.materiasprimas.controller;

import com.tracersoftware.common.controls.MessageToast;
import com.tracersoftware.materiasprimas.api.MateriasPrimasApiService;
import com.tracersoftware.materiasprimas.model.MateriaPrimaItem;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class MateriasPrimasListController implements com.tracersoftware.ui.ViewLifecycle {

    @FXML private TableView<MateriaPrimaItem> table;
    @FXML private TableColumn<MateriaPrimaItem,Integer> colId;
    @FXML private TableColumn<MateriaPrimaItem,String> colNombre;
    @FXML private TableColumn<MateriaPrimaItem,String> colCodigoInterno;
    @FXML private TableColumn<MateriaPrimaItem,String> colUnidad;
    @FXML private TableColumn<MateriaPrimaItem,String> colCategoria;
    @FXML private TableColumn<MateriaPrimaItem,String> colOrigen;
    @FXML private TableColumn<MateriaPrimaItem,Boolean> colActiva;
    @FXML private TableColumn<MateriaPrimaItem,Double> colStockActual;
    @FXML private TableColumn<MateriaPrimaItem,Double> colStockMin;
    @FXML private TableColumn<MateriaPrimaItem,Double> colStockMax;
    @FXML private TableColumn<MateriaPrimaItem,Double> colCosto;
    @FXML private TableColumn<MateriaPrimaItem,Void> colAcciones;
    @FXML private com.tracersoftware.common.controls.PaginatorControl paginator;
    @FXML private com.tracersoftware.common.controls.SearchBar searchBar;
    @FXML private Button btnNew;
    @FXML private javafx.scene.layout.HBox exportBarContainer; // OBLIGATORIO

    private final MateriasPrimasApiService service = new MateriasPrimasApiService();
    private final ObservableList<MateriaPrimaItem> data = FXCollections.observableArrayList();
    private final java.util.List<MateriaPrimaItem> fullData = new java.util.ArrayList<>();

    @FXML
    public void initialize() {
        if (table != null) {
            // Cambiar a UNCONSTRAINED_RESIZE_POLICY para permitir scroll horizontal
            try { table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY); } catch (Exception ignored) {}
        }
        
        // Configurar anchos mínimos para las columnas
        if (colId != null) {
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colId.setMinWidth(60);
            colId.setPrefWidth(60);
        }
        if (colNombre != null) {
            colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
            colNombre.setMinWidth(150);
            colNombre.setPrefWidth(200);
        }
        if (colCodigoInterno != null) {
            colCodigoInterno.setCellValueFactory(new PropertyValueFactory<>("codigoInterno"));
            colCodigoInterno.setMinWidth(120);
            colCodigoInterno.setPrefWidth(150);
        }
        if (colUnidad != null) {
            colUnidad.setCellValueFactory(new PropertyValueFactory<>("unidad"));
            colUnidad.setMinWidth(80);
            colUnidad.setPrefWidth(100);
        }
        if (colCategoria != null) {
            colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoriaMateriaPrimaNombre"));
            colCategoria.setMinWidth(120);
            colCategoria.setPrefWidth(150);
        }
        if (colOrigen != null) {
            colOrigen.setCellValueFactory(new PropertyValueFactory<>("materiaPrimaOrigenNombre"));
            colOrigen.setMinWidth(120);
            colOrigen.setPrefWidth(150);
        }
        if (colActiva != null) {
            colActiva.setCellValueFactory(new PropertyValueFactory<>("activa"));
            colActiva.setMinWidth(70);
            colActiva.setPrefWidth(80);
        }
        if (colStockActual != null) {
            colStockActual.setCellValueFactory(new PropertyValueFactory<>("stockActual"));
            colStockActual.setMinWidth(100);
            colStockActual.setPrefWidth(120);
        }
        if (colStockMin != null) {
            colStockMin.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));
            colStockMin.setMinWidth(90);
            colStockMin.setPrefWidth(110);
        }
        if (colStockMax != null) {
            colStockMax.setCellValueFactory(new PropertyValueFactory<>("stockMaximo"));
            colStockMax.setMinWidth(90);
            colStockMax.setPrefWidth(110);
        }
        if (colCosto != null) {
            colCosto.setCellValueFactory(new PropertyValueFactory<>("costoUnitario"));
            colCosto.setMinWidth(120);
            colCosto.setPrefWidth(140);
        }
        // toggle for Activa similar a Usuarios
        if (colActiva != null) {
            colActiva.setCellFactory(tc -> new TableCell<>() {
                private final com.tracersoftware.common.controls.SwitchToggleButton switchBtn = new com.tracersoftware.common.controls.SwitchToggleButton();
                private boolean programmatic = false;
                {   // size
                    switchBtn.setMinWidth(48); switchBtn.setPrefWidth(48); switchBtn.setMaxWidth(48);
                    switchBtn.setMinHeight(26); switchBtn.setPrefHeight(26); switchBtn.setMaxHeight(26);
                    switchBtn.getStyleClass().add("cell-switch-btn");
                    switchBtn.switchedOnProperty().addListener((obs, ov, nv) -> {
                        if (programmatic) return;
                        int idx = getIndex(); if (idx < 0 || idx >= getTableView().getItems().size()) return;
                        MateriaPrimaItem mp = getTableView().getItems().get(idx); if (mp == null) return;
                        boolean newState = Boolean.TRUE.equals(nv);
                        switchBtn.setDisable(true);
                        Task<Void> t = new Task<>() { @Override protected Void call() throws Exception { service.toggleEstado(mp.getId(), newState); return null; } };
                        t.setOnSucceeded(ev -> { mp.setActiva(newState); switchBtn.setDisable(false); });
                        t.setOnFailed(ev -> { switchBtn.setDisable(false); programmatic = true; switchBtn.setSelected(!newState); programmatic = false; MessageToast.showSystemError(null, "No se pudo cambiar estado: " + (t.getException()==null?"":t.getException().getMessage())); });
                        new Thread(t, "mp-toggle").start();
                    });
                }
                @Override protected void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    programmatic = true; switchBtn.setSelected(Boolean.TRUE.equals(item)); programmatic = false;
                    setGraphic(switchBtn);
                }
            });
        }
        if (table != null) table.setItems(data);

        // paginator
        if (paginator != null) {
            paginator.pageIndexProperty().addListener((o,ov,nv) -> applyFilterAndPaginate());
            paginator.pageSizeProperty().addListener((o,ov,nv) -> applyFilterAndPaginate());
        }

        // OBLIGATORIO: Configuración de ExportBar usando el contenedor del FXML
        if (exportBarContainer != null) {
            com.tracersoftware.common.controls.ExportBar exportBar = new com.tracersoftware.common.controls.ExportBar(table, "materiasprimas", () -> service.listAll());
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            exportBarContainer.getChildren().addAll(spacer, exportBar);
        }

        // search
        if (searchBar != null) {
            searchBar.textProperty().addListener((o,ov,nv) -> {
                if (paginator != null) paginator.setPageIndex(0);
                applyFilterAndPaginate();
            });
        }

        // acciones column (editar/eliminar)
        if (colAcciones != null) {
            colAcciones.setMinWidth(150);
            colAcciones.setPrefWidth(170);
            colAcciones.setSortable(false);
            colAcciones.setCellFactory(tc -> new TableCell<>() {
                private final Button btnEdit = new Button("Editar");
                private final Button btnDel = new Button("Eliminar");
                private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(6, btnEdit, btnDel);
                {
                    try { btnEdit.getStyleClass().addAll("btn-small","btn-warning","icon-btn"); } catch (Exception ignored) {}
                    try { btnDel.getStyleClass().addAll("btn-small","btn-danger","icon-btn"); } catch (Exception ignored) {}
                    btnEdit.setOnAction(evt -> {
                        MateriaPrimaItem mp = getTableView().getItems().get(getIndex());
                        if (mp != null) openForm(mp);
                    });
                    btnDel.setOnAction(evt -> {
                        MateriaPrimaItem mp = getTableView().getItems().get(getIndex());
                        if (mp == null) return;
                        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
                        a.setTitle("Confirmar eliminación");
                        a.setHeaderText(null);
                        a.setContentText("¿Eliminar registro #" + mp.getId() + "?");
                        var res = a.showAndWait();
                        if (res.isPresent() && res.get() == ButtonType.OK) {
                            Task<Void> t = new Task<>() { @Override protected Void call() throws Exception { service.delete(mp.getId()); return null; } };
                            t.setOnSucceeded(ev -> { MessageToast.show(null, "Eliminado", MessageToast.ToastType.SUCCESS); reload(); });
                            t.setOnFailed(ev -> MessageToast.showSystemError(null, "Error eliminando: " + (t.getException()==null?"":t.getException().getMessage())));
                            new Thread(t, "mp-del").start();
                        }
                    });
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
        }

        if (btnNew != null) {
            // Estilo estándar del botón "Nuevo" en toda la app
            try { btnNew.getStyleClass().addAll("button-blue","btn-primary","icon-btn"); btnNew.setText("➕ Nuevo"); } catch (Exception ignored) {}
            btnNew.setOnAction(e -> openForm(null));
        }
    }

    @Override
    public void onViewShown() {
        reload();
    }

    private void reload() {
        Task<List<MateriaPrimaItem>> t = new Task<>() {
            @Override protected List<MateriaPrimaItem> call() throws Exception { return service.listAll(); }
        };
        t.setOnSucceeded(ev -> {
            fullData.clear();
            if (t.getValue() != null) fullData.addAll(t.getValue());
            applyFilterAndPaginate();
        });
        t.setOnFailed(ev -> {
            MessageToast.showSystemError(null, "Error cargando materias primas: " + safeMsg(t.getException()));
        });
        new Thread(t, "materiasprimas-load").start();
    }

    private String safeMsg(Throwable ex) { return ex == null ? "" : ex.getMessage(); }

    private void applyFilterAndPaginate() {
        String q = searchBar != null ? (searchBar.getText()==null?"":searchBar.getText().trim().toLowerCase()) : "";
        java.util.List<MateriaPrimaItem> filtered = new java.util.ArrayList<>();
        for (MateriaPrimaItem m : fullData) {
            if (q.isEmpty()) { filtered.add(m); continue; }
            String hay = (safe(m.getNombre())+" "+safe(m.getCodigoInterno())+" "+safe(m.getCategoriaMateriaPrimaNombre())+" "+safe(m.getMateriaPrimaOrigenNombre())).toLowerCase();
            if (hay.contains(q)) filtered.add(m);
        }
        int page = paginator != null ? paginator.getPageIndex() : 0;
        int size = paginator != null ? paginator.getPageSize() : Math.max(1, filtered.size());
        int total = filtered.size();
        int from = Math.max(0, Math.min(page * size, total));
        int to = Math.max(from, Math.min(from + size, total));
        data.setAll(filtered.subList(from, to));
        try { paginator.setTotalItems(total); } catch (Exception ignored) {}
    }
    private String safe(String s) { return s==null?"":s; }

    private void openForm(MateriaPrimaItem item) {
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource("/materiasprimas/fxml/materiasprimas_form.fxml"));
            javafx.scene.Parent n = l.load();
            MateriaPrimaFormController ctrl = l.getController();
            ctrl.setOnSaved(() -> Platform.runLater(this::reload));
            if (item != null) ctrl.edit(item);
            com.tracersoftware.common.ui.ModalUtils.showModalAndWait(
                    (Stage) (btnNew != null ? btnNew.getScene().getWindow() : null),
                    n,
                    item == null ? "Nueva Materia Prima" : "Editar Materia Prima");
        } catch (IOException ex) {
            MessageToast.showSystemError(null, "No se pudo abrir el formulario: " + ex.getMessage());
        }
    }
}
