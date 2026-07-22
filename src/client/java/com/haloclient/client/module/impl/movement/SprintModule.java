package com.haloclient.client.module.impl.movement;

import com.haloclient.client.module.Category;
import com.haloclient.client.module.Module;
import com.haloclient.client.module.property.ComboBoxProperty;

public class SprintModule extends Module {
    private final ComboBoxProperty mode = new ComboBoxProperty("Mode", "Legit", "Legit", "Omni");

    public SprintModule() {
        super("Sprint", "Forces automatic sprinting", Category.MOVEMENT);
        addProperties(mode);
    }

    public ComboBoxProperty getMode() {
        return mode;
    }

    public boolean isOmniSprint() {
        return isEnabled() && "Omni".equalsIgnoreCase(mode.getValue());
    }
}
