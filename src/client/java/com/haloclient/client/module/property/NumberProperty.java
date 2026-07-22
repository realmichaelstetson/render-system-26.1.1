package com.haloclient.client.module.property;

public class NumberProperty extends Property<Double> {
    private final double min;
    private final double max;
    private final double step;

    public NumberProperty(String name, String description, double min, double max, double defaultValue, double step) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public NumberProperty(String name, double min, double max, double defaultValue, double step) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    @Override
    public void setValue(Double value) {
        double clamped = Math.max(min, Math.min(max, value));
        double rounded = Math.round(clamped / step) * step;
        super.setValue(rounded);
    }
}
