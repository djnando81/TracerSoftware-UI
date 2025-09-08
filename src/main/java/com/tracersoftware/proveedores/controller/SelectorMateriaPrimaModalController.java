package com.tracersoftware.proveedores.controller;

import com.tracersoftware.proveedores.model.MateriaPrimaItem;
import com.tracersoftware.proveedores.api.ProveedoresApiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.List;

public class SelectorMateriaPrimaModalController {
    @FXML private Label lblTitle;
    @FXML private TextField txtBuscar;
    @FXML private ListView<MateriaPrimaItem> listMateriasPrimas;
    @FXML private Label lblCount;
    @FXML private Label lblError;
    @FXML private ProgressIndicator progress;
    @FXML private Button btnSelect;
    @FXML private Button btnCancel;

    private final ProveedoresApiService service = new ProveedoresApiService();
    private final ObservableList<MateriaPrimaItem> todasLasMaterias = FXCollections.observableArrayList();
    private final FilteredList<MateriaPrimaItem> materiasFiltered = new FilteredList<>(todasLasMaterias);
    private ObservableList<MateriaPrimaItem> materiasYaSeleccionadas;
    private MateriaPrimaItem result;
    private boolean cancelled = false;

    @FXML
    public void initialize() {
        // Configurar ListView
        listMateriasPrimas.setItems(materiasFiltered);
        
        // Cell factory para mostrar información de materias primas
        listMateriasPrimas.setCellFactory(listView -> new ListCell<MateriaPrimaItem>() {
            @Override
            protected void updateItem(MateriaPrimaItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Formato: "Nombre - Categoría (Unidad)"
                    setText(String.format("%s - %s (%s)", 
                        item.getNombre(),
                        item.getCategoriaMateriaPrimaNombre() != null ? item.getCategoriaMateriaPrimaNombre() : "Sin categoría",
                        item.getUnidad() != null ? item.getUnidad() : "Sin unidad"));
                }
            }
        });
        
        // Configurar búsqueda
        txtBuscar.textProperty().addListener((obs, oldText, newText) -> {
            materiasFiltered.setPredicate(materia -> {
                if (newText == null || newText.trim().isEmpty()) {
                    return true;
                }
                String searchText = newText.toLowerCase().trim();
                return materia.getNombre().toLowerCase().contains(searchText) ||
                       (materia.getCategoriaMateriaPrimaNombre() != null && materia.getCategoriaMateriaPrimaNombre().toLowerCase().contains(searchText));
            });
            updateCount();
        });
        
        // Listener para actualizar contador
        materiasFiltered.addListener((javafx.collections.ListChangeListener<MateriaPrimaItem>) c -> {
            updateCount();
        });
        
        // Configurar botones
        btnSelect.setOnAction(e -> seleccionar());
        btnCancel.setOnAction(e -> cancelar());
        
        // Habilitar botón solo cuando hay selección
        btnSelect.disableProperty().bind(listMateriasPrimas.getSelectionModel().selectedItemProperty().isNull());
        
        // Cargar materias primas
        cargarMateriasPrimas();
    }
    
    private void cargarMateriasPrimas() {
        Task<List<MateriaPrimaItem>> task = new Task<List<MateriaPrimaItem>>() {
            @Override
            protected List<MateriaPrimaItem> call() throws Exception {
                return service.obtenerTodasLasMateriasPrimas();
            }
        };
        
        task.setOnSucceeded(e -> {
            List<MateriaPrimaItem> materias = task.getValue();
            todasLasMaterias.clear();
            if (materias != null) {
                // Filtrar materias que ya están seleccionadas
                materias.removeIf(materia -> {
                    if (materiasYaSeleccionadas == null) return false;
                    return materiasYaSeleccionadas.stream()
                        .anyMatch(seleccionada -> seleccionada.getId() == materia.getId());
                });
                todasLasMaterias.addAll(materias);
            }
            updateCount();
            hideProgress();
        });
        
        task.setOnFailed(e -> {
            hideProgress();
            showError("Error al cargar materias primas: " + task.getException().getMessage());
        });
        
        showProgress();
        new Thread(task).start();
    }
    
    public void setMateriasYaSeleccionadas(ObservableList<MateriaPrimaItem> materiasSeleccionadas) {
        this.materiasYaSeleccionadas = materiasSeleccionadas;
        // Recargar para filtrar las ya seleccionadas
        if (!todasLasMaterias.isEmpty()) {
            cargarMateriasPrimas();
        }
    }
    
    private void seleccionar() {
        MateriaPrimaItem selected = listMateriasPrimas.getSelectionModel().getSelectedItem();
        if (selected != null) {
            result = selected;
            closeWindow();
        }
    }
    
    private void cancelar() {
        cancelled = true;
        closeWindow();
    }
    
    private void updateCount() {
        int count = materiasFiltered.size();
        lblCount.setText(count + " materias primas disponibles");
    }
    
    private void showProgress() {
        if (progress != null) progress.setVisible(true);
    }
    
    private void hideProgress() {
        if (progress != null) progress.setVisible(false);
    }
    
    private void showError(String message) {
        if (lblError != null) {
            lblError.setText(message);
            lblError.setVisible(true);
            lblError.setManaged(true);
        }
    }
    
    private void closeWindow() {
        if (btnCancel != null && btnCancel.getScene() != null) {
            Stage stage = (Stage) btnCancel.getScene().getWindow();
            if (stage != null) stage.close();
        }
    }
    
    public MateriaPrimaItem getResult() {
        return result;
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
}
