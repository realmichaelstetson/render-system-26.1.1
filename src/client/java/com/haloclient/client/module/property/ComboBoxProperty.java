package com.haloclient.client.module.property;

import java.util.Arrays;
import java.util.List;

public class ComboBoxProperty extends Property<String> {
    private final List<String> options;

    public ComboBoxProperty(String name, String description, String defaultValue, List<String> options) {
        super(name, description, defaultValue);
        this.options = options;
        if (!this.options.contains(defaultValue)) {
            throw new IllegalArgumentException("Default value must be one of the options");
        }
    }

    public ComboBoxProperty(String name, String defaultValue, String... options) {
        super(name, "", defaultValue);
        this.options = Arrays.asList(options);
        if (!this.options.contains(defaultValue)) {
            throw new IllegalArgumentException("Default value must be one of the options");
        }
    }

    public List<String> getOptions() {
        return options;
    }

    public List<String> getModes() {
        return options;
    }

    public void select(String option) {
        if (options.contains(option)) {
            setValue(option);
        }
    }

    public void increment() {
        int index = options.indexOf(getValue());
        index = (index + 1) % options.size();
        setValue(options.get(index));
    }

    public void decrement() {
        int index = options.indexOf(getValue());
        index = (index - 1 + options.size()) % options.size();
        setValue(options.get(index));
    }
}
