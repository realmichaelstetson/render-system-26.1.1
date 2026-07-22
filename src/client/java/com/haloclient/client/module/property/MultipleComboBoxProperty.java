package com.haloclient.client.module.property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultipleComboBoxProperty extends Property<List<String>> {
    private final List<String> options;

    public MultipleComboBoxProperty(String name, String description, List<String> defaultValues, String... options) {
        super(name, description, new ArrayList<>(defaultValues));
        this.options = Arrays.asList(options);
        for (String val : defaultValues) {
            if (!this.options.contains(val)) {
                throw new IllegalArgumentException("Default value " + val + " must be one of the options");
            }
        }
    }

    public MultipleComboBoxProperty(String name, List<String> defaultValues, String... options) {
        this(name, "", defaultValues, options);
    }

    public List<String> getOptions() {
        return options;
    }

    public boolean isSelected(String option) {
        return getValue() != null && getValue().contains(option);
    }

    public void toggle(String option) {
        List<String> current = new ArrayList<>(getValue());
        if (current.contains(option)) {
            current.remove(option);
        } else {
            current.add(option);
        }
        setValue(current);
    }

    public void setSelected(String option, boolean selected) {
        List<String> current = new ArrayList<>(getValue());
        if (selected && !current.contains(option)) {
            current.add(option);
        } else if (!selected && current.contains(option)) {
            current.remove(option);
        }
        setValue(current);
    }

    public String getFormattedValue() {
        List<String> selected = getValue();
        if (selected == null || selected.isEmpty()) {
            return "None";
        }
        return String.join(", ", selected);
    }
}
