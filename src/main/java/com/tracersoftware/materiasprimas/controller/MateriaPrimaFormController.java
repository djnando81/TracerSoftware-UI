package com.tracersoftware.materiasprimas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tracersoftware.common.controls.MessageToast;
import com.tracersoftware.materiasprimas.api.MateriasPrimasApiService;
import com.tracersoftware.categoriasmateriaprima.api.CategoriasMateriaPrimaApiService;
import com.tracersoftware.categoriasmateriaprima.model.CategoriaMateriaPrimaItem;
import com.tracersoftware.unidades.api.UnidadesApiService;
import com.tracersoftware.unidades.model.UnidadMedidaItem;
import com.tracersoftware.materiasprimas.model.MateriaPrimaItem;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.layout.AnchorPane;

public class MateriaPrimaFormController {

    @FXML private Label lblTitle;
    @FXML private TextField txtNombre;
    @FXML private TextField txtCodigoInterno;
    @FXML private ComboBox<UnidadMedidaItem> cboUnidad;
    @FXML private ComboBox<MateriaPrimaItem> cboOrigen;
    @FXML private ComboBox<CategoriaMateriaPrimaItem> cboCategoria;
    @FXML private TextField txtStockMin;
    @FXML private TextField txtStockMax;
    @FXML private TextField txtCostoUnitario;
    @FXML private CheckBox chkActiva;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Label lblError;
    @FXML private ProgressIndicator progress;
    @FXML private AnchorPane overlay;
    @FXML private ProgressIndicator overlayProgress;

    private final MateriasPrimasApiService service = new MateriasPrimasApiService();
    private final CategoriasMateriaPrimaApiService categoriasService = new CategoriasMateriaPrimaApiService();
    private final UnidadesApiService unidadesService = new UnidadesApiService();
    private final ObjectMapper mapper = new ObjectMapper();
    private Integer editingId = null;
    private Runnable onSaved;
    // pending selections when choices load asynchronously
    private Integer pendingCategoriaId = null;
    private String pendingUnidadAbrev = null;
    private Integer pendingOrigenId = null;

    @FXML
    public void initialize() {
        if (btnCancel != null) btnCancel.setOnAction(e -> close());
        if (btnSave != null) btnSave.setOnAction(e -> save());
        try {
            if (btnSave != null) {
                btnSave.setText("Crear");
                btnSave.getStyleClass().removeAll("btn-action-update");
                if (!btnSave.getStyleClass().contains("btn-action-create")) btnSave.getStyleClass().add("btn-action-create");
            }
            if (btnCancel != null && !btnCancel.getStyleClass().contains("btn-action-cancel")) {
                btnCancel.getStyleClass().add("btn-action-cancel");
            }
        } catch (Exception ignored) {}
        try {
            // Mostrar nombre en celdas del ComboBox
            cboCategoria.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(CategoriaMateriaPrimaItem item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : (item.getId() + " - " + item.getNombre()));
                }
            });
            cboCategoria.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(CategoriaMateriaPrimaItem item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : (item.getId() + " - " + item.getNombre()));
                }
            });
            // Unidades: mostrar abreviatura - nombre
            cboUnidad.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(UnidadMedidaItem item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : (item.getAbreviatura() + " - " + item.getNombre()));
                }
            });
            cboUnidad.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(UnidadMedidaItem item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : (item.getAbreviatura() + " - " + item.getNombre()));
                }
            });
        } catch (Exception ignored) {}
        // cargar categorías
        try {
            javafx.concurrent.Task<java.util.List<CategoriaMateriaPrimaItem>> t = new javafx.concurrent.Task<>() {
                @Override protected java.util.List<CategoriaMateriaPrimaItem> call() throws Exception { return categoriasService.listAll(); }
            };
            t.setOnSucceeded(ev -> {
                try {
                    cboCategoria.getItems().setAll(t.getValue());
                    if (pendingCategoriaId != null) {
                        for (CategoriaMateriaPrimaItem c : cboCategoria.getItems()) {
                            if (c.getId() == pendingCategoriaId) { cboCategoria.getSelectionModel().select(c); break; }
                        }
                        pendingCategoriaId = null;
                    }
                } catch (Exception ignored) {}
            });
            new Thread(t, "catmp-load").start();
        } catch (Exception ignored) {}
        // cargar unidades
        try {
            javafx.concurrent.Task<java.util.List<UnidadMedidaItem>> t2 = new javafx.concurrent.Task<>() {
                @Override protected java.util.List<UnidadMedidaItem> call() throws Exception { return unidadesService.listAll(); }
            };
            t2.setOnSucceeded(ev -> {
                try {
                    cboUnidad.getItems().setAll(t2.getValue());
                    if (pendingUnidadAbrev != null) {
                        for (UnidadMedidaItem u : cboUnidad.getItems()) {
                            if (u.getAbreviatura() != null && u.getAbreviatura().equalsIgnoreCase(pendingUnidadAbrev)) { cboUnidad.getSelectionModel().select(u); break; }
                        }
                        pendingUnidadAbrev = null;
                    }
                } catch (Exception ignored) {}
            });
            new Thread(t2, "unidades-load").start();
        } catch (Exception ignored) {}
        // cargar orígenes (materias primas existentes)
        try {
            javafx.concurrent.Task<java.util.List<MateriaPrimaItem>> t3 = new javafx.concurrent.Task<>() {
                @Override protected java.util.List<MateriaPrimaItem> call() throws Exception { return service.listAll(); }
            };
            t3.setOnSucceeded(ev -> {
                try {
                    java.util.List<MateriaPrimaItem> list = t3.getValue();
                    if (list != null) {
                        if (editingId != null) list.removeIf(mp -> mp.getId() == editingId);
                        cboOrigen.getItems().setAll(list);
                        if (pendingOrigenId != null) {
                            for (MateriaPrimaItem o : cboOrigen.getItems()) {
                                if (o.getId() == pendingOrigenId) { cboOrigen.getSelectionModel().select(o); break; }
                            }
                            pendingOrigenId = null;
                        }
                    }
                } catch (Exception ignored) {}
            });
            // Render id - nombre (o código - nombre si está)
            cboOrigen.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(MateriaPrimaItem item, boolean empty) {
                    super.updateItem(item, empty);
                    String label = null;
                    if (!empty && item != null) {
                        String code = item.getCodigoInterno();
                        String name = item.getNombre();
                        label = (code != null && !code.isBlank() ? code + " - " : "") + (name == null ? ("MP #" + item.getId()) : name);
                    }
                    setText(label);
                }
            });
            cboOrigen.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(MateriaPrimaItem item, boolean empty) {
                    super.updateItem(item, empty);
                    String label = null;
                    if (!empty && item != null) {
                        String code = item.getCodigoInterno();
                        String name = item.getNombre();
                        label = (code != null && !code.isBlank() ? code + " - " : "") + (name == null ? ("MP #" + item.getId()) : name);
                    }
                    setText(label);
                }
            });
            new Thread(t3, "origenes-load").start();
        } catch (Exception ignored) {}
    }

    public void setOnSaved(Runnable r) { this.onSaved = r; }

    public void edit(MateriaPrimaItem item) {
        if (item == null) return;
        this.editingId = item.getId();
        if (lblTitle != null) lblTitle.setText("Editar Materia Prima #" + item.getId());
        try {
            if (btnSave != null) {
                btnSave.setText("Actualizar");
                btnSave.getStyleClass().removeAll("btn-action-create");
                if (!btnSave.getStyleClass().contains("btn-action-update")) btnSave.getStyleClass().add("btn-action-update");
            }
        } catch (Exception ignored) {}
        txtNombre.setText(item.getNombre());
        txtCodigoInterno.setText(item.getCodigoInterno());
        // seleccionar unidad por abreviatura
        try {
            for (UnidadMedidaItem u : cboUnidad.getItems()) {
                if (u.getAbreviatura() != null && u.getAbreviatura().equalsIgnoreCase(item.getUnidad())) {
                    cboUnidad.getSelectionModel().select(u);
                    break;
                }
            }
            if (cboUnidad.getSelectionModel().isEmpty()) pendingUnidadAbrev = item.getUnidad();
        } catch (Exception ignored) {}
        // seleccionar origen por id
        try {
            if (item.getMateriaPrimaOrigenId() > 0) {
                for (MateriaPrimaItem o : cboOrigen.getItems()) {
                    if (o.getId() == item.getMateriaPrimaOrigenId()) { cboOrigen.getSelectionModel().select(o); break; }
                }
            }
            if (cboOrigen.getSelectionModel().isEmpty()) pendingOrigenId = item.getMateriaPrimaOrigenId();
        } catch (Exception ignored) {}
        try { txtStockMin.setText(String.valueOf(item.getStockMinimo())); } catch (Exception ignored) {}
        try { txtStockMax.setText(String.valueOf(item.getStockMaximo())); } catch (Exception ignored) {}
        try { txtCostoUnitario.setText(String.valueOf(item.getCostoUnitario())); } catch (Exception ignored) {}
        try { chkActiva.setSelected(item.isActiva()); } catch (Exception ignored) {}
        // seleccionar categoría por nombre si disponible
        try {
            if (item.getCategoriaMateriaPrimaId() > 0) {
                for (CategoriaMateriaPrimaItem c : cboCategoria.getItems()) {
                    if (c.getId() == item.getCategoriaMateriaPrimaId()) { cboCategoria.getSelectionModel().select(c); break; }
                }
            }
            if (cboCategoria.getSelectionModel().isEmpty()) pendingCategoriaId = item.getCategoriaMateriaPrimaId();
        } catch (Exception ignored) {}
    }

    private void save() {
        try {
            if (progress != null) progress.setVisible(true);
            if (overlay != null) overlay.setVisible(true);
            ObjectNode body = mapper.createObjectNode();
            // DTO de creación/edición (maestro)
            body.put("nombre", txtNombre.getText()==null?"":txtNombre.getText().trim());
            body.put("codigoInterno", txtCodigoInterno.getText()==null?"":txtCodigoInterno.getText().trim());
            String unidad = null; try { var u = cboUnidad.getSelectionModel().getSelectedItem(); if (u!=null) unidad = u.getAbreviatura(); } catch (Exception ignored) {}
            body.put("unidad", unidad==null?"":unidad);
            int catId = 0; try { var sel = cboCategoria.getSelectionModel().getSelectedItem(); if (sel!=null) catId = sel.getId(); } catch (Exception ignored) {}
            body.put("categoriaMateriaPrimaId", catId);
            try {
                var origSel = cboOrigen.getSelectionModel().getSelectedItem();
                if (origSel != null) body.put("materiaPrimaOrigenId", origSel.getId());
                else body.putNull("materiaPrimaOrigenId");
            } catch (Exception ignored) {}
            double sMin = 0; try { sMin = Double.parseDouble(txtStockMin.getText().trim()); } catch (Exception ignored) {}
            double sMax = 0; try { sMax = Double.parseDouble(txtStockMax.getText().trim()); } catch (Exception ignored) {}
            double costo = 0; try { costo = Double.parseDouble(txtCostoUnitario.getText().trim()); } catch (Exception ignored) {}
            body.put("stockMinimo", sMin);
            body.put("stockMaximo", sMax);
            body.put("costoUnitario", costo);
            body.put("activa", chkActiva.isSelected());

            if (editingId == null) {
                service.create(body);
                MessageToast.show(null, "Materia prima creada", MessageToast.ToastType.SUCCESS);
            } else {
                service.update(editingId, body);
                MessageToast.show(null, "Materia prima actualizada", MessageToast.ToastType.SUCCESS);
            }
            if (onSaved != null) onSaved.run();
            close();
        } catch (Exception ex) {
            if (lblError != null) {
                lblError.setManaged(true);
                lblError.setVisible(true);
                lblError.setText("Error al guardar: " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
            }
            MessageToast.showSystemError(null, "Error al guardar: " + ex.getMessage());
        } finally {
            try { if (progress != null) progress.setVisible(false); } catch (Exception ignored) {}
            try { if (overlay != null) overlay.setVisible(false); } catch (Exception ignored) {}
        }
    }

    private void close() {
        try {
            Stage s = (Stage) btnCancel.getScene().getWindow();
            s.close();
        } catch (Exception ignore) {}
    }
}
