package com.tracersoftware.proveedores.controller;

import com.tracersoftware.common.controls.MessageToast;
import com.tracersoftware.proveedores.api.ProveedoresApiService;
import com.tracersoftware.proveedores.model.MateriaPrimaItem;
import com.tracersoftware.proveedores.model.ProveedorItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ProveedoresFormController {
    @FXML private Label lblTitle;
    @FXML private TextField txtNombre;
    @FXML private TextField txtCuit;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtEmail;
    @FXML private TextField txtDireccion;
    @FXML private CheckBox chkActivo;
    @FXML private Label lblError;
    @FXML private ProgressIndicator progress;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    @FXML private Button btnAgregarMateria;
    @FXML private Button btnQuitarMateria;
    @FXML private ListView<MateriaPrimaItem> listMateriasPrimas;
    @FXML private Label lblCountMaterias;

    private final ProveedoresApiService service = new ProveedoresApiService();
    private final ObservableList<MateriaPrimaItem> materiasPrimas = FXCollections.observableArrayList();
    private ProveedorItem currentItem;
    private boolean result = false;
    private boolean cancelled = false;
    private Runnable onSavedCallback; // Callback para ejecutar cuando se guarde exitosamente

    @FXML public void initialize() {
        // OBLIGATORIO: Configuración de ListView de materias primas
        if (listMateriasPrimas != null) {
            listMateriasPrimas.setItems(materiasPrimas);
            
            // OBLIGATORIO: Cell factory personalizada para mostrar información completa
            listMateriasPrimas.setCellFactory(listView -> new ListCell<MateriaPrimaItem>() {
                @Override
                protected void updateItem(MateriaPrimaItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        // Formato: "Nombre - Categoría (Stock: X Unidad) - $Costo"
                        String display = String.format("%s - %s (Stock: %.2f %s) - $%.2f",
                            item.getNombre(),
                            item.getCategoriaMateriaPrimaNombre() != null ? item.getCategoriaMateriaPrimaNombre() : "Sin categoría",
                            item.getStockActual(),
                            item.getUnidad() != null ? item.getUnidad() : "Kg",
                            item.getCostoUnitario()
                        );
                        setText(display);
                        
                        // Estilo según estado
                        if (item.isActiva()) {
                            setStyle("-fx-text-fill: #2d3748;");
                        } else {
                            setStyle("-fx-text-fill: #a0aec0; -fx-font-style: italic;");
                        }
                    }
                }
            });
            
            // Listener para actualizar contador
            materiasPrimas.addListener((javafx.collections.ListChangeListener<MateriaPrimaItem>) c -> {
                updateMateriasCount();
            });
        }
        
        // OBLIGATORIO: CheckBox para estado
        if (chkActivo != null) {
            chkActivo.setSelected(true); // Default activo
        }
        
        // OBLIGATORIO: Botones con estilos estándar y funcionalidad
        if (btnAgregarMateria != null) {
            btnAgregarMateria.setOnAction(e -> agregarMateriaPrima());
        }
        
        if (btnQuitarMateria != null) {
            btnQuitarMateria.setOnAction(e -> quitarMateriaPrima());
        }
        
        if (btnGuardar != null) {
            btnGuardar.setOnAction(e -> guardar());
            // ESTABLECER TEXTO POR DEFECTO PARA MODO CREAR
            btnGuardar.setText("Crear");
        }
        
        if (btnCancelar != null) {
            btnCancelar.setOnAction(e -> cancelar());
        }
        
        // OBLIGATORIO: Validación de campos obligatorios
        setupValidation();
        
        // Inicializar contador
        updateMateriasCount();
        
        // Forzar validación inicial después de que todo esté configurado
        javafx.application.Platform.runLater(() -> {
            validateForm();
        });
    }
    
    private void setupValidation() {
        // Validación en tiempo real de campos obligatorios
        if (txtNombre != null) {
            txtNombre.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        }
        if (txtCuit != null) {
            txtCuit.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        }
        if (txtEmail != null) {
            txtEmail.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        }
        
        // Validación inicial
        validateForm();
    }
    
    private void validateForm() {
        boolean valid = true;
        java.util.List<String> errorList = new java.util.ArrayList<>();
        
        // Limpiar estilos de error previos
        clearFieldErrors();
        
        // Nombre obligatorio
        if (txtNombre != null) {
            String nombre = txtNombre.getText();
            if (nombre == null || nombre.trim().isEmpty()) {
                valid = false;
                errorList.add("El nombre del proveedor es obligatorio");
                markFieldError(txtNombre);
            }
        } else {
            valid = false;
        }
        
        // CUIT obligatorio y formato básico
        if (txtCuit != null) {
            String cuit = txtCuit.getText();
            if (cuit == null || cuit.trim().isEmpty()) {
                valid = false;
                errorList.add("El CUIT es obligatorio");
                markFieldError(txtCuit);
            } else if (cuit.length() < 11) {
                valid = false;
                errorList.add("El CUIT debe tener al menos 11 caracteres");
                markFieldError(txtCuit);
            }
        } else {
            valid = false;
        }
        
        // Email válido si se proporciona
        if (txtEmail != null && txtEmail.getText() != null && !txtEmail.getText().trim().isEmpty()) {
            String email = txtEmail.getText().trim();
            if (!email.contains("@") || !email.contains(".")) {
                valid = false;
                errorList.add("El email debe tener un formato válido");
                markFieldError(txtEmail);
            }
        }
        
        // Mostrar errores de forma estética
        if (lblError != null) {
            if (!errorList.isEmpty()) {
                // Crear mensaje más estético
                StringBuilder message = new StringBuilder();
                message.append("⚠️ Por favor, corrija los siguientes errores:\n\n");
                
                for (int i = 0; i < errorList.size(); i++) {
                    message.append("• ").append(errorList.get(i));
                    if (i < errorList.size() - 1) {
                        message.append("\n");
                    }
                }
                
                lblError.setText(message.toString());
                lblError.setVisible(true);
                lblError.setManaged(true);
                
                // Aplicar estilos CSS para mejor apariencia
                lblError.getStyleClass().removeAll("error-label", "warning-label");
                lblError.getStyleClass().add("validation-error");
                lblError.setStyle("-fx-background-color: #fff3cd; " +
                                "-fx-border-color: #ffeaa7; " +
                                "-fx-border-width: 1px; " +
                                "-fx-border-radius: 4px; " +
                                "-fx-background-radius: 4px; " +
                                "-fx-padding: 8px 12px; " +
                                "-fx-text-fill: #856404; " +
                                "-fx-font-size: 12px; " +
                                "-fx-line-spacing: 2px;");
            } else {
                lblError.setVisible(false);
                lblError.setManaged(false);
            }
        }
        
        // Habilitar/deshabilitar botón guardar
        if (btnGuardar != null) {
            btnGuardar.setDisable(!valid);
        }
    }
    
    private void clearFieldErrors() {
        if (txtNombre != null) {
            txtNombre.getStyleClass().remove("field-error");
            txtNombre.setStyle("");
        }
        if (txtCuit != null) {
            txtCuit.getStyleClass().remove("field-error");
            txtCuit.setStyle("");
        }
        if (txtEmail != null) {
            txtEmail.getStyleClass().remove("field-error");
            txtEmail.setStyle("");
        }
    }
    
    private void markFieldError(javafx.scene.control.TextField field) {
        if (field != null) {
            field.getStyleClass().add("field-error");
            field.setStyle("-fx-border-color: #dc3545; " +
                          "-fx-border-width: 2px; " +
                          "-fx-border-radius: 4px; " +
                          "-fx-background-color: #fff5f5;");
        }
    }
    
    private void agregarMateriaPrima() {
        try {
            // Cargar el modal selector siguiendo la documentación
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/proveedores/fxml/selector_materiaprima_modal.fxml"));
            javafx.scene.Parent root = loader.load();
            SelectorMateriaPrimaModalController controller = loader.getController();
            
            // Pasar las materias ya seleccionadas para filtrarlas
            controller.setMateriasYaSeleccionadas(materiasPrimas);
            
            // Obtener la ventana actual como owner para mantener jerarquía
            javafx.stage.Stage ownerStage = null;
            if (btnAgregarMateria != null && btnAgregarMateria.getScene() != null) {
                javafx.stage.Window window = btnAgregarMateria.getScene().getWindow();
                if (window instanceof javafx.stage.Stage) {
                    ownerStage = (javafx.stage.Stage) window;
                }
            }
            
            // Mostrar modal usando ModalUtils estándar con owner correcto
            com.tracersoftware.common.ui.ModalUtils.showModalAndWait(
                ownerStage, root, "Seleccionar Materia Prima");
            
            // Verificar resultado
            if (!controller.isCancelled() && controller.getResult() != null) {
                MateriaPrimaItem materiaSeleccionada = controller.getResult();
                materiasPrimas.add(materiaSeleccionada);
                MessageToast.show(null, "Materia prima agregada exitosamente", MessageToast.ToastType.SUCCESS);
                // Ejecutar validación después de cambios
                validateForm();
            }
            
        } catch (Exception ex) {
            MessageToast.showSystemError(null, "No se pudo abrir el formulario de materia prima: " + ex.getMessage());
        }
    }
    
    private void updateMateriasCount() {
        if (lblCountMaterias != null) {
            int count = materiasPrimas.size();
            lblCountMaterias.setText(count + " materias primas asociadas");
        }
        // Ejecutar validación cuando cambia el número de materias primas
        validateForm();
    }
    
    private void quitarMateriaPrima() {
        if (listMateriasPrimas != null) {
            MateriaPrimaItem selected = listMateriasPrimas.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Confirmar eliminación");
                alert.setHeaderText("¿Quitar materia prima del proveedor?");
                alert.setContentText("Materia prima: " + selected.getNombre());
                
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        materiasPrimas.remove(selected);
                        MessageToast.show(null, "Materia prima removida", MessageToast.ToastType.SUCCESS);
                    }
                });
            } else {
                MessageToast.show(null, "Seleccione una materia prima para quitar", MessageToast.ToastType.WARNING);
            }
        }
    }
    
    public boolean isResult() {
        return result;
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
    
    public ProveedorItem getResult() {
        return result ? currentItem : null;
    }
    
    // OBLIGATORIO: Método para establecer callback de actualización
    public void setOnSaved(Runnable callback) {
        this.onSavedCallback = callback;
    }
    
    public void edit(ProveedorItem item) {
        this.currentItem = item;
        if (lblTitle != null) {
            lblTitle.setText("Editar Proveedor");
        }
        
        // CAMBIAR TEXTO DEL BOTÓN A "ACTUALIZAR" EN MODO EDICIÓN
        if (btnGuardar != null) {
            btnGuardar.setText("Actualizar");
        }
        
        // Cargar datos del proveedor
        if (txtNombre != null) txtNombre.setText(item.getNombre());
        if (txtCuit != null) txtCuit.setText(item.getCuit());
        if (txtTelefono != null) txtTelefono.setText(item.getTelefono());
        if (txtEmail != null) txtEmail.setText(item.getEmail());
        if (txtDireccion != null) txtDireccion.setText(item.getDireccion());
        if (chkActivo != null) chkActivo.setSelected(item.isActivo());
        
        // Cargar materias primas
        materiasPrimas.clear();
        if (item.getMateriasPrimas() != null) {
            materiasPrimas.addAll(item.getMateriasPrimas());
        }
    }
    
    public void create() {
        this.currentItem = null;
        if (lblTitle != null) {
            lblTitle.setText("Nuevo Proveedor");
        }
        
        // ASEGURAR QUE EL BOTÓN DIGA "CREAR" EN MODO NUEVO
        if (btnGuardar != null) {
            btnGuardar.setText("Crear");
        }
        
        // Limpiar campos
        if (txtNombre != null) txtNombre.setText("");
        if (txtCuit != null) txtCuit.setText("");
        if (txtTelefono != null) txtTelefono.setText("");
        if (txtEmail != null) txtEmail.setText("");
        if (txtDireccion != null) txtDireccion.setText("");
        if (chkActivo != null) chkActivo.setSelected(true);
        
        // Limpiar materias primas
        materiasPrimas.clear();
    }
    
    @FXML private void guardar() {
        if (!validateFields()) return;
        
        ProveedorItem item = currentItem != null ? currentItem : new ProveedorItem();
        
        // Mapear campos
        item.setNombre(txtNombre.getText().trim());
        item.setCuit(txtCuit.getText().trim());
        item.setTelefono(txtTelefono != null ? txtTelefono.getText() : "");
        item.setEmail(txtEmail != null ? txtEmail.getText() : "");
        item.setDireccion(txtDireccion != null ? txtDireccion.getText() : "");
        item.setActivo(chkActivo != null ? chkActivo.isSelected() : true);
        item.setMateriasPrimas(new java.util.ArrayList<>(materiasPrimas));
        
        // Deshabilitar botón mientras se guarda
        if (btnGuardar != null) btnGuardar.setDisable(true);
        
        Task<Void> saveTask = new Task<>() {
            @Override protected Void call() throws Exception {
                // CREAR ESTRUCTURA JSON CORRECTA PARA EL API
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.node.ObjectNode jsonData = mapper.createObjectNode();
                
                // Campos básicos del proveedor
                jsonData.put("nombre", item.getNombre());
                jsonData.put("cuit", item.getCuit());
                jsonData.put("telefono", item.getTelefono());
                jsonData.put("email", item.getEmail());
                jsonData.put("direccion", item.getDireccion());
                
                // IMPORTANTE: Convertir materias primas a lista de IDs
                com.fasterxml.jackson.databind.node.ArrayNode materiaPrimaIds = mapper.createArrayNode();
                for (MateriaPrimaItem mp : materiasPrimas) {
                    materiaPrimaIds.add(mp.getId());
                }
                jsonData.set("materiaPrimaIds", materiaPrimaIds);
                
                System.out.println("Enviando JSON al API: " + jsonData.toString());
                
                if (currentItem != null) {
                    service.update(item.getId(), jsonData);
                } else {
                    service.create(jsonData);
                }
                return null;
            }
        };
        
        saveTask.setOnSucceeded(e -> {
            // IMPORTANTE: Actualizar currentItem para que getResult() funcione
            currentItem = item;
            
            MessageToast.show(null, 
                currentItem.getId() > 0 ? "Proveedor actualizado exitosamente" : "Proveedor creado exitosamente", 
                MessageToast.ToastType.SUCCESS);
            result = true;
            
            // EJECUTAR CALLBACK PARA ACTUALIZAR LISTA
            if (onSavedCallback != null) {
                onSavedCallback.run();
            }
            
            closeWindow();
        });
        
        saveTask.setOnFailed(e -> {
            if (btnGuardar != null) btnGuardar.setDisable(false);
            MessageToast.showSystemError(null, "Error al guardar proveedor: " + 
                (saveTask.getException() == null ? "" : saveTask.getException().getMessage()));
        });
        
        new Thread(saveTask, "proveedores-save").start();
    }
    
    private boolean validateFields() {
        // Validación nombre
        if (txtNombre == null || txtNombre.getText() == null || txtNombre.getText().trim().isEmpty()) {
            MessageToast.show(null, "El nombre es obligatorio", MessageToast.ToastType.WARNING);
            if (txtNombre != null) txtNombre.requestFocus();
            return false;
        }
        
        // Validación CUIT
        if (txtCuit == null || txtCuit.getText() == null || txtCuit.getText().trim().isEmpty()) {
            MessageToast.show(null, "El CUIT es obligatorio", MessageToast.ToastType.WARNING);
            if (txtCuit != null) txtCuit.requestFocus();
            return false;
        }
        
        String cuit = txtCuit.getText().trim();
        if (cuit.length() < 11) {
            MessageToast.show(null, "El CUIT debe tener al menos 11 caracteres", MessageToast.ToastType.WARNING);
            if (txtCuit != null) txtCuit.requestFocus();
            return false;
        }
        
        // Validación email si está presente
        if (txtEmail != null && txtEmail.getText() != null && !txtEmail.getText().trim().isEmpty()) {
            String email = txtEmail.getText().trim();
            if (!email.contains("@") || !email.contains(".")) {
                MessageToast.show(null, "El email no tiene un formato válido", MessageToast.ToastType.WARNING);
                txtEmail.requestFocus();
                return false;
            }
        }
        
        return true;
    }
    
    @FXML private void cancelar() {
        cancelled = true;
        result = false;
        closeWindow();
    }
    
    private void closeWindow() {
        if (btnCancelar != null && btnCancelar.getScene() != null) {
            Stage stage = (Stage) btnCancelar.getScene().getWindow();
            if (stage != null) stage.close();
        }
    }
}
