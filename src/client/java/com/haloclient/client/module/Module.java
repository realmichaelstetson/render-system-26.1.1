package com.haloclient.client.module;

import com.haloclient.client.module.property.Property;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import com.haloclient.client.event.EventBus;
import com.haloclient.client.event.events.SuffixEvent;
import com.haloclient.client.module.property.ComboBoxProperty;
import com.haloclient.client.module.property.ModeProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Module {
    protected static final Minecraft mc = Minecraft.getInstance();
    
    private final String name;
    private final String description;
    private final Category category;
    private int key;
    private boolean enabled;
    private boolean expanded = false;
    private String suffix;
    private final List<Property<?>> properties = new ArrayList<>();

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.key = 0;
        this.enabled = false;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public List<Property<?>> getProperties() {
        return properties;
    }

    public void addProperties(Property<?>... properties) {
        this.properties.addAll(Arrays.asList(properties));
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getSuffix() {
        String currentSuffix = this.suffix;

        if (currentSuffix == null) {
            for (Property<?> property : properties) {
                if (property.getName().equalsIgnoreCase("Mode") || property.getName().equalsIgnoreCase("Type")) {
                    if (property instanceof ModeProperty modeProperty) {
                        currentSuffix = modeProperty.getValue();
                        break;
                    } else if (property instanceof ComboBoxProperty comboBoxProperty) {
                        currentSuffix = comboBoxProperty.getValue();
                        break;
                    }
                }
            }
        }

        SuffixEvent event = new SuffixEvent(this, currentSuffix);
        EventBus.getInstance().post(event);

        return event.getSuffix();
    }

    /** Called every client tick when module is enabled. */
    public void onTick() {}

    public void onRender(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {}

    protected void onEnable() {}
    protected void onDisable() {}
}
