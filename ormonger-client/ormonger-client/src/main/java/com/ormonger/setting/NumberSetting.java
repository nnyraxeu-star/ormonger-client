package com.ormonger.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A number clamped to [min, max] and snapped to {@code step}.
 * Stored as a double; use {@link #intValue()} / {@link #floatValue()} for convenience.
 */
public class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String name, String description, double defaultValue, double min, double max, double step) {
        super(name, description, defaultValue);
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

    public int intValue() {
        return (int) Math.round(get());
    }

    public float floatValue() {
        return get().floatValue();
    }

    @Override
    protected Double sanitize(Double newValue) {
        double v = newValue == null ? getDefault() : newValue;
        if (step > 0) {
            v = min + Math.round((v - min) / step) * step;
        }
        return Math.max(min, Math.min(max, v));
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            set(element.getAsDouble());
        }
    }
}
