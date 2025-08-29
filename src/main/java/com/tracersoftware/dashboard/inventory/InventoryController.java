
package com.tracersoftware.dashboard.inventory;

import com.fasterxml.jackson.databind.JsonNode;
import com.tracersoftware.api.ApiClient;
import com.tracersoftware.api.InventoryService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class InventoryController {

    @FXML
    private Label placeholder;

    @FXML
    public void initialize() {
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                try {
                    ApiClient client = new ApiClient();
                    InventoryService svc = new InventoryService(client);
                    JsonNode data = svc.getStock();
                    final String text = (data != null && data.isArray()) ? "Items en stock: " + data.size() : "Stock: sin datos";
                    javafx.application.Platform.runLater(() -> placeholder.setText(text));
                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> placeholder.setText("Error al cargar inventario: " + e.getMessage()));
                }
                return null;
            }
        };
        new Thread(task).start();
    }
}
