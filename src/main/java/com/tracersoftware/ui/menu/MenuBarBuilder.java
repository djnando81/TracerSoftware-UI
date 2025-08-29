package com.tracersoftware.ui.menu;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.stage.Window;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import com.tracersoftware.common.controls.MessageToast;
import com.tracersoftware.common.controls.MessageToast.ToastType;

public class MenuBarBuilder {

    /**
     * Builds a VBox acting as a sidebar menu from the manifest.
     * navigate: called when an item has a route (route string passed)
     * action: called when an item has an action (action id passed)
     */
    public VBox build(MenuModel.MenuManifest manifest, CurrentUserProvider userProvider,
                      Consumer<String> navigate, Consumer<String> action) {

        VBox root = new VBox();
        root.getStyleClass().add("app-sidebar");
        root.setPadding(new Insets(12));
        root.setSpacing(8);

        // header with title and user (brand + avatar)
        Label title = new Label(manifest.getTitle() != null ? manifest.getTitle() : "App");
        title.getStyleClass().add("sidebar-title");

        HBox userBox = new HBox();
        userBox.setSpacing(8);
        userBox.getStyleClass().add("sidebar-user-box");

        String display = userProvider != null ? userProvider.getDisplayName() : "Guest";
        Label user = new Label(display);
        user.getStyleClass().addAll("sidebar-user", "sidebar-user-name");

        String avatarUrl = userProvider != null ? userProvider.getAvatarUrl() : null;
        if (avatarUrl != null) {
            try {
                Image img;
                if (avatarUrl.startsWith("/")) {
                    img = new Image(getClass().getResourceAsStream(avatarUrl));
                } else {
                    img = new Image(avatarUrl);
                }
                ImageView iv = new ImageView(img);
                iv.setFitWidth(40);
                iv.setFitHeight(40);
                iv.setPreserveRatio(true);
                iv.getStyleClass().add("sidebar-avatar");
                userBox.getChildren().addAll(iv, user);
            } catch (Exception ex) {
                // fallback to initials
                Label initials = new Label(display != null && !display.isEmpty() ? display.substring(0,1).toUpperCase() : "U");
                initials.getStyleClass().add("sidebar-avatar");
                userBox.getChildren().addAll(initials, user);
            }
        } else {
            Label initials = new Label(display != null && !display.isEmpty() ? display.substring(0,1).toUpperCase() : "U");
            initials.getStyleClass().add("sidebar-avatar");
            userBox.getChildren().addAll(initials, user);
        }

        root.getChildren().addAll(title, userBox);

        if (manifest.getItems() != null) {
            for (MenuModel.MenuItem it : manifest.getItems()) {
                Node node = buildItem(it, navigate, action);
                if (node != null) root.getChildren().add(node);
            }
        }

        // growing spacer to push items to top when embedded in BorderPane
        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        root.getChildren().add(spacer);

        return root;
    }

    private Node buildItem(MenuModel.MenuItem item, Consumer<String> navigate, Consumer<String> action) {
        String labelText = item.getLabel() == null ? "?" : item.getLabel();
        String iconText = item.getIcon() == null ? "" : item.getIcon();
    Button b = new Button(labelText);
        b.getStyleClass().add("sidebar-button");
        // if iconText looks like a font icon key (contains '-') use FontIcon, otherwise render text/emoji
        if (!iconText.isEmpty() && iconText.contains("-")) {
            try {
                FontIcon fi = new FontIcon(iconText);
                fi.getStyleClass().add("sidebar-icon");
                b.setGraphic(fi);
            } catch (Exception ex) {
                // Fallback: don't break UI load — show a small plain label instead
                Label fallback = new Label("•");
                fallback.getStyleClass().add("sidebar-icon");
                b.setGraphic(fallback);
            }
        } else if (!iconText.isEmpty()) {
            b.setText(iconText + "  " + labelText);
        }
    // expose id and route for external navigation/active tracking
    if (item.getId() != null) b.setId("menu-" + item.getId());
    if (item.getRoute() != null) {
        String ud = item.getRoute().startsWith("/") ? item.getRoute().substring(1) : item.getRoute();
        b.setUserData(ud);
    }
        b.getStyleClass().add("sidebar-button");
        b.setMaxWidth(Double.MAX_VALUE);
        if (item.getRoute() != null && navigate != null) {
            String routeNormalized = item.getRoute().startsWith("/") ? item.getRoute().substring(1) : item.getRoute();
            b.setOnAction(evt -> {
                System.out.println("[MenuBarBuilder] clicked route='" + routeNormalized + "' (id=" + item.getId() + ")");
                // Determine owner window from event source so the popup is attached to the app window when possible
                try {
                    Window w = ((Node) evt.getSource()).getScene().getWindow();
                    if (w instanceof Stage) MessageToast.show((Stage) w, "Ir a: " + routeNormalized, ToastType.INFO);
                    else MessageToast.show(null, "Ir a: " + routeNormalized, ToastType.INFO);
                } catch (Exception ignored) {
                    try { MessageToast.show(null, "Ir a: " + routeNormalized, ToastType.INFO); } catch (Exception ignored2) {}
                }
                navigate.accept(routeNormalized);
            });
        } else if (item.getAction() != null && action != null) {
            b.setOnAction(evt -> action.accept(item.getAction()));
        }

        // If there are children, create indented nodes (simple rendering)
        if (item.getChildren() != null && !item.getChildren().isEmpty()) {
            VBox box = new VBox();
            box.getChildren().add(b);
            for (MenuModel.MenuItem child : item.getChildren()) {
                Button cb = new Button(child.getLabel() == null ? "?" : child.getLabel());
                if (child.getIcon() != null && child.getIcon().contains("-")) {
                    try {
                        FontIcon cfi = new FontIcon(child.getIcon());
                        cfi.getStyleClass().add("sidebar-icon");
                        cb.setGraphic(cfi);
                    } catch (Exception ex) {
                        Label fallback = new Label("•");
                        fallback.getStyleClass().add("sidebar-icon");
                        cb.setGraphic(fallback);
                    }
                } else if (child.getIcon() != null) {
                    cb.setText(child.getIcon() + "  " + (child.getLabel() == null ? "?" : child.getLabel()));
                }
                if (child.getId() != null) cb.setId("menu-" + child.getId());
                if (child.getRoute() != null) {
                    String cud = child.getRoute().startsWith("/") ? child.getRoute().substring(1) : child.getRoute();
                    cb.setUserData(cud);
                }
                cb.getStyleClass().addAll("sidebar-button", "sidebar-child");
                cb.setMaxWidth(Double.MAX_VALUE);
                if (child.getRoute() != null && navigate != null) {
                    String childRouteNormalized = child.getRoute().startsWith("/") ? child.getRoute().substring(1) : child.getRoute();
                    cb.setOnAction(evt -> {
                        System.out.println("[MenuBarBuilder] clicked child route='" + childRouteNormalized + "' (id=" + child.getId() + ")");
                        try {
                            Window w = ((Node) evt.getSource()).getScene().getWindow();
                            if (w instanceof Stage) MessageToast.show((Stage) w, "Ir a: " + childRouteNormalized, ToastType.INFO);
                            else MessageToast.show(null, "Ir a: " + childRouteNormalized, ToastType.INFO);
                        } catch (Exception ignored) {
                            try { MessageToast.show(null, "Ir a: " + childRouteNormalized, ToastType.INFO); } catch (Exception ignored2) {}
                        }
                        navigate.accept(childRouteNormalized);
                    });
                }
                else if (child.getAction() != null && action != null) cb.setOnAction(evt -> action.accept(child.getAction()));
                box.getChildren().add(cb);
            }
            return box;
        }

        return b;
    }
}
