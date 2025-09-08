package com.tracersoftware.common.controls;

import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

/**
 * SearchBar: barra de búsqueda reutilizable (control común)
 * - expone textProperty() para escuchar/escribir el criterio
 * - incluye botón limpiar y sugerencia de placeholder "Buscar..."
 */
public class SearchBar extends HBox {
    private final TextField txt = new TextField();
    private final Button btnClear = new Button("✖");
    private final StringProperty text = new SimpleStringProperty("");

    public SearchBar() {
        super(6);
        setPadding(new Insets(0,0,0,0));
        try { getStyleClass().add("search-bar"); } catch (Exception ignored) {}
        txt.setPromptText("Buscar...");
        txt.setPrefWidth(260);
        HBox.setHgrow(txt, javafx.scene.layout.Priority.SOMETIMES);
        try { btnClear.getStyleClass().addAll("btn-small", "button-gray"); } catch (Exception ignored) {}
        btnClear.setTooltip(new Tooltip("Limpiar"));
        btnClear.setOnAction(e -> txt.clear());
        // sync internal field <-> exposed property
        txt.textProperty().addListener((o,ov,nv) -> text.set(nv==null?"":nv));
        text.addListener((o,ov,nv) -> { if (!txt.getText().equals(nv)) txt.setText(nv); });
        getChildren().addAll(txt, btnClear);
    }

    public StringProperty textProperty() { return text; }
    public String getText() { return text.get(); }
    public void setText(String v) { text.set(v); }
}

