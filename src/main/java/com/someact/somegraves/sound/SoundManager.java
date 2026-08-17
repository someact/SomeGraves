package com.someact.somegraves.sound;

import com.someact.somegraves.SomeGravesPlugin;
import com.someact.somegraves.config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Customizable sound effects engine for SomeGraves with robust fallback support.
 */
public class SoundManager {

    private final SomeGravesPlugin plugin;
    private final ConfigManager config;

    public SoundManager(SomeGravesPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    public void playSound(Player player, String soundKey, Sound fallback, float defaultVolume, float defaultPitch) {
        if (player == null || !config.isSoundsEnabled()) return;
        if (!config.isSoundEventEnabled(soundKey)) return;

        Sound sound = config.getSoundEvent(soundKey, fallback);
        if (sound == null) sound = fallback;
        if (sound == null) return;

        float volume = config.getSoundEventVolume(soundKey, defaultVolume);
        float pitch = config.getSoundEventPitch(soundKey, defaultPitch);

        try {
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not play sound '" + soundKey + "': " + e.getMessage());
        }
    }

    public void playSound(Location loc, String soundKey, Sound fallback, float defaultVolume, float defaultPitch) {
        if (loc == null || loc.getWorld() == null || !config.isSoundsEnabled()) return;
        if (!config.isSoundEventEnabled(soundKey)) return;

        Sound sound = config.getSoundEvent(soundKey, fallback);
        if (sound == null) sound = fallback;
        if (sound == null) return;

        float volume = config.getSoundEventVolume(soundKey, defaultVolume);
        float pitch = config.getSoundEventPitch(soundKey, defaultPitch);

        try {
            loc.getWorld().playSound(loc, sound, volume, pitch);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not play sound '" + soundKey + "': " + e.getMessage());
        }
    }
}
