
package com.tracersoftware.dashboard.reports;

import com.fasterxml.jackson.databind.JsonNode;
import com.tracersoftware.api.ApiClient;
import com.tracersoftware.api.ReportsService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ReportsController {

    @FXML
    private Label placeholder;

    @FXML
    public void initialize() {
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                try {
                    ApiClient client = new ApiClient();
                    ReportsService svc = new ReportsService(client);
                    JsonNode health = svc.getHealth();
                    final String text = "API Health: " + (health != null ? health.toString() : "n/a");
                    javafx.application.Platform.runLater(() -> placeholder.setText(text));
                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> placeholder.setText("Error al cargar reportes: " + e.getMessage()));
                }
                return null;
            }
        };
        new Thread(task).start();
    }
}
