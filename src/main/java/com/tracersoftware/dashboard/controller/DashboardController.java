package com.tracersoftware.dashboard.controller;

import com.tracersoftware.ui.menu.DefaultCurrentUserProvider;
import com.tracersoftware.ui.menu.MenuBarBuilder;
import com.tracersoftware.ui.menu.MenuLoader;
import com.tracersoftware.ui.menu.MenuModel;
import com.tracersoftware.common.controls.MessageToast;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class DashboardController {

    @FXML
    private Label salesLabel;

    @FXML
    private ScrollPane mainScroll;

    @FXML
    private GridPane mainGrid;

    @FXML
    private BorderPane rootPane;

    @FXML
    private Label lblCurrentUser;

    // static buttons from dashboard.fxml (kept for users who open the original layout)
    @FXML
    private javafx.scene.control.Button btnStaticDashboard;
    @FXML
    private javafx.scene.control.Button btnStaticUsuarios;
    @FXML
    private javafx.scene.control.Button btnStaticReportes;
    @FXML
    private javafx.scene.control.Button btnStaticSettings;

    @FXML
    public void initialize() {
        // Establecer texto con símbolo monetario desde Java para evitar parsing en FXML
        if (salesLabel != null) {
            salesLabel.setText("$508");
        }
        // Forzar que el GridPane ocupe el ancho disponible del ScrollPane
        if (mainScroll != null && mainGrid != null) {
            mainGrid.prefWidthProperty().bind(mainScroll.widthProperty().subtract(48));
        }

        // Montar menú lateral aditivo desde manifest JSON
        try {
            MenuLoader loader = new MenuLoader();
            MenuModel.MenuManifest manifest = loader.loadFromResource("/menu-manifest.json");
            MenuBarBuilder builder = new MenuBarBuilder();
        // Use SessionManager username/display name when available
        String username = com.tracersoftware.common.SessionManager.getUsername();
        DefaultCurrentUserProvider userProvider = new DefaultCurrentUserProvider(
            username == null ? "guest" : username,
            username == null ? "Invitado" : username
        );

            // Build dynamic sidebar
            VBox sidebar = builder.build(manifest, userProvider,
                    // navigate callback -> intentará cargar /fxml/{routeName}.fxml y marcar botón activo
                    route -> {
                        try {
                                if (route == null || route.isEmpty()) return;
                                // normalizar: route puede venir como "/dashboard" o "dashboard"
                                String name = route.startsWith("/") ? route.substring(1) : route;
                                String fxmlPath = "/fxml/" + name + ".fxml";
                                System.out.println("[DashboardController] navigate received route='" + route + "', normalized='" + name + "', trying fxml='" + fxmlPath + "'");
                                try { MessageToast.show(null, "Cargando: " + name, MessageToast.ToastType.INFO); } catch (Exception ignored) {}
                                if (getClass().getResource(fxmlPath) != null) {
                                    System.out.println("[DashboardController] resource found: " + fxmlPath);
                                    javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
                                    Node center = fxmlLoader.load();
                                    // If controller implements lifecycle hook, call onViewShown()
                                    Object ctrl = fxmlLoader.getController();
                                    try {
                                        if (ctrl instanceof com.tracersoftware.ui.ViewLifecycle) {
                                            ((com.tracersoftware.ui.ViewLifecycle) ctrl).onViewShown();
                                        }
                                    } catch (Exception ignore) {}
                                    System.out.println("[DashboardController] fxml loaded successfully: " + fxmlPath);
                                    if (rootPane != null) rootPane.setCenter(center);
                                // marcar activo en sidebar
                                if (rootPane != null && rootPane.getLeft() instanceof VBox) {
                                    VBox left = (VBox) rootPane.getLeft();
                                    left.getChildren().forEach(n -> {
                                        if (n instanceof javafx.scene.control.Button) {
                                            javafx.scene.control.Button btn = (javafx.scene.control.Button) n;
                                            btn.getStyleClass().remove("active");
                                            Object ud = btn.getUserData();
                                            if (ud != null && ud.equals(ruleNormaliseRoute(route))) {
                                                if (!btn.getStyleClass().contains("active")) btn.getStyleClass().add("active");
                                            }
                                        }
                                        // handle boxes with children
                                        if (n instanceof VBox) {
                                            ((VBox) n).getChildren().forEach(cn -> {
                                                if (cn instanceof javafx.scene.control.Button) {
                                                    javafx.scene.control.Button cbtn = (javafx.scene.control.Button) cn;
                                                    cbtn.getStyleClass().remove("active");
                                                    Object ud = cbtn.getUserData();
                                                    if (ud != null && ud.equals(ruleNormaliseRoute(route))) {
                                                        if (!cbtn.getStyleClass().contains("active")) cbtn.getStyleClass().add("active");
                                                    }
                                                }
                                            });
                                        }
                                    });
                                }
                            } else {
                                MessageToast.show(null, "Pantalla no disponible: " + name, MessageToast.ToastType.INFO);
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            MessageToast.showSystemError(null, "Error cargando pantalla: " + ex.getMessage());
                        }
                    },
                    // action callback
                    action -> {
                        System.out.println("Execute action: " + action);
                        if ("openSettings".equals(action)) {
                            MessageToast.show(null, "Ajustes no implementados aún.", MessageToast.ToastType.INFO);
                        }
                    }
            );

            // Reemplaza el panel izquierdo existente solo si rootPane y sidebar existen
            if (rootPane != null && sidebar != null) {
                rootPane.setLeft(sidebar);
            }

            // Mostrar el usuario actual en el topbar si existe el label
            try {
                if (lblCurrentUser != null) {
                    String sessionUser = com.tracersoftware.common.SessionManager.getUsername();
                    if (sessionUser != null && !sessionUser.isEmpty()) {
                        lblCurrentUser.setText(sessionUser);
                    }
                }
            } catch (Exception ignored) {}

            // Enlazar botones estáticos a la misma navegación que usa el sidebar (rutas del manifest)
            try {
                if (btnStaticDashboard != null) btnStaticDashboard.setOnAction(evt -> navigateTo("dashboard"));
                if (btnStaticUsuarios != null) btnStaticUsuarios.setOnAction(evt -> navigateTo("usuarios_list"));
                if (btnStaticReportes != null) btnStaticReportes.setOnAction(evt -> navigateTo("reports"));
                if (btnStaticSettings != null) btnStaticSettings.setOnAction(evt -> executeAction("openSettings"));
            } catch (Exception ignored) {}
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Public helper used by static buttons to reuse the same navigation logic
    private void navigateTo(String route) {
        try {
            if (route == null || route.isEmpty()) return;
            String name = route.startsWith("/") ? route.substring(1) : route;
            String fxmlPath = "/fxml/" + name + ".fxml";
            System.out.println("[DashboardController] navigateTo route='" + route + "' -> fxml='" + fxmlPath + "'");
            try { MessageToast.show(null, "Cargando: " + name, MessageToast.ToastType.INFO); } catch (Exception ignored) {}
            if (getClass().getResource(fxmlPath) != null) {
                javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
                Node center = fxmlLoader.load();
                // First set the loaded node into the center so it's attached to the scene graph
                if (rootPane != null) rootPane.setCenter(center);
                // If controller implements lifecycle hook, call onViewShown() now that the node is attached
                try {
                    Object ctrl = fxmlLoader.getController();
                    if (ctrl instanceof com.tracersoftware.ui.ViewLifecycle) {
                        ((com.tracersoftware.ui.ViewLifecycle) ctrl).onViewShown();
                    }
                } catch (Exception ignore) {}
            } else {
                MessageToast.show(null, "Pantalla no disponible: " + name, MessageToast.ToastType.INFO);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            MessageToast.showSystemError(null, "Error cargando pantalla: " + ex.getMessage());
        }
    }

    private void executeAction(String action) {
        if ("openSettings".equals(action)) {
            MessageToast.show(null, "Ajustes no implementados aún.", MessageToast.ToastType.INFO);
        }
    }

    public void handleLogout() {
        // Aquí puedes agregar lógica para cerrar sesión, volver al login, etc.
        System.out.println("Logout ejecutado desde el botón ⎋");
    }

    private String ruleNormaliseRoute(String route) {
        if (route == null) return null;
        return route.startsWith("/") ? route.substring(1) : route;
    }
}
