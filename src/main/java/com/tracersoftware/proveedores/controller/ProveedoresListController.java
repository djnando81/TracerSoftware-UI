package com.tracersoftware.proveedores.controller;

import com.tracersoftware.common.controls.MessageToast;
import com.tracersoftware.common.controls.SwitchToggleButton;
import com.tracersoftware.proveedores.api.ProveedoresApiService;
import com.tracersoftware.proveedores.model.ProveedorItem;
import com.tracersoftware.proveedores.model.MateriaPrimaItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class ProveedoresListController implements com.tracersoftware.ui.ViewLifecycle {
    @FXML private TableView<ProveedorItem> table;
    @FXML private TableColumn<ProveedorItem,Integer> colId;
    @FXML private TableColumn<ProveedorItem,String> colNombre;
    @FXML private TableColumn<ProveedorItem,String> colCuit;
    @FXML private TableColumn<ProveedorItem,String> colTelefono;
    @FXML private TableColumn<ProveedorItem,String> colEmail;
    @FXML private TableColumn<ProveedorItem,String> colMateriasPrimas;
    @FXML private TableColumn<ProveedorItem,Boolean> colActivo;
    @FXML private TableColumn<ProveedorItem,Void> colAcciones;
    @FXML private com.tracersoftware.common.controls.PaginatorControl paginator;
    @FXML private com.tracersoftware.common.controls.SearchBar searchBar;
    @FXML private Button btnNuevo;
    @FXML private javafx.scene.layout.HBox exportBarContainer; // OBLIGATORIO

    private final ProveedoresApiService service = new ProveedoresApiService();
    private final ObservableList<ProveedorItem> data = FXCollections.observableArrayList();
    private final java.util.List<ProveedorItem> full = new java.util.ArrayList<>();

    @FXML public void initialize() {
        // OBLIGATORIO: Configuración de tabla
        if (table != null) {
            table.setItems(data);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        }
        
        // OBLIGATORIO: Configuración de columnas con anchos estándar
        if (colId != null) {
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colId.setPrefWidth(80);
        }
        if (colNombre != null) {
            colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
            colNombre.setPrefWidth(180);
        }
        if (colCuit != null) {
            colCuit.setCellValueFactory(new PropertyValueFactory<>("cuit"));
            colCuit.setPrefWidth(120);
        }
        if (colTelefono != null) {
            colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
            colTelefono.setPrefWidth(120);
        }
        if (colEmail != null) {
            colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
            colEmail.setPrefWidth(180);
        }
        if (colMateriasPrimas != null) {
            colMateriasPrimas.setPrefWidth(200);
            colMateriasPrimas.setCellValueFactory(cellData -> {
                ProveedorItem proveedor = cellData.getValue();
                if (proveedor.getMateriasPrimas() != null) {
                    int count = proveedor.getMateriasPrimas().size();
                    return new javafx.beans.property.SimpleStringProperty(count + " materias");
                }
                return new javafx.beans.property.SimpleStringProperty("0 materias");
            });
            
            // Cell factory para mostrar ComboBox con materias primas
            colMateriasPrimas.setCellFactory(tc -> new TableCell<ProveedorItem, String>() {
                private final ComboBox<String> comboBox = new ComboBox<>();
                
                {
                    comboBox.setMaxWidth(Double.MAX_VALUE);
                    comboBox.getStyleClass().add("combo-cell");
                    
                    // Listener para cuando se selecciona un item
                    comboBox.setOnAction(e -> {
                        String selected = comboBox.getValue();
                        if (selected != null && !selected.isEmpty()) {
                            // Mostrar información de la materia prima seleccionada
                            ProveedoresListController.this.showMateriaPrimaInfo(selected);
                        }
                    });
                }
                
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        ProveedorItem proveedor = getTableRow().getItem();
                        
                        comboBox.getItems().clear();
                        
                        if (proveedor.getMateriasPrimas() != null && !proveedor.getMateriasPrimas().isEmpty()) {
                            // Agregar prompt text
                            comboBox.setPromptText(proveedor.getMateriasPrimas().size() + " materias disponibles");
                            
                            // Agregar cada materia prima al ComboBox
                            for (MateriaPrimaItem materia : proveedor.getMateriasPrimas()) {
                                String displayText = String.format("%s (%s)", 
                                    materia.getNombre(),
                                    materia.getUnidad() != null ? materia.getUnidad() : "Sin unidad");
                                comboBox.getItems().add(displayText);
                            }
                        } else {
                            comboBox.setPromptText("Sin materias primas");
                        }
                        
                        setGraphic(comboBox);
                        setText(null);
                    }
                }
            });
        }
        if (colActivo != null) {
            colActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));
            colActivo.setPrefWidth(100);
        }
        if (colAcciones != null) {
            colAcciones.setPrefWidth(150);
        }
        
        // OBLIGATORIO: Toggle estado con SwitchToggleButton
        if (colActivo != null) colActivo.setCellFactory(tc -> new TableCell<>() {
            private final SwitchToggleButton sw = new SwitchToggleButton();
            private boolean prog = false;
            {
                sw.setMinWidth(48); sw.setPrefWidth(48); sw.setMaxWidth(48);
                sw.setMinHeight(26); sw.setPrefHeight(26); sw.setMaxHeight(26);
                sw.getStyleClass().add("cell-switch-btn");
                sw.switchedOnProperty().addListener((o,ov,nv) -> {
                    if (prog) return;
                    int idx = getIndex(); 
                    if (idx < 0 || idx >= getTableView().getItems().size()) return;
                    ProveedorItem row = getTableView().getItems().get(idx);
                    boolean ns = Boolean.TRUE.equals(nv);
                    sw.setDisable(true);
                    Task<Void> t = new Task<>() { 
                        @Override protected Void call() throws Exception { 
                            service.toggleEstado(row.getId(), ns); 
                            return null; 
                        } 
                    };
                    t.setOnSucceeded(e -> { row.setActivo(ns); sw.setDisable(false); });
                    t.setOnFailed(e -> { 
                        sw.setDisable(false); 
                        prog = true; 
                        sw.setSelected(!ns); 
                        prog = false; 
                        MessageToast.showSystemError(null, "Error al cambiar estado: " + 
                            (t.getException() == null ? "" : t.getException().getMessage())); 
                    });
                    new Thread(t, "proveedores-toggle").start();
                });
            }
            @Override protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                prog = true; sw.setSelected(Boolean.TRUE.equals(item)); prog = false;
                setGraphic(sw);
            }
        });
        
        // OBLIGATORIO: Columna acciones con botones estilizados
        if (colAcciones != null) colAcciones.setCellFactory(tc -> new TableCell<>() {
            private final Button btnEdit = new Button("Editar");
            private final Button btnDel = new Button("Eliminar");
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(6, btnEdit, btnDel);
            { 
                btnEdit.getStyleClass().addAll("btn-small", "button-yellow", "icon-btn"); 
                btnDel.getStyleClass().addAll("btn-small", "button-red", "icon-btn");
                btnEdit.setOnAction(e -> {
                    int idx = getIndex(); 
                    if (idx < 0 || idx >= getTableView().getItems().size()) return;
                    ProveedorItem item = getTableView().getItems().get(idx);
                    if (item != null) openForm(item);
                });
                btnDel.setOnAction(e -> {
                    int idx = getIndex(); 
                    if (idx < 0 || idx >= getTableView().getItems().size()) return;
                    ProveedorItem item = getTableView().getItems().get(idx);
                    if (item != null) deleteItem(item);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty); 
                setGraphic(empty ? null : box);
            }
        });
        
        // OBLIGATORIO: ExportBar en contenedor dedicado
        if (exportBarContainer != null) {
            com.tracersoftware.common.controls.ExportBar exportBar = 
                new com.tracersoftware.common.controls.ExportBar(table, "proveedores", () -> service.listAll());
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            exportBarContainer.getChildren().addAll(spacer, exportBar);
        }
        
        // OBLIGATORIO: Búsqueda y paginación
        if (searchBar != null) searchBar.textProperty().addListener((o,ov,nv) -> { 
            if (paginator != null) paginator.setPageIndex(0); 
            applyPage(); 
        });
        if (paginator != null) { 
            paginator.pageIndexProperty().addListener((o,ov,nv) -> applyPage()); 
            paginator.pageSizeProperty().addListener((o,ov,nv) -> applyPage()); 
        }
        
        // OBLIGATORIO: Botón nuevo con estilos estándar
        if (btnNuevo != null) {
            btnNuevo.getStyleClass().addAll("button-blue", "btn-primary", "icon-btn"); 
            btnNuevo.setText("➕ Nuevo");
            btnNuevo.setOnAction(e -> openForm(null));
        }
    }

    @Override public void onViewShown() { load(); }
    
    private void load() {
        Task<java.util.List<ProveedorItem>> t = new Task<>() { 
            @Override protected java.util.List<ProveedorItem> call() throws Exception { 
                return service.listAll(); 
            } 
        };
        t.setOnSucceeded(e -> { 
            full.clear(); 
            if (t.getValue() != null) full.addAll(t.getValue()); 
            applyPage(); 
        });
        t.setOnFailed(e -> {
            MessageToast.showSystemError(null, "Error cargando proveedores: " + 
                (t.getException() == null ? "" : t.getException().getMessage()));
        });
        new Thread(t, "proveedores-load").start();
    }
    
    private void applyPage() {
        String q = searchBar != null && searchBar.getText() != null ? 
                   searchBar.getText().trim().toLowerCase() : "";
        java.util.List<ProveedorItem> filtered = new java.util.ArrayList<>();
        for (ProveedorItem it : full) { 
            if (q.isEmpty() || (it.getNombre() + " " + it.getCuit() + " " + it.getEmail() + " " + it.getId()).toLowerCase().contains(q)) 
                filtered.add(it); 
        }
        int page = paginator != null ? paginator.getPageIndex() : 0;
        int size = paginator != null ? paginator.getPageSize() : Math.max(1, filtered.size());
        int total = filtered.size(); 
        int from = Math.max(0, Math.min(page * size, total)); 
        int to = Math.max(from, Math.min(from + size, total));
        data.setAll(filtered.subList(from, to));
        if (paginator != null) paginator.setTotalItems(total);
    }
    
    private void deleteItem(ProveedorItem item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar eliminación");
        alert.setHeaderText("¿Está seguro de eliminar este proveedor?");
        alert.setContentText("Nombre: " + item.getNombre() + "\nCUIT: " + item.getCuit());
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> deleteTask = new Task<>() {
                    @Override protected Void call() throws Exception {
                        service.delete(item.getId());
                        return null;
                    }
                };
                deleteTask.setOnSucceeded(e -> {
                    MessageToast.show(null, "Proveedor eliminado exitosamente", MessageToast.ToastType.SUCCESS);
                    load();
                });
                deleteTask.setOnFailed(e -> {
                    MessageToast.showSystemError(null, "Error al eliminar proveedor: " + 
                        (deleteTask.getException() == null ? "" : deleteTask.getException().getMessage()));
                });
                new Thread(deleteTask, "proveedores-delete").start();
            }
        });
    }
    
    private void openForm(ProveedorItem item) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/proveedores/fxml/proveedores_form.fxml"));
            Parent root = loader.load();
            ProveedoresFormController controller = loader.getController();
            
            // ESTABLECER CALLBACK PARA ACTUALIZAR LISTA INMEDIATAMENTE
            controller.setOnSaved(() -> load());
            
            // LLAMAR AL MÉTODO CORRECTO SEGÚN EL MODO
            if (item != null) {
                controller.edit(item);  // Modo edición - botón "Actualizar"
            } else {
                controller.create();    // Modo creación - botón "Crear"
            }
            
            // Obtener el Stage actual como owner
            javafx.stage.Stage ownerStage = null;
            if (table != null && table.getScene() != null) {
                javafx.stage.Window window = table.getScene().getWindow();
                if (window instanceof javafx.stage.Stage) {
                    ownerStage = (javafx.stage.Stage) window;
                }
            }
            
            com.tracersoftware.common.ui.ModalUtils.showModalAndWait(
                ownerStage, root, item == null ? "Nuevo Proveedor" : "Editar Proveedor");
                
        } catch (Exception ex) { 
            MessageToast.showSystemError(null, "No se pudo abrir el formulario: " + ex.getMessage()); 
        }
    }
    
    private void showMateriaPrimaInfo(String materiaPrimaInfo) {
        // Mostrar información de la materia prima seleccionada
        MessageToast.show(null, "Materia Prima: " + materiaPrimaInfo, MessageToast.ToastType.INFO);
    }
}
