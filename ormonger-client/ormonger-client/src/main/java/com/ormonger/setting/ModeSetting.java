package com.ormonger.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Picks one value out of a fixed list of named modes (e.g. ElytraFly: "Control" or "Packet").
 */
public class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String description, String defaultMode, String... modes) {
        super(name, description, defaultMode);
        this.modes = Collections.unmodifiableList(Arrays.asList(modes));
        if (!this.modes.contains(defaultMode)) {
            throw new IllegalArgumentException("Default mode '" + defaultMode + "' is not in the mode list of " + name);
        }
    }

    public List<String> getModes() {
        return modes;
    }

    public boolean is(String mode) {
        return get().equalsIgnoreCase(mode);
    }

    /** Moves to the next mode, wrapping around at the end. Used by the GUI on click. */
    public void cycle() {
        int index = modes.indexOf(get());
        set(modes.get((index + 1) % modes.size()));
    }

    @Override
    protected String sanitize(String newValue) {
        if (newValue != null) {
            for (String mode : modes) {
                if (mode.equalsIgnoreCase(newValue)) {
                    return mode;
                }
            }
        }
        return getDefault();
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            set(element.getAsString());
        }
    }
}
