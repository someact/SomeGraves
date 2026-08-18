package com.someact.somegraves;

import com.someact.somegraves.api.SomeGravesAPI;
import com.someact.somegraves.command.SomeGravesCommand;
import com.someact.somegraves.config.ConfigManager;
import com.someact.somegraves.grave.SomeGravesManager;
import com.someact.somegraves.item.GraveScrollManager;
import com.someact.somegraves.listener.*;
import com.someact.somegraves.sound.SoundManager;
import com.someact.somegraves.storage.GraveStorageManager;
import com.someact.somegraves.tracker.GraveTrackerManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Main plugin class for SomeGraves on PaperMC 26.2.
 */
public class SomeGravesPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private SoundManager soundManager;
    private GraveStorageManager storageManager;
    private SomeGravesManager gravestoneManager;
    private GraveTrackerManager trackerManager;
    private GraveScrollManager scrollManager;
    private ChatInputListener chatInputListener;

    @Override
    public void onLoad() {
        // Register Paper Lifecycle Commands
        try {
            getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                SomeGravesCommand cmd = new SomeGravesCommand(this);
                event.registrar().register("somegraves", "SomeGraves main command", List.of("sg"), cmd);
            });
        } catch (Exception e) {
            getLogger().warning("Could not register commands via LifecycleEvents: " + e.getMessage());
        }
    }

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        // 1. Initialize API
        SomeGravesAPI.setPlugin(this);

        // 2. Configuration & Sounds
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.soundManager = new SoundManager(this);

        // 3. Storage
        this.storageManager = new GraveStorageManager(this);
        this.storageManager.init();

        // 4. Managers & Tickers
        this.gravestoneManager = new SomeGravesManager(this);
        this.gravestoneManager.startTicker();

        this.trackerManager = new GraveTrackerManager(this);
        this.trackerManager.start();

        // 5. Items & Recipes
        this.scrollManager = new GraveScrollManager(this);
        this.scrollManager.registerRecipe();

        // 6. Listeners
        this.chatInputListener = new ChatInputListener(this);

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerDeathListener(this), this);
        pm.registerEvents(new GraveInteractListener(this), this);
        pm.registerEvents(new GraveProtectionListener(this), this);
        pm.registerEvents(new InventoryClickListener(), this);
        pm.registerEvents(this.chatInputListener, this);

        if (!configManager.isSilentStartup()) {
            Bukkit.getConsoleSender().sendMessage(com.someact.somegraves.util.MessageUtil.parse(
                    "<dark_gray>[<gradient:#ff7675:#fab1a0><bold>SomeGraves</bold></gradient>]</dark_gray> <green>Thank you for using my <gradient:#ff7675:#fab1a0><bold>SomeGraves</bold></gradient> plugin! :></green> <dark_gray>(v" + getPluginMeta().getVersion() + ", " + (System.currentTimeMillis() - startTime) + "ms)</dark_gray>"
            ));
        } else {
            getLogger().info("SomeGraves plugin enabled successfully in " + (System.currentTimeMillis() - startTime) + "ms!");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling SomeGraves plugin...");

        // Stop background tickers
        if (gravestoneManager != null) {
            gravestoneManager.stopTicker();
        }
        if (trackerManager != null) {
            trackerManager.stop();
        }

        // Unregister recipes
        if (scrollManager != null) {
            scrollManager.unregisterRecipe();
        }

        // Synchronous storage flush
        if (storageManager != null) {
            storageManager.saveAllSync();
        }

        getLogger().info("SomeGraves plugin disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public GraveStorageManager getStorageManager() {
        return storageManager;
    }

    public SomeGravesManager getGravestoneManager() {
        return gravestoneManager;
    }

    public GraveTrackerManager getTrackerManager() {
        return trackerManager;
    }

    public GraveScrollManager getScrollManager() {
        return scrollManager;
    }

    public ChatInputListener getChatInputListener() {
        return chatInputListener;
    }
}
