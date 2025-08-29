package com.tracersoftware.common.controls;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ToggleButton;

/**
 * ResponsiveToggleButton
 * - Exposes an int value (0/1) that mirrors selection state
 * - Fits parent (use HGrow/VGrow or setMaxWidth/Height)
 * - Shows text "Activo" / "Inactivo"
 * - Fires a value-changed event when toggled
 */
public class ResponsiveToggleButton extends ToggleButton {
    private final IntegerProperty value = new SimpleIntegerProperty(this, "value", 0);
    private final ObjectProperty<EventHandler<ActionEvent>> onValueChanged = new SimpleObjectProperty<>();

    public ResponsiveToggleButton() {
        super("Inactivo");
        initialize();
    }

    public ResponsiveToggleButton(int initialValue) {
        super(initialValue == 1 ? "Activo" : "Inactivo");
        this.value.set(initialValue == 1 ? 1 : 0);
        initialize();
        setSelected(initialValue == 1);
    }

    private void initialize() {
        // responsive sizing
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(Double.MAX_VALUE);
        // no extra padding by default
        getStyleClass().add("responsive-toggle");

        // when the ToggleButton is clicked, update int value and text
        super.setOnAction(evt -> {
            boolean sel = isSelected();
            value.set(sel ? 1 : 0);
            setText(sel ? "Activo" : "Inactivo");
            EventHandler<ActionEvent> h = onValueChanged.get();
            if (h != null) h.handle(new ActionEvent(this, this));
        });

        // if value is changed programmatically, reflect into selected/text
        value.addListener((obs, oldV, newV) -> {
            boolean shouldBeSelected = newV != null && newV.intValue() == 1;
            if (isSelected() != shouldBeSelected) setSelected(shouldBeSelected);
            setText(shouldBeSelected ? "Activo" : "Inactivo");
        });
    }

    public final IntegerProperty valueProperty() { return value; }
    public final int getValue() { return value.get(); }
    public final void setValue(int v) { this.value.set(v == 1 ? 1 : 0); }

    // Event property (invoked after value changes due to user click)
    public final ObjectProperty<EventHandler<ActionEvent>> onValueChangedProperty() { return onValueChanged; }
    public final void setOnValueChanged(EventHandler<ActionEvent> handler) { onValueChanged.set(handler); }
    public final EventHandler<ActionEvent> getOnValueChanged() { return onValueChanged.get(); }
}
