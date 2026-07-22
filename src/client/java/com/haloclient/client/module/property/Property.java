package com.haloclient.client.module.property;

import java.util.function.Supplier;

public class Property<T> {
    private final String name;
    private final String description;
    private T value;
    private Supplier<Boolean> visible = () -> true;

    public Property(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
    }

    public Property(String name, T defaultValue) {
        this(name, "", defaultValue);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public boolean isVisible() {
        return visible.get();
    }

    public Property<T> setVisible(Supplier<Boolean> visible) {
        this.visible = visible;
        return this;
    }
}
