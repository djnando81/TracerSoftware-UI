package com.tracersoftware.categoriasmateriaprima.controller;

import com.tracersoftware.categoriasmateriaprima.api.CategoriasMateriaPrimaApiService;
import com.tracersoftware.categoriasmateriaprima.model.CategoriaMateriaPrimaItem;
import com.tracersoftware.common.controls.MessageToast;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class CategoriasMateriaPrimaListController implements com.tracersoftware.ui.ViewLifecycle {
    @FXML private TableView<CategoriaMateriaPrimaItem> table;
    @FXML private TableColumn<CategoriaMateriaPrimaItem,Integer> colId;
    @FXML private TableColumn<CategoriaMateriaPrimaItem,String> colNombre;
    @FXML private TableColumn<CategoriaMateriaPrimaItem,Boolean> colActiva;
    @FXML private TableColumn<CategoriaMateriaPrimaItem,Void> colAcciones;
    @FXML private Button btnNuevo;
    @FXML private com.tracersoftware.common.controls.PaginatorControl paginator;
    @FXML private com.tracersoftware.common.controls.SearchBar searchBar;
    @FXML private javafx.scene.layout.HBox exportBarContainer;

    private final CategoriasMateriaPrimaApiService service = new CategoriasMateriaPrimaApiService();
    private final javafx.collections.ObservableList<CategoriaMateriaPrimaItem> data = javafx.collections.FXCollections.observableArrayList();
    private final java.util.List<CategoriaMateriaPrimaItem> full = new java.util.ArrayList<>();

    @FXML public void initialize() {
        if (table != null) {
            table.setItems(data);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        }
        if (colId != null) {
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colId.setPrefWidth(80);
        }
        if (colNombre != null) {
            colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
            colNombre.setPrefWidth(200);
        }
        if (colActiva != null) {
            colActiva.setCellValueFactory(new PropertyValueFactory<>("activa"));
            colActiva.setPrefWidth(100);
        }
        if (colAcciones != null) {
            colAcciones.setPrefWidth(150);
        }
        
        // Toggle estado
        if (colActiva != null) colActiva.setCellFactory(tc -> new TableCell<>() {
            private final com.tracersoftware.common.controls.SwitchToggleButton sw = new com.tracersoftware.common.controls.SwitchToggleButton();
            private boolean prog = false;
            {
                sw.setMinWidth(48); sw.setPrefWidth(48); sw.setMaxWidth(48);
                sw.setMinHeight(26); sw.setPrefHeight(26); sw.setMaxHeight(26);
                sw.getStyleClass().add("cell-switch-btn");
                sw.switchedOnProperty().addListener((o,ov,nv)->{
                    if (prog) return;
                    int i = getIndex(); if (i<0||i>=getTableView().getItems().size()) return;
                    CategoriaMateriaPrimaItem it = getTableView().getItems().get(i); if (it==null) return;
                    boolean ns = Boolean.TRUE.equals(nv);
                    sw.setDisable(true);
                    Task<Void> t = new Task<>(){ @Override protected Void call() throws Exception { service.toggleEstado(it.getId(), ns); return null; } };
                    t.setOnSucceeded(ev->{ it.setActiva(ns); sw.setDisable(false); });
                    t.setOnFailed(ev->{ sw.setDisable(false); prog=true; sw.setSelected(!ns); prog=false; MessageToast.showSystemError(null, "Error al cambiar estado: " + (t.getException()==null?"":t.getException().getMessage())); });
                    new Thread(t,"catmp-toggle").start();
                });
            }
            @Override protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                prog=true; sw.setSelected(Boolean.TRUE.equals(item)); prog=false; setGraphic(sw);
            }
        });
        
        // Columna acciones
        if (colAcciones != null) colAcciones.setCellFactory(tc -> new TableCell<>() {
            private final Button btnEdit = new Button("Editar");
            private final Button btnDel = new Button("Eliminar");
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(6, btnEdit, btnDel);
            { 
                btnEdit.getStyleClass().addAll("btn-small","button-yellow","icon-btn"); 
                btnDel.getStyleClass().addAll("btn-small","button-red","icon-btn");
                btnEdit.setOnAction(e -> {
                    int idx = getIndex(); if (idx < 0 || idx >= getTableView().getItems().size()) return;
                    CategoriaMateriaPrimaItem item = getTableView().getItems().get(idx);
                    if (item != null) openForm(item);
                });
                btnDel.setOnAction(e -> {
                    int idx = getIndex(); if (idx < 0 || idx >= getTableView().getItems().size()) return;
                    CategoriaMateriaPrimaItem item = getTableView().getItems().get(idx);
                    if (item != null) deleteItem(item);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty); setGraphic(empty?null:box);
            }
        });
        
        // ExportBar en contenedor dedicado
        if (exportBarContainer != null) {
            com.tracersoftware.common.controls.ExportBar exportBar = new com.tracersoftware.common.controls.ExportBar(table, "categorias_materia_prima", () -> service.listAll());
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            exportBarContainer.getChildren().addAll(spacer, exportBar);
        }
        
        // Búsqueda y paginación
        if (searchBar != null) searchBar.textProperty().addListener((o,ov,nv)->{ if (paginator!=null) paginator.setPageIndex(0); applyPage(); });
        if (paginator != null) {
            paginator.pageIndexProperty().addListener((o,ov,nv)->applyPage());
            paginator.pageSizeProperty().addListener((o,ov,nv)->applyPage());
        }
        
        // Botón nuevo (estilo app)
        if (btnNuevo != null) {
            btnNuevo.getStyleClass().addAll("button-blue","btn-primary","icon-btn"); 
            btnNuevo.setText("➕ Nuevo");
            btnNuevo.setOnAction(e->openForm(null));
        }
    }

    @Override public void onViewShown() { load(); }

    private void load() {
        Task<java.util.List<CategoriaMateriaPrimaItem>> t = new Task<>() { @Override protected java.util.List<CategoriaMateriaPrimaItem> call() throws Exception { return service.listAll(); } };
        t.setOnSucceeded(ev->{ full.clear(); if (t.getValue()!=null) full.addAll(t.getValue()); applyPage(); });
        t.setOnFailed(ev->{ MessageToast.showSystemError(null, "Error cargando categorías: " + (t.getException()==null?"":t.getException().getMessage())); });
        new Thread(t,"catmp-load").start();
    }

    private void applyPage() {
        String q = searchBar != null && searchBar.getText() != null ? searchBar.getText().trim().toLowerCase() : "";
        java.util.List<CategoriaMateriaPrimaItem> filtered = new java.util.ArrayList<>();
        for (var it : full) { 
            if (q.isEmpty() || (it.getNombre() + " " + it.getId()).toLowerCase().contains(q)) filtered.add(it); 
        }
        int page = paginator != null ? paginator.getPageIndex() : 0;
        int size = paginator != null ? paginator.getPageSize() : Math.max(1, filtered.size());
        int total = filtered.size(); int from = Math.max(0, Math.min(page*size,total)); int to = Math.max(from, Math.min(from+size,total));
        data.setAll(filtered.subList(from,to));
        if (paginator != null) paginator.setTotalItems(total);
    }

    private void deleteItem(CategoriaMateriaPrimaItem item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar eliminación");
        alert.setHeaderText("¿Está seguro de eliminar esta categoría?");
        alert.setContentText("Nombre: " + item.getNombre());
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> deleteTask = new Task<>() {
                    @Override protected Void call() throws Exception {
                        service.delete(item.getId());
                        return null;
                    }
                };
                deleteTask.setOnSucceeded(e -> {
                    MessageToast.show(null, "Categoría eliminada exitosamente", MessageToast.ToastType.SUCCESS);
                    load(); // Recargar la lista
                });
                deleteTask.setOnFailed(e -> {
                    MessageToast.showSystemError(null, "Error al eliminar categoría: " + 
                        (deleteTask.getException() == null ? "" : deleteTask.getException().getMessage()));
                });
                new Thread(deleteTask, "catmp-delete").start();
            }
        });
    }

    private void openForm(CategoriaMateriaPrimaItem item) {
        try {
            javafx.fxml.FXMLLoader l = new javafx.fxml.FXMLLoader(getClass().getResource("/categoriasmateriaprima/fxml/categoriasmateriaprima_form.fxml"));
            javafx.scene.Parent root = l.load();
            CategoriaMateriaPrimaFormController ctrl = l.getController();
            ctrl.setOnSaved(this::load);
            if (item != null) ctrl.edit(item);
            com.tracersoftware.common.ui.ModalUtils.showModalAndWait(null, root, item==null?"Nueva Categoría":"Editar Categoría");
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}
