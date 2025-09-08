package com.tracersoftware.common.ui;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Helper to show undecorated modals with consistent styling, drag, and animation.
 */
public final class ModalUtils {
    private ModalUtils() {}

    public static void showModalAndWait(Stage owner, Parent root, String title) {
        Stage st = new Stage();
        if (title != null) st.setTitle(title);
        Scene sc = new Scene(root);
        try { sc.getStylesheets().add(ModalUtils.class.getResource("/com/tracersoftware/common/controls/buttons.css").toExternalForm()); } catch (Exception ignored) {}
        try { sc.getStylesheets().add(ModalUtils.class.getResource("/com/tracersoftware/common/controls/modal.css").toExternalForm()); } catch (Exception ignored) {}
        st.initStyle(StageStyle.UNDECORATED);
        st.setResizable(false);
        if (owner != null) st.initOwner(owner);
        st.initModality(Modality.APPLICATION_MODAL);
        st.setScene(sc);

        enableDragging(st, root);
        playEnterAnimation(root);
        st.showAndWait();
    }

    private static void enableDragging(Stage stage, Parent root) {
        try {
            final double[] dragDelta = new double[2];
            javafx.scene.Node dragHandle = root.lookup(".modal-accent");
            if (dragHandle == null) dragHandle = root;
            final javafx.scene.Node handle = dragHandle;
            handle.setOnMousePressed(e -> {
                dragDelta[0] = stage.getX() - e.getScreenX();
                dragDelta[1] = stage.getY() - e.getScreenY();
            });
            handle.setOnMouseDragged(e -> {
                stage.setX(e.getScreenX() + dragDelta[0]);
                stage.setY(e.getScreenY() + dragDelta[1]);
            });
        } catch (Exception ignored) {}
    }

    private static void playEnterAnimation(Parent root) {
        try {
            root.setOpacity(0);
            root.setTranslateY(12);
            FadeTransition fade = new FadeTransition(Duration.millis(180), root);
            fade.setFromValue(0);
            fade.setToValue(1);
            TranslateTransition slide = new TranslateTransition(Duration.millis(180), root);
            slide.setFromY(12);
            slide.setToY(0);
            fade.play();
            slide.play();
        } catch (Exception ignored) {}
    }
}

