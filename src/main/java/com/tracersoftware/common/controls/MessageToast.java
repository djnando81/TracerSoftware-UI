package com.tracersoftware.common.controls;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.stage.Window;
import javafx.stage.Screen;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MessageToast {
    public enum ToastType {
        SUCCESS, ERROR, INFO, WARNING, SYSTEM_ERROR
    }

    public static void show(Stage owner, String message, ToastType type) {
        show(owner, message, type, false);
    }

    public static void showSystemError(Stage owner, String message) {
        show(owner, message, ToastType.SYSTEM_ERROR, true);
    }

    private static void show(Stage owner, String message, ToastType type, boolean isSystemError) {
        Platform.runLater(() -> {
            Popup popup = new Popup();

            // Icono según tipo
            Label icon = new Label(getIcon(type));
            icon.setFont(Font.font("Segoe UI Emoji", FontWeight.NORMAL, 16));
            icon.setTextFill(Color.WHITE);
            icon.setPadding(new Insets(0, 10, 0, 0));

            Label label = new Label(message);
            label.setFont(Font.font("Segoe UI", FontWeight.NORMAL, isSystemError ? 14 : 13));
            label.setTextFill(Color.WHITE);
            label.setPadding(new Insets(12, isSystemError ? 48 : 32, 12, 0));
            label.setWrapText(isSystemError);
            label.setMaxWidth(isSystemError ? 600 : 340);

            HBox hbox = new HBox(icon, label);
            hbox.setAlignment(Pos.CENTER_LEFT);

            StackPane root = new StackPane(hbox);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setStyle("-fx-background-radius: 12; -fx-background-color: " + getColor(type) + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 16,0,0,4); -fx-opacity: 0.97;");
            root.setPadding(new Insets(0, 0, 24, 0));

            popup.getContent().add(root);
            popup.setAutoFix(true);
            popup.setAutoHide(true);
            popup.setHideOnEscape(true);

            // Determine owner window safely. If caller passed null, try to find a showing window.
            Window ownerWindow = owner;
            if (ownerWindow == null) {
                ownerWindow = Window.getWindows().stream().filter(Window::isShowing).findFirst().orElse(null);
            }

            double width = isSystemError ? 640 : 340;
            double x, y;
            root.setPrefWidth(width);

            if (ownerWindow != null && ownerWindow.getScene() != null) {
                Scene scene = ownerWindow.getScene();
                x = ownerWindow.getX() + (scene.getWidth() - width) / 2;
                y = ownerWindow.getY() + scene.getHeight() * 0.85;
                popup.show(ownerWindow, x, y);
            } else {
                // No active window found — position relative to primary screen.
                Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
                x = bounds.getMinX() + (bounds.getWidth() - width) / 2;
                y = bounds.getMinY() + bounds.getHeight() * 0.85;
                popup.show((Window) null, x, y);
            }

            // Animación slide in + fade in
            root.setOpacity(0);
            root.setTranslateY(40);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(220), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(220), root);
            slideIn.setFromY(40);
            slideIn.setToY(0);
            fadeIn.play();
            slideIn.play();

            // Animación de salida
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), root);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setDelay(Duration.seconds(isSystemError ? 6 : 2.5));
            fadeOut.setOnFinished(e -> popup.hide());
            fadeOut.play();
        });
    }

    private static String getColor(ToastType type) {
        switch (type) {
            case SUCCESS:
                return "rgba(67,160,71,0.97)"; // Verde
            case ERROR:
                return "rgba(229,57,53,0.97)"; // Rojo
            case INFO:
                return "rgba(30,136,229,0.97)"; // Azul
            case WARNING:
                return "rgba(251,192,45,0.97)"; // Amarillo
            case SYSTEM_ERROR:
                return "rgba(255,140,0,0.97)"; // Naranja fuerte
            default:
                return "rgba(51,51,51,0.97)";
        }
    }

    private static String getIcon(ToastType type) {
        switch (type) {
            case SUCCESS:
                return "\u2714"; // ✓
            case ERROR:
                return "\u26A0"; // ⚠
            case INFO:
                return "\u2139"; // ℹ
            case WARNING:
                return "\u26A0"; // ⚠
            default:
                return "";
        }
    }
}
