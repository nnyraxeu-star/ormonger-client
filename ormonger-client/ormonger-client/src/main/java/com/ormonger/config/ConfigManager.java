package com.ormonger.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ormonger.OrmongerClient;
import com.ormonger.friend.FriendManager;
import com.ormonger.input.KeybindHandler;
import com.ormonger.module.Module;
import com.ormonger.module.ModuleManager;
import com.ormonger.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Saves and loads config/ormonger.json: module states, settings, keybinds, the ClickGUI key and friends.
 *
 * Layout:
 * {
 *   "clickGuiKey": 344,
 *   "friends": ["Name1", "Name2"],
 *   "modules": {
 *     "AutoTotem": { "enabled": true, "keybind": -1, "settings": { "Delay": 2.0 } }
 *   }
 * }
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file = FabricLoader.getInstance().getConfigDir().resolve("ormonger.json");
    private final ModuleManager moduleManager;
    private final KeybindHandler keybindHandler;
    private final FriendManager friendManager;

    public ConfigManager(ModuleManager moduleManager, KeybindHandler keybindHandler, FriendManager friendManager) {
        this.moduleManager = moduleManager;
        this.keybindHandler = keybindHandler;
        this.friendManager = friendManager;
    }

    /** Reads the config file. Missing files, keys or unknown names are ignored so old configs keep working. */
    public void load() {
        if (!Files.exists(file)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(file)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                return;
            }
            JsonObject root = parsed.getAsJsonObject();

            keybindHandler.getGuiKey().fromJson(root.get("clickGuiKey"));

            JsonElement friends = root.get("friends");
            if (friends != null && friends.isJsonArray()) {
                friendManager.setAll(toNames(friends.getAsJsonArray()));
            }

            JsonElement modulesElement = root.get("modules");
            if (modulesElement != null && modulesElement.isJsonObject()) {
                JsonObject modulesJson = modulesElement.getAsJsonObject();
                for (Module module : moduleManager.getModules()) {
                    JsonElement entry = modulesJson.get(module.getName());
                    if (entry != null && entry.isJsonObject()) {
                        loadModule(module, entry.getAsJsonObject());
                    }
                }
            }
        } catch (Exception e) {
            OrmongerClient.LOGGER.error("Could not read ormonger.json, keeping defaults", e);
        }
    }

    private void loadModule(Module module, JsonObject json) {
        module.getKeybind().fromJson(json.get("keybind"));

        JsonElement settingsElement = json.get("settings");
        if (settingsElement != null && settingsElement.isJsonObject()) {
            JsonObject settingsJson = settingsElement.getAsJsonObject();
            for (Setting<?> setting : module.getSettings()) {
                setting.fromJson(settingsJson.get(setting.getName()));
            }
        }

        JsonElement enabled = json.get("enabled");
        if (module.persistsEnabledState() && enabled != null && enabled.isJsonPrimitive()
                && enabled.getAsJsonPrimitive().isBoolean() && enabled.getAsBoolean()) {
            module.queueEnable();
        }
    }

    /** Writes the config file. Called when the client stops, and can be called any time. */
    public void save() {
        JsonObject root = new JsonObject();
        root.add("clickGuiKey", keybindHandler.getGuiKey().toJson());

        JsonArray friends = new JsonArray();
        for (String name : friendManager.getFriends()) {
            friends.add(name);
        }
        root.add("friends", friends);

        JsonObject modulesJson = new JsonObject();
        for (Module module : moduleManager.getModules()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("enabled", module.persistsEnabledState() && module.isEnabled());
            entry.add("keybind", module.getKeybind().toJson());

            JsonObject settingsJson = new JsonObject();
            for (Setting<?> setting : module.getSettings()) {
                settingsJson.add(setting.getName(), setting.toJson());
            }
            entry.add("settings", settingsJson);

            modulesJson.add(module.getName(), entry);
        }
        root.add("modules", modulesJson);

        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling("ormonger.json.tmp");
            try (Writer writer = Files.newBufferedWriter(temp)) {
                GSON.toJson(root, writer);
            }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            OrmongerClient.LOGGER.error("Could not save ormonger.json", e);
        }
    }

    private static Iterable<String> toNames(JsonArray array) {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                names.add(element.getAsString());
            }
        }
        return names;
    }
}
