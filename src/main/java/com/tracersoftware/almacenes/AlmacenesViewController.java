package com.tracersoftware.almacenes;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import com.tracersoftware.almacenes.model.AlmacenDTO;
import com.tracersoftware.common.ApiClient;
import java.util.List;

public class AlmacenesViewController {
    @FXML private TableView<AlmacenDTO> almacenesTable;
    @FXML private TableColumn<AlmacenDTO, Integer> colId;
    @FXML private TableColumn<AlmacenDTO, String> colNombre;
    @FXML private TableColumn<AlmacenDTO, String> colTipo;
    @FXML private TableColumn<AlmacenDTO, String> colDescripcion;
    @FXML private TableColumn<AlmacenDTO, Boolean> colActivo;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));
        loadAlmacenes();
    }

    private void loadAlmacenes() {
        List<AlmacenDTO> almacenes = ApiClient.getAlmacenes();
        ObservableList<AlmacenDTO> data = FXCollections.observableArrayList(almacenes);
        almacenesTable.setItems(data);
    }
}
