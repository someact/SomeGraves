package com.someact.somegraves.tracker;

import com.someact.somegraves.SomeGravesPlugin;
import com.someact.somegraves.api.event.GraveTrackEvent;
import com.someact.somegraves.config.ConfigManager;
import com.someact.somegraves.model.GraveData;
import com.someact.somegraves.sound.SoundManager;
import com.someact.somegraves.util.MessageUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages live on-screen compass tracking and Actionbar navigation towards graves.
 */
public class GraveTrackerManager {

    private final SomeGravesPlugin plugin;
    private final ConfigManager config;
    private final SoundManager soundManager;
    private final Map<UUID, UUID> trackingMap = new ConcurrentHashMap<>();
    private ScheduledTask trackerTask;

    public GraveTrackerManager(SomeGravesPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.soundManager = plugin.getSoundManager();
    }

    public void start() {
        if (trackerTask != null) trackerTask.cancel();

        long ticks = config.getTrackingUpdateTicks();
        long periodMs = Math.max(50, ticks * 50);

        this.trackerTask = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            updateAllTracking();
        }, periodMs, periodMs, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (trackerTask != null) trackerTask.cancel();
        trackingMap.clear();
    }

    public void startTracking(Player player, GraveData grave) {
        if (!config.isTrackingEnabled()) {
            MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("tracking-disabled",
                    "<red>Grave compass tracking is disabled on this server.</red>"));
            return;
        }

        GraveTrackEvent event = new GraveTrackEvent(player, grave, GraveTrackEvent.TrackAction.START);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        trackingMap.put(player.getUniqueId(), grave.getGraveId());
        player.setCompassTarget(grave.getLocation());

        soundManager.playSound(player, "tracking-start", Sound.ITEM_LODESTONE_COMPASS_LOCK, 1.0f, 1.2f);

        TagResolver res = TagResolver.resolver(
                Placeholder.parsed("x", String.valueOf(grave.getLocation().getBlockX())),
                Placeholder.parsed("y", String.valueOf(grave.getLocation().getBlockY())),
                Placeholder.parsed("z", String.valueOf(grave.getLocation().getBlockZ()))
        );
        MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("tracking-start",
                "<green>Tracking started for grave at <yellow><x>, <y>, <z></yellow>. Follow the actionbar compass!</green>"), res);
    }

    public void stopTracking(Player player) {
        UUID graveId = trackingMap.remove(player.getUniqueId());
        if (graveId != null) {
            GraveData grave = plugin.getStorageManager().getGraveById(graveId);
            GraveTrackEvent event = new GraveTrackEvent(player, grave, GraveTrackEvent.TrackAction.STOP);
            Bukkit.getPluginManager().callEvent(event);

            MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("tracking-stop",
                    "<yellow>Grave tracking stopped.</yellow>"));
        }
    }

    public boolean isTracking(Player player) {
        return trackingMap.containsKey(player.getUniqueId());
    }

    public GraveData getTrackedGrave(Player player) {
        UUID graveId = trackingMap.get(player.getUniqueId());
        if (graveId == null) return null;
        return plugin.getStorageManager().getGraveById(graveId);
    }

    private void updateAllTracking() {
        for (Map.Entry<UUID, UUID> entry : trackingMap.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                trackingMap.remove(entry.getKey());
                continue;
            }

            GraveData grave = plugin.getStorageManager().getGraveById(entry.getValue());
            if (grave == null || grave.isLooted()) {
                trackingMap.remove(entry.getKey());
                continue;
            }

            Location pLoc = player.getLocation();
            Location gLoc = grave.getLocation();

            if (!pLoc.getWorld().equals(gLoc.getWorld())) {
                MessageUtil.sendActionBar(player, "<yellow>Grave is in another world: <aqua>" + gLoc.getWorld().getName() + "</aqua></yellow>");
                continue;
            }

            double distance = pLoc.distance(gLoc);

            // Arrived
            if (distance <= config.getTrackingArrivedDistance()) {
                trackingMap.remove(player.getUniqueId());
                soundManager.playSound(player, "tracking-arrived", Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("tracking-arrived",
                        "<green><bold>✔ You have arrived at your gravestone!</bold></green>"));
                continue;
            }

            String arrow = calculateDirectionArrow(pLoc, gLoc);
            String distStr = String.format("%.1f", distance);

            TagResolver res = TagResolver.resolver(
                    Placeholder.parsed("player_name", grave.getOwnerName()),
                    Placeholder.parsed("distance", distStr),
                    Placeholder.parsed("direction_arrow", arrow),
                    Placeholder.parsed("x", String.valueOf(gLoc.getBlockX())),
                    Placeholder.parsed("y", String.valueOf(gLoc.getBlockY())),
                    Placeholder.parsed("z", String.valueOf(gLoc.getBlockZ())),
                    Placeholder.parsed("world", gLoc.getWorld().getName())
            );

            MessageUtil.sendActionBar(player, config.getTrackingActionbarFormat(), res);
        }
    }

    private String calculateDirectionArrow(Location playerLoc, Location targetLoc) {
        double dx = targetLoc.getX() - playerLoc.getX();
        double dz = targetLoc.getZ() - playerLoc.getZ();

        double angleToTarget = Math.toDegrees(Math.atan2(-dx, dz));
        if (angleToTarget < 0) angleToTarget += 360;

        double playerYaw = playerLoc.getYaw();
        while (playerYaw < 0) playerYaw += 360;
        while (playerYaw >= 360) playerYaw -= 360;

        double relativeAngle = angleToTarget - playerYaw;
        while (relativeAngle < 0) relativeAngle += 360;
        while (relativeAngle >= 360) relativeAngle -= 360;

        if (relativeAngle >= 337.5 || relativeAngle < 22.5) return "⬆";
        if (relativeAngle >= 22.5 && relativeAngle < 67.5) return "⬈";
        if (relativeAngle >= 67.5 && relativeAngle < 112.5) return "➡";
        if (relativeAngle >= 112.5 && relativeAngle < 157.5) return "⬊";
        if (relativeAngle >= 157.5 && relativeAngle < 202.5) return "⬇";
        if (relativeAngle >= 202.5 && relativeAngle < 247.5) return "⬋";
        if (relativeAngle >= 247.5 && relativeAngle < 292.5) return "⬅";
        return "⬉";
    }
}
