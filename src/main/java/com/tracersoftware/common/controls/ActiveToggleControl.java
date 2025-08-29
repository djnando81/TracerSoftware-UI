package com.tracersoftware.common.controls;

import com.tracersoftware.usuarios.api.UsuariosApiService;
import com.tracersoftware.usuarios.model.UsuarioDTO;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Reusable styled toggle switch for the user's activo state.
 * Visual is a track + knob. Clicking toggles state, performs API call,
 * and reverts on failure.
 */
public class ActiveToggleControl extends HBox {
    private final StackPane trackPane = new StackPane();
    private final Region track = new Region();
    private final Region knob = new Region();
    private final UsuariosApiService api = new UsuariosApiService();
    private UsuarioDTO currentUser;
    private boolean selected = false;
    // compact sizes (reduced so control fits typical table cells)
    private static final double TRACK_W = 48;
    private static final double TRACK_H = 26;
    private static final double KNOB_SIZE = 20;

    public ActiveToggleControl() {
        super();
        getStyleClass().add("active-toggle-container");

        // ensure stylesheet is available
        try {
            String sheet = getClass().getResource("/css/toggle.css").toExternalForm();
            this.getStylesheets().add(sheet);
        } catch (Exception ignored) {
            // if stylesheet not found, control still works with default look
        }

        track.getStyleClass().add("toggle-track");
        knob.getStyleClass().add("toggle-knob");

    track.setPrefSize(TRACK_W, TRACK_H);
    knob.setPrefSize(KNOB_SIZE, KNOB_SIZE);
    // keep the trackPane tight to the preferred size so it won't expand past the column cell
    trackPane.setPrefSize(TRACK_W, TRACK_H);
    trackPane.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
    trackPane.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
    this.setAlignment(javafx.geometry.Pos.CENTER);

        trackPane.getChildren().addAll(track, knob);
        getChildren().add(trackPane);

        // accessibility and input handling
        trackPane.setFocusTraversable(true);
        trackPane.setOnMouseClicked(ev -> {
            if (currentUser == null) return;
            trackPane.requestFocus();
            toggleState(!selected);
        });
        // keyboard: space or enter toggles
        trackPane.setOnKeyPressed(ev -> {
            switch (ev.getCode()) {
                case SPACE, ENTER -> {
                    if (currentUser == null) return;
                    toggleState(!selected);
                    ev.consume();
                }
                default -> {}
            }
        });
        // reflect the trackPane disabled state on the whole control so callers can bind to ctl.disabledProperty()
        trackPane.disabledProperty().addListener((obs, oldV, newV) -> this.setDisable(newV));
    }

    private void toggleState(boolean newState) {
        // disable while in-flight (apply to both internal pane and control root)
        trackPane.setDisable(true);
        this.setDisable(true);
        // optimistic UI animation
        animateKnob(newState);

        Task<Void> t = new Task<>() {
            @Override
            protected Void call() throws Exception {
                com.fasterxml.jackson.databind.node.ObjectNode payload = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
                payload.put("Activo", newState);
                api.updateUser(currentUser.getId(), payload);
                return null;
            }
        };

        t.setOnSucceeded(ev -> {
            selected = newState;
            currentUser.setActivo(newState);
            Platform.runLater(() -> {
                trackPane.setDisable(false);
                this.setDisable(false);
            });
        });

        t.setOnFailed(ev -> {
            // revert animation
            Platform.runLater(() -> {
                animateKnob(!newState);
                trackPane.setDisable(false);
                this.setDisable(false);
            });
        });

        Thread th = new Thread(t, "active-toggle"); th.setDaemon(true); th.start();
    }

    private void animateKnob(boolean on) {
        double padding = 3; // internal padding from track edges
        double offX = 0.0;
        double onX = TRACK_W - KNOB_SIZE - (padding * 2);
        TranslateTransition tt = new TranslateTransition(Duration.millis(160), knob);
        tt.setToX(on ? onX : offX);
        tt.play();
        // toggle CSS selected state on track so color changes
        if (on) track.getStyleClass().add("selected"); else track.getStyleClass().remove("selected");
        selected = on;
    }

    public void setUser(UsuarioDTO u) {
        this.currentUser = u;
        boolean sel = u != null && u.isActivo();
        // position knob to initial state without animation
        Platform.runLater(() -> {
            double padding = 3;
            if (sel) {
                double onX = TRACK_W - KNOB_SIZE - (padding * 2);
                knob.setTranslateX(onX);
                if (!track.getStyleClass().contains("selected")) track.getStyleClass().add("selected");
            } else {
                knob.setTranslateX(0);
                track.getStyleClass().remove("selected");
            }
            selected = sel;
        });
    }
}
