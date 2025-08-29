package com.tracersoftware.app;


import com.tracersoftware.common.ConfigManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
// unused import removed
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class MainApp extends Application {

    private void checkAndDeleteLegacyToken() {
        Path keyFilePath = Paths.get(System.getProperty("user.dir"), "key.ini");
        if (Files.exists(keyFilePath)) {
            try (InputStream input = Files.newInputStream(keyFilePath)) {
                Properties properties = new Properties();
                properties.load(input);
                String apiToken = properties.getProperty("api.token");
                if (apiToken != null) {
                    String trimmed = apiToken.trim();
                    // heuristic: JWT-like tokens have two dots and are reasonably long
                    boolean looksLikeJwt = trimmed.split("\\.").length == 3 && trimmed.length() > 40;
                    if (looksLikeJwt) {
                        // backup key.ini then migrate token to user TokenStore
                        try {
                            Path bak = Paths.get(keyFilePath.toString() + ".bak");
                            Files.copy(keyFilePath, bak, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException ignored) {}
                        try {
                            // persist to user TokenStore file
                            com.tracersoftware.auth.TokenStore.saveTokens(trimmed, null);
                        } catch (Exception ignored) {}
                        // remove legacy file to avoid legacy usage
                        try { Files.delete(keyFilePath); } catch (IOException ignored) {}
                    } else if (trimmed.equalsIgnoreCase("refreshToken")) {
                        // clearly invalid placeholder: backup and remove
                        try {
                            Path bak = Paths.get(keyFilePath.toString() + ".bak");
                            Files.copy(keyFilePath, bak, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException ignored) {}
                        try { Files.delete(keyFilePath); } catch (IOException ignored) {}
                    } else {
                        // unknown value: leave file but create a backup so it's safe
                        try {
                            Path bak = Paths.get(keyFilePath.toString() + ".manualbackup");
                            Files.copy(keyFilePath, bak, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException ignored) {}
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Tracer Software App");
        try {
            // 1. Cargar configuración
            checkAndDeleteLegacyToken();
            ConfigManager.loadConfig();
            String urlBase = ConfigManager.getUrlBase();
            if (urlBase == null || urlBase.isEmpty()) {
                throw new RuntimeException("No se encontró la URL base de la API en config.ini");
            }



            // 3. Cargar pantalla de login
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/tracersoftware/login/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            // Add global button styles
            scene.getStylesheets().add(getClass().getResource("/com/tracersoftware/common/controls/buttons.css").toExternalForm());
            // Add paginator styles
            try { scene.getStylesheets().add(getClass().getResource("/com/tracersoftware/common/controls/paginator.css").toExternalForm()); } catch (Exception ignored) {}
            primaryStage.setScene(scene);
            primaryStage.setFullScreen(true);
            primaryStage.show();

            // Try to read persisted access token from user-level TokenStore and set it to common.SessionManager
            try {
                String tok = com.tracersoftware.auth.TokenStore.loadAccessToken();
                if (tok != null && !tok.isBlank()) {
                    com.tracersoftware.common.SessionManager.setAuthToken(tok);
                    // Validate token in background; if invalid, clear both common and auth stores
                    javafx.concurrent.Task<Boolean> task = new javafx.concurrent.Task<>() {
                        @Override
                        protected Boolean call() {
                            return com.tracersoftware.api.ApiClient.pingAuth();
                        }
                    };
                    task.setOnSucceeded(ev -> {
                        Boolean ok = task.getValue();
                        if (!Boolean.TRUE.equals(ok)) {
                            com.tracersoftware.common.SessionManager.clear();
                            try { com.tracersoftware.auth.TokenStore.clearAll(); } catch (Exception ignore) {}
                        } else {
                            // populate auth.SessionManager in-memory with the persisted token
                            try { com.tracersoftware.auth.SessionManager.get().setSession(tok, null, null, new String[0], com.tracersoftware.auth.TokenStore.loadRefreshToken()); } catch (Exception ignore) {}
                        }
                    });
                    new Thread(task).start();
                }
            } catch (Exception ex) {
                // ignore token loading errors
            }
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al iniciar la aplicación");
            alert.setHeaderText("No se pudo iniciar la aplicación");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
