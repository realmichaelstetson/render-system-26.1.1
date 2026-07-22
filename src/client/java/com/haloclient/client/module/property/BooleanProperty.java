package com.haloclient.client.module.property;

public class BooleanProperty extends Property<Boolean> {
    public BooleanProperty(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }

    public BooleanProperty(String name, boolean defaultValue) {
        super(name, defaultValue);
    }

    public void toggle() {
        setValue(!getValue());
    }
}
