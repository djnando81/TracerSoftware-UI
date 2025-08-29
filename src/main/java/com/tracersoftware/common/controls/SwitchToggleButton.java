package com.tracersoftware.common.controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;

public class SwitchToggleButton extends Control {
    private final BooleanProperty switchedOn = new SimpleBooleanProperty(false);

    public SwitchToggleButton() {
        getStyleClass().add("switch-toggle-button");
    }

    public BooleanProperty switchedOnProperty() {
        return switchedOn;
    }

    public boolean isSwitchedOn() {
        return switchedOn.get();
    }

    public void setSwitchedOn(boolean value) {
        switchedOn.set(value);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new SwitchToggleButtonSkin(this);
    }
}
