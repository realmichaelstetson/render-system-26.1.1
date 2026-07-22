package com.haloclient.client.module.property;

import net.minecraft.util.ARGB;

public class ColorPickerProperty extends Property<Integer> {
    private int color1;
    private int color2;
    private final boolean dual;

    public ColorPickerProperty(String name, String description, int defaultColor) {
        super(name, description, defaultColor);
        this.color1 = defaultColor;
        this.color2 = defaultColor;
        this.dual = false;
    }

    public ColorPickerProperty(String name, int defaultColor) {
        this(name, "", defaultColor);
    }

    public ColorPickerProperty(String name, String description, int defaultColor1, int defaultColor2) {
        super(name, description, defaultColor1);
        this.color1 = defaultColor1;
        this.color2 = defaultColor2;
        this.dual = true;
    }

    public ColorPickerProperty(String name, int defaultColor1, int defaultColor2) {
        this(name, "", defaultColor1, defaultColor2);
    }

    public int getColor1() {
        return color1;
    }

    public void setColor1(int color1) {
        this.color1 = color1;
        setValue(color1);
    }

    public int getColor2() {
        return color2;
    }

    public void setColor2(int color2) {
        this.color2 = color2;
    }

    public boolean isDual() {
        return dual;
    }

    public int getColor() {
        return color1;
    }

    public void setColor(int color) {
        setColor1(color);
    }
}
