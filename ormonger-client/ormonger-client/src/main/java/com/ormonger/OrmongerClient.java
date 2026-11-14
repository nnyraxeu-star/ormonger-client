package com.ormonger;

import com.ormonger.config.ConfigManager;
import com.ormonger.friend.FriendManager;
import com.ormonger.input.KeybindHandler;
import com.ormonger.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrmongerClient implements ClientModInitializer {
    public static final String MOD_ID = "ormonger";
    public static final String MOD_NAME = "Ormonger Client";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static OrmongerClient instance;

    private ModuleManager moduleManager;
    private FriendManager friendManager;
    private KeybindHandler keybindHandler;
    private ConfigManager configManager;

    public static OrmongerClient getInstance() {
        return instance;
    }

    public static ModuleManager modules() {
        return instance.moduleManager;
    }

    public static FriendManager friends() {
        return instance.friendManager;
    }

    public static KeybindHandler keybinds() {
        return instance.keybindHandler;
    }

    public static ConfigManager config() {
        return instance.configManager;
    }

    @Override
    public void onInitializeClient() {
        instance = this;

        moduleManager = new ModuleManager();
        friendManager = new FriendManager();
        keybindHandler = new KeybindHandler(moduleManager);
        configManager = new ConfigManager(moduleManager, keybindHandler, friendManager);

        moduleManager.init();
        configManager.load();

        // Keys are polled before the game ticks, modules run after it.
        ClientTickEvents.START_CLIENT_TICK.register(client -> keybindHandler.tick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> moduleManager.tick(MinecraftClient.getInstance()));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> moduleManager.onDisconnect());

        // Save module states, settings, keybinds and friends when the game closes.
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> configManager.save());

        LOGGER.info("{} loaded with {} modules", MOD_NAME, moduleManager.getModules().size());
    }
}
