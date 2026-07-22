package com.haloclient.client.module.property;

import java.util.ArrayList;
import java.util.List;

public class GroupProperty extends Property<Boolean> {
    private final List<Property<?>> properties = new ArrayList<>();

    public GroupProperty(String name, String description) {
        super(name, description, false);
    }

    public GroupProperty(String name) {
        this(name, "");
    }

    public GroupProperty addProperty(Property<?> property) {
        properties.add(property);
        return this;
    }

    public GroupProperty addProperties(Property<?>... props) {
        for (Property<?> p : props) {
            properties.add(p);
        }
        return this;
    }

    public List<Property<?>> getProperties() {
        return properties;
    }
}
