
package com.tracersoftware.dashboard.orders;

import com.fasterxml.jackson.databind.JsonNode;
import com.tracersoftware.api.ApiClient;
import com.tracersoftware.api.OrdersService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class OrdersController {

    @FXML
    private Label placeholder;

    @FXML
    public void initialize() {
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                try {
                    ApiClient client = new ApiClient();
                    OrdersService svc = new OrdersService(client);
                    JsonNode data = svc.listProductionOrders();
                    final String text = (data != null && data.isArray()) ? "Órdenes encontradas: " + data.size() : "Órdenes: sin datos";
                    javafx.application.Platform.runLater(() -> placeholder.setText(text));
                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> placeholder.setText("Error al cargar órdenes: " + e.getMessage()));
                }
                return null;
            }
        };
        new Thread(task).start();
    }
}
