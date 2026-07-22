package com.haloclient.client.module.property;

import java.util.Arrays;
import java.util.List;

public class ModeProperty extends Property<String> {
    private final List<String> modes;

    public ModeProperty(String name, String defaultValue, String... modes) {
        super(name, defaultValue);
        this.modes = Arrays.asList(modes);
        if (!this.modes.contains(defaultValue)) {
            throw new IllegalArgumentException("Default value must be one of the modes");
        }
    }

    public List<String> getModes() {
        return modes;
    }

    public void increment() {
        int index = modes.indexOf(getValue());
        index = (index + 1) % modes.size();
        setValue(modes.get(index));
    }

    public void decrement() {
        int index = modes.indexOf(getValue());
        index = (index - 1 + modes.size()) % modes.size();
        setValue(modes.get(index));
    }
}
