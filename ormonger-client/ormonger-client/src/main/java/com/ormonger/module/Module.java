package com.ormonger.module;

import com.ormonger.setting.KeybindSetting;
import com.ormonger.setting.Setting;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for every feature. A module has a name, a category, an on/off state,
 * a keybind and any number of settings. Subclasses override the lifecycle hooks.
 */
public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private final KeybindSetting keybind;
    private final List<Setting<?>> settings = new ArrayList<>();

    private boolean enabled;
    private boolean enableQueued;

    protected Module(String name, String description, Category category) {
        this(name, description, category, KeybindSetting.NONE);
    }

    protected Module(String name, String description, Category category, int defaultKey) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.keybind = new KeybindSetting("Keybind", "Key that toggles " + name, defaultKey);
    }

    // ------------------------------------------------------------------ lifecycle hooks

    /** Runs when the module is switched on. The world may be null if enabled from the config at startup. */
    protected void onEnable() {
    }

    /** Runs when the module is switched off. */
    protected void onDisable() {
    }

    /** Runs once per client tick while the player is in a world and the module is enabled. */
    public void onTick() {
    }

    /** Runs when the client disconnects from a world or server. */
    public void onDisconnect() {
    }

    /**
     * Whether the on/off state is saved to the config. Return false for one-shot or
     * situational modules (e.g. Freecam, Surround) so they always start off.
     */
    public boolean persistsEnabledState() {
        return true;
    }

    /** Short text shown after the module name in the HUD, or null for none. */
    public String getInfo() {
        return null;
    }

    // ------------------------------------------------------------------ state

    public final boolean isEnabled() {
        return enabled;
    }

    public final void setEnabled(boolean value) {
        if (this.enabled == value) {
            return;
        }
        this.enabled = value;
        if (value) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    /** Marks the module to be switched on at the first tick inside a world (used by the config loader). */
    public final void queueEnable() {
        this.enableQueued = true;
    }

    public final boolean isEnableQueued() {
        return enableQueued;
    }

    public final void clearEnableQueue() {
        this.enableQueued = false;
    }

    // ------------------------------------------------------------------ settings

    /** Registers a setting and returns it, so it can be assigned to a field in one line. */
    protected final <S extends Setting<?>> S addSetting(S setting) {
        settings.add(setting);
        return setting;
    }

    /** Settings shown in the GUI (the keybind is separate, see {@link #getKeybind()}). */
    public final List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public final KeybindSetting getKeybind() {
        return keybind;
    }

    // ------------------------------------------------------------------ info

    public final String getName() {
        return name;
    }

    public final String getDescription() {
        return description;
    }

    public final Category getCategory() {
        return category;
    }

    /** True when a world and player exist, so modules can safely touch them. */
    protected static boolean inWorld() {
        return mc.player != null && mc.world != null;
    }

    protected static boolean isKeyDown(int key) {
        return mc.getWindow() != null
                && GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }
}
