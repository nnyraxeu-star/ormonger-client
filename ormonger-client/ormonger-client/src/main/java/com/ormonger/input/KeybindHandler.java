package com.ormonger.input;

import com.ormonger.module.Module;
import com.ormonger.module.ModuleManager;
import com.ormonger.setting.KeybindSetting;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/**
 * Polls GLFW once per client tick and fires module toggles and the ClickGUI key
 * on the moment a key goes down. Also supports a "listening" mode so the GUI can rebind keys.
 *
 * Keys are ignored while any screen (chat, inventory, the ClickGUI) is open.
 */
public final class KeybindHandler {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final ModuleManager moduleManager;
    private final KeybindSetting guiKey =
            new KeybindSetting("ClickGUI", "Key that opens the ClickGUI", GLFW.GLFW_KEY_RIGHT_SHIFT);
    private final boolean[] down = new boolean[GLFW.GLFW_KEY_LAST + 1];

    private Runnable guiOpener = () -> { };
    private KeybindSetting listening;

    public KeybindHandler(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    /** The key that opens the ClickGUI. Saved by the ConfigManager. */
    public KeybindSetting getGuiKey() {
        return guiKey;
    }

    /** Sets what happens when the ClickGUI key is pressed. */
    public void setGuiOpener(Runnable guiOpener) {
        this.guiOpener = guiOpener == null ? () -> { } : guiOpener;
    }

    /** The next key pressed is assigned to this setting. Escape cancels, Backspace unbinds. */
    public void startListening(KeybindSetting setting) {
        this.listening = setting;
    }

    public void stopListening() {
        this.listening = null;
    }

    public boolean isListening() {
        return listening != null;
    }

    public boolean isListeningTo(KeybindSetting setting) {
        return listening == setting;
    }

    /** Call once per client tick. */
    public void tick() {
        if (mc.getWindow() == null) {
            return;
        }
        long handle = mc.getWindow().getHandle();

        if (listening != null) {
            pollListening(handle);
            return;
        }

        boolean canTrigger = mc.world != null && mc.player != null && mc.currentScreen == null;

        check(handle, guiKey.get(), canTrigger, () -> guiOpener.run());
        for (Module module : moduleManager.getModules()) {
            check(handle, module.getKeybind().get(), canTrigger, module::toggle);
        }
    }

    private void check(long handle, int key, boolean canTrigger, Runnable action) {
        if (key < GLFW.GLFW_KEY_SPACE || key > GLFW.GLFW_KEY_LAST) {
            return;
        }
        boolean pressed = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
        boolean wasDown = down[key];
        down[key] = pressed;
        if (pressed && !wasDown && canTrigger) {
            action.run();
        }
    }

    private void pollListening(long handle) {
        for (int key = GLFW.GLFW_KEY_SPACE; key <= GLFW.GLFW_KEY_LAST; key++) {
            if (GLFW.glfwGetKey(handle, key) != GLFW.GLFW_PRESS) {
                continue;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                listening.set(KeybindSetting.NONE);
            } else if (key != GLFW.GLFW_KEY_ESCAPE) {
                listening.set(key);
            }
            listening = null;
            syncDownState(handle);
            return;
        }
    }

    /** Marks every currently held key as "down" so finishing a rebind does not also trigger it. */
    private void syncDownState(long handle) {
        for (int key = GLFW.GLFW_KEY_SPACE; key <= GLFW.GLFW_KEY_LAST; key++) {
            down[key] = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
        }
    }
}
