package com.ormonger.setting;

import com.google.gson.JsonElement;

import java.util.function.Consumer;

/**
 * A single configurable value belonging to a module.
 */
public abstract class Setting<T> {
    private final String name;
    private final String description;
    private final T defaultValue;
    protected T value;
    private Consumer<T> listener;

    protected Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public T get() {
        return value;
    }

    public T getDefault() {
        return defaultValue;
    }

    public void set(T newValue) {
        this.value = sanitize(newValue);
        if (listener != null) {
            listener.accept(this.value);
        }
    }

    public void reset() {
        set(defaultValue);
    }

    /** Called whenever the value changes (from the GUI, config load or code). */
    public void setListener(Consumer<T> listener) {
        this.listener = listener;
    }

    /** Lets subclasses clamp or normalise incoming values. */
    protected T sanitize(T newValue) {
        return newValue;
    }

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement element);
}
