package com.ormonger.module;

import com.ormonger.OrmongerClient;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Central registry for all modules. Owns the tick loop that drives enabled modules.
 * Each module added in a later step gets one {@link #register(Module)} line in {@link #registerModules()}.
 */
public final class ModuleManager {
    private final Map<String, Module> modulesByName = new LinkedHashMap<>();
    private final List<Module> modules = new ArrayList<>();

    /** Creates and registers every module. */
    public void init() {
        registerModules();
    }

    private void registerModules() {
        // Modules are added here as each category is implemented.
    }

    public void register(Module module) {
        String key = module.getName().toLowerCase(Locale.ROOT);
        if (modulesByName.containsKey(key)) {
            throw new IllegalStateException("Duplicate module name: " + module.getName());
        }
        modulesByName.put(key, module);
        modules.add(module);
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public Module get(String name) {
        return name == null ? null : modulesByName.get(name.toLowerCase(Locale.ROOT));
    }

    public <T extends Module> T get(Class<T> type) {
        for (Module module : modules) {
            if (type.isInstance(module)) {
                return type.cast(module);
            }
        }
        return null;
    }

    public List<Module> getByCategory(Category category) {
        List<Module> result = new ArrayList<>();
        for (Module module : modules) {
            if (module.getCategory() == category) {
                result.add(module);
            }
        }
        return result;
    }

    public List<Module> getEnabled() {
        List<Module> result = new ArrayList<>();
        for (Module module : modules) {
            if (module.isEnabled()) {
                result.add(module);
            }
        }
        return result;
    }

    /** Runs at the end of every client tick. */
    public void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        for (Module module : modules) {
            if (module.isEnableQueued()) {
                module.clearEnableQueue();
                safely(module, () -> module.setEnabled(true));
            }
            if (module.isEnabled()) {
                safely(module, module::onTick);
            }
        }
    }

    /** Runs when the client leaves a world or server. */
    public void onDisconnect() {
        for (Module module : modules) {
            safely(module, module::onDisconnect);
            if (module.isEnabled() && !module.persistsEnabledState()) {
                safely(module, () -> module.setEnabled(false));
            }
        }
    }

    /** A broken module is switched off and logged instead of crashing the game every tick. */
    private void safely(Module module, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            OrmongerClient.LOGGER.error("Module {} threw an error and was disabled", module.getName(), t);
            try {
                module.setEnabled(false);
            } catch (Throwable ignored) {
                // nothing more can be done for this module
            }
        }
    }
}
