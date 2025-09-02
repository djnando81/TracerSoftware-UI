package com.tracersoftware.common.controls;

import javafx.animation.TranslateTransition;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class SwitchToggleButtonSkin extends SkinBase<SwitchToggleButton> {
    private final StackPane track;
    private final StackPane knob;
    private final TranslateTransition slide;

    public SwitchToggleButtonSkin(SwitchToggleButton control) {
        super(control);
        track = new StackPane();
        track.getStyleClass().add("switch-track");
        track.setPrefSize(48, 26);
        // Fallback inline style so it is visible even if CSS is not loaded
        try { track.setStyle("-fx-background-radius: 13; -fx-background-color: #e6e6e6;"); } catch (Exception ignored) {}

        knob = new StackPane();
        knob.getStyleClass().add("switch-knob");
        knob.setPrefSize(18, 18);
        try { knob.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.18), 4, 0, 0, 1);"); } catch (Exception ignored) {}

        StackPane root = new StackPane(track, knob);
        root.setPrefSize(48, 26);
        getChildren().add(root);

        slide = new TranslateTransition(Duration.millis(120), knob);

        control.switchedOnProperty().addListener((obs, oldVal, newVal) -> animateSwitch(newVal));
        // Mouse toggle and keyboard space/enter
        root.setOnMouseClicked(e -> control.setSwitchedOn(!control.isSwitchedOn()));
        root.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER:
                case SPACE:
                    control.setSwitchedOn(!control.isSwitchedOn());
                    e.consume();
                    break;
            }
        });
        animateSwitch(control.isSwitchedOn());
    }

    private void animateSwitch(boolean on) {
        if (on) {
            slide.setToX(24);
            if (!track.getStyleClass().contains("active")) track.getStyleClass().add("active");
        } else {
            slide.setToX(0);
            track.getStyleClass().remove("active");
        }
        slide.play();
    }
}
