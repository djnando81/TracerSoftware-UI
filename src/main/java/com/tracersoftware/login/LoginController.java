package com.tracersoftware.login;


import com.tracersoftware.apiinfo.ApiService;
import com.tracersoftware.common.ConfigManager;
import com.tracersoftware.common.SessionManager;
// use com.tracersoftware.auth.SessionManager via fully-qualified calls to avoid collision
import com.tracersoftware.common.controls.MessageToast;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
// unused imports removed
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.stage.Stage;
// java.io.IOException not used here

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField passwordVisibleField;
    @FXML
    private Button togglePasswordButton;
    @FXML
    private Button loginButton;
    @FXML
    private Button closeButton;
    @FXML
    private javafx.scene.control.Label apiStatusLabel;
    @FXML
    private Button reconnectButton;

    private ApiService apiService;

    public void initialize() {
        // Inicializar ApiService con la URL base
        String urlBase = ConfigManager.getUrlBase();
        apiService = new ApiService(urlBase);
        // Verificar conectividad con la API en un hilo aparte para no bloquear la carga
        new Thread(() -> {
            boolean apiOk = apiService.pingApi();
            javafx.application.Platform.runLater(() -> updateApiStatus(apiOk));
        }).start();

        // Sincronizar campos de contraseña
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!passwordVisibleField.isVisible()) {
                passwordVisibleField.setText(newVal);
            }
        });
        passwordVisibleField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (passwordVisibleField.isVisible()) {
                passwordField.setText(newVal);
            }
        });
        togglePasswordButton.setOnAction(e -> togglePasswordVisibility());
        // Inicialmente ocultar campo visible
        passwordVisibleField.setVisible(false);
        passwordVisibleField.setManaged(false);
    }

    private void togglePasswordVisibility() {
        boolean show = !passwordVisibleField.isVisible();
        passwordVisibleField.setVisible(show);
        passwordVisibleField.setManaged(show);
        passwordField.setVisible(!show);
        passwordField.setManaged(!show);
        if (show) {
            passwordVisibleField.setText(passwordField.getText());
            togglePasswordButton.setText("🙈");
        } else {
            passwordField.setText(passwordVisibleField.getText());
            togglePasswordButton.setText("👁");
        }
    }
    
    @FXML
    private void onClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onLogin() {
        String username = usernameField.getText();
    String password = passwordField.isVisible() ? passwordField.getText() : passwordVisibleField.getText();
        Stage stage = (Stage) loginButton.getScene().getWindow();
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            MessageToast.show(stage, "Ingrese usuario y contraseña", MessageToast.ToastType.WARNING);
            return;
        }
        // Mostrar toast de proceso
        MessageToast.show(stage, "Verificando credenciales...", MessageToast.ToastType.INFO);
        // debug: record username and start
        try {
            java.nio.file.Path dbg = java.nio.file.Paths.get("debug_auth.log");
            String line = java.time.ZonedDateTime.now().toString() + " | UI LOGIN ATTEMPT | user=" + username + System.lineSeparator();
            java.nio.file.Files.write(dbg, line.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {}

        boolean loginOk = authenticate(username, password);
        // debug: record login result and API last response
        try {
            java.nio.file.Path dbg2 = java.nio.file.Paths.get("debug_auth.log");
            String lastJson = apiService.getLastLoginResponseJson();
            String line2 = java.time.ZonedDateTime.now().toString() + " | UI LOGIN RESULT | user=" + username + " | ok=" + loginOk + " | lastJson=" + (lastJson == null ? "(null)" : lastJson) + System.lineSeparator();
            java.nio.file.Files.write(dbg2, line2.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
        if (loginOk) {
            MessageToast.show(stage, "¡Bienvenido!", MessageToast.ToastType.SUCCESS);
                // Persist tokens via the auth.SessionManager (no org.json helper used here)
                try {
                    String access = apiService.getAuthToken();
                    if (access != null) access = access.replaceFirst("(?i)^Bearer\\s+", "").trim();
                    try {
                        com.tracersoftware.auth.SessionManager.get().setSession(access, null, username, new String[0], null);
                    } catch (Exception ignored) {}
                    // Ensure persistence: also write tokens directly to TokenStore (defensive)
                    try {
                        String refresh = null;
                        try {
                            String json = apiService.getLastLoginResponseJson();
                            if (json != null && !json.isBlank()) {
                                com.fasterxml.jackson.databind.JsonNode r = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
                                if (r.has("refreshToken")) refresh = r.path("refreshToken").asText(null);
                                if (refresh == null && r.has("refresh_token")) refresh = r.path("refresh_token").asText(null);
                            }
                        } catch (Exception ignore) {}
                        try { com.tracersoftware.auth.TokenStore.saveTokens(access, refresh); } catch (Exception ignored) {}
                    } catch (Exception ignored) {}
                } catch (Exception ignored) {}
            goToDashboard();
        } else {
            MessageToast.show(stage, "Usuario o contraseña incorrectos", MessageToast.ToastType.ERROR);
        }
    }


    private void updateApiStatus(boolean apiOk) {
        if (!apiOk) {
            apiStatusLabel.setText("Estado API: Sin conexión");
            apiStatusLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold; -fx-background-color: #fff0f0; -fx-border-radius: 6; -fx-border-color: #e57373; -fx-border-width: 1; -fx-alignment: center;");
            reconnectButton.setVisible(true);
            MessageToast.show((Stage) loginButton.getScene().getWindow(), "No hay comunicación con la API", MessageToast.ToastType.ERROR);
        } else {
            apiStatusLabel.setText("Estado API: Conectado");
            apiStatusLabel.setStyle("-fx-text-fill: #388e3c; -fx-font-weight: bold; -fx-background-color: #f4fff4; -fx-border-radius: 6; -fx-border-color: #81c784; -fx-border-width: 1; -fx-alignment: center;");
            reconnectButton.setVisible(false);
        }
    }

    @FXML
    private void onReconnect() {
        boolean apiOk = apiService.pingApi();
        updateApiStatus(apiOk);
        if (apiOk) {
            MessageToast.show((Stage) loginButton.getScene().getWindow(), "¡Conexión restablecida con la API!", MessageToast.ToastType.SUCCESS);
        }
    }

    private boolean authenticate(String username, String password) {
        boolean ok = apiService.login(username, password);
        if (ok) {
            SessionManager.setAuthToken(apiService.getAuthToken());
            SessionManager.setUsername(username);
            // Try to set a default avatar from resources: /images/avatars/{username}.png
            try {
                String candidate = "/images/avatars/" + username + ".png";
                if (getClass().getResource(candidate) != null) {
                    SessionManager.setAvatarUrl(candidate);
                } else {
                    SessionManager.setAvatarUrl(null);
                }
            } catch (Exception ignored) {
                SessionManager.setAvatarUrl(null);
            }
            // Try to create or fetch an API key for persistent use
            try {
                // Persist the JWT immediately via auth.SessionManager so legacy key.ini is not used.
                String jwtNow = apiService.getAuthToken();
                    try {
                        if (jwtNow != null && !jwtNow.isEmpty()) {
                            com.tracersoftware.auth.SessionManager.get().setSession(jwtNow, null, username, new String[0], null);
                        }
                    } catch (Exception ignore) {}
                    try {
                        java.nio.file.Path dbgInit = java.nio.file.Paths.get("debug_auth.log");
                        String lineInit = java.time.ZonedDateTime.now().toString() + " | initial save JWT -> setSession invoked" + System.lineSeparator();
                        java.nio.file.Files.write(dbgInit, lineInit.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                    } catch (Exception ignore) {}

                // Now attempt to create an API key; if successful, replace persisted token with apikey
                String apikey = apiService.createApiKey();
                // log attempt result for debugging
                try {
                    java.nio.file.Path dbg = java.nio.file.Paths.get("debug_auth.log");
                    String line = java.time.ZonedDateTime.now().toString() + " | createApiKey -> " + (apikey == null ? "<null>" : "<len=" + apikey.length() + ">") + System.lineSeparator();
                    java.nio.file.Files.write(dbg, line.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                } catch (Exception ignore) {}

                    if (apikey != null && !apikey.isEmpty()) {
                        // prefer not to use apikey persistence in legacy key.ini; instead keep JWT or apikey
                        try {
                            com.tracersoftware.auth.SessionManager.get().setSession(apikey, null, username, new String[0], null);
                            com.tracersoftware.common.SessionManager.setAuthToken(apikey);
                            MessageToast.show((Stage) loginButton.getScene().getWindow(), "API key creada y persistida.", MessageToast.ToastType.SUCCESS);
                        } catch (Exception ignore) {
                            MessageToast.show((Stage) loginButton.getScene().getWindow(), "API key recibida pero no pudo persistirse.", MessageToast.ToastType.WARNING);
                        }
                    } else {
                    // no apikey; keep the JWT in session
                    try { com.tracersoftware.common.SessionManager.setAuthToken(jwtNow); } catch (Exception ignore) {}
                    MessageToast.show((Stage) loginButton.getScene().getWindow(), "No hubo apikey; JWT en sesión.", MessageToast.ToastType.SUCCESS);
                }
            } catch (Exception ignored) {}
        }
        return ok;
    }

    private void goToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent dashboardRoot = loader.load();
            Scene dashboardScene = new Scene(dashboardRoot);
            // Agregar estilos globales
            dashboardScene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());
            dashboardScene.getStylesheets().add(getClass().getResource("/com/tracersoftware/common/controls/buttons.css").toExternalForm());
            Stage stage = (Stage) loginButton.getScene().getWindow();
            // Siempre iniciar en fullscreen y con resolución mínima
            stage.setFullScreen(true);
            stage.setMinWidth(1280);
            stage.setMinHeight(720);
            stage.setScene(dashboardScene);
        } catch (Exception e) {
            e.printStackTrace();
            StringBuilder msg = new StringBuilder();
            msg.append("No se pudo cargar el dashboard: ")
               .append(e.getClass().getSimpleName())
               .append(" - ")
               .append(e.getMessage() != null ? e.getMessage() : "Sin mensaje");
            if (e.getCause() != null) {
                msg.append("\nCausa: ").append(e.getCause().toString());
            }
            for (StackTraceElement ste : e.getStackTrace()) {
                msg.append("\n    at ").append(ste.toString());
            }
            MessageToast.showSystemError((Stage) loginButton.getScene().getWindow(), msg.toString());
        }
    }

    // showError eliminado, se usa MessageToast
}
