package com.tracersoftware.common.controls;

import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;

/**
 * Componente reutilizable para mostrar un indicador de progreso global/universal.
 */
public class GlobalProgressIndicator extends StackPane {
    private final ProgressIndicator progressIndicator;

    public GlobalProgressIndicator() {
        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(100, 100);
        this.getChildren().add(progressIndicator);
        this.setVisible(false);
        this.setStyle("-fx-background-color: rgba(0,0,0,0.3);");
    }

    public void show() {
        this.setVisible(true);
    }

    public void hide() {
        this.setVisible(false);
    }
}
