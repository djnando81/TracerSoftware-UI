package com.tracersoftware.common.controls;

import javafx.animation.TranslateTransition;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class SwitchToggleButtonSkin extends SkinBase<SwitchToggleButton> {
    private final StackPane track;
    private final Circle thumb;
    private final TranslateTransition slide;

    public SwitchToggleButtonSkin(SwitchToggleButton control) {
        super(control);
        track = new StackPane();
        track.setPrefSize(44, 22);
        track.setStyle("-fx-background-radius: 11; -fx-background-color: #bdbdbd;");

        thumb = new Circle(11, Color.WHITE);
        thumb.setStroke(Color.LIGHTGRAY);
        thumb.setStrokeWidth(1.2);
        thumb.setCenterX(11);
        thumb.setCenterY(11);
        thumb.setEffect(new javafx.scene.effect.DropShadow(2, Color.rgb(0,0,0,0.18)));

        StackPane root = new StackPane(track, thumb);
        root.setPrefSize(44, 22);
        getChildren().add(root);

    slide = new TranslateTransition(Duration.millis(120), thumb);

    control.switchedOnProperty().addListener((obs, oldVal, newVal) -> animateSwitch(newVal));
    root.setOnMouseClicked(e -> control.setSwitchedOn(!control.isSwitchedOn()));
    animateSwitch(control.isSwitchedOn());
    }

    private void animateSwitch(boolean on) {
        if (on) {
            slide.setToX(22);
            track.setStyle("-fx-background-radius: 11; -fx-background-color: #43a047;");
        } else {
            slide.setToX(0);
            track.setStyle("-fx-background-radius: 11; -fx-background-color: #bdbdbd;");
        }
        slide.play();
    }
}
