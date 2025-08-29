
package com.tracersoftware.dashboard.settings;

import com.fasterxml.jackson.databind.JsonNode;
import com.tracersoftware.api.ApiClient;
import com.tracersoftware.api.SettingsService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class SettingsController {

    @FXML
    private Label placeholder;

    @FXML
    public void initialize() {
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                try {
                    ApiClient client = new ApiClient();
                    SettingsService svc = new SettingsService(client);
                    JsonNode data = svc.getAllSettings();
                    final String text = (data != null && data.isArray()) ? "Configuraciones: " + data.size() : "Configuraciones: sin datos";
                    javafx.application.Platform.runLater(() -> placeholder.setText(text));
                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> placeholder.setText("Error al cargar configuraciones: " + e.getMessage()));
                }
                return null;
            }
        };
        new Thread(task).start();
    }
}
