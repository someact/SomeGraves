package com.someact.somegraves.api;

import com.someact.somegraves.SomeGravesPlugin;
import com.someact.somegraves.model.GraveData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Public developer API for interacting with the SomeGraves system.
 */
public class SomeGravesAPI {

    private static SomeGravesPlugin plugin;

    public static void setPlugin(SomeGravesPlugin instance) {
        plugin = instance;
    }

    public static SomeGravesPlugin getPlugin() {
        if (plugin == null) {
            throw new IllegalStateException("SomeGravesAPI has not been initialized yet!");
        }
        return plugin;
    }

    public static List<GraveData> getGraves(UUID playerUuid) {
        return getPlugin().getStorageManager().getActiveGravesForPlayer(playerUuid);
    }

    public static GraveData getGraveAt(Location location) {
        return getPlugin().getStorageManager().getGraveAtLocation(location);
    }

    public static GraveData getGrave(UUID graveId) {
        return getPlugin().getStorageManager().getGraveById(graveId);
    }

    public static Collection<GraveData> getAllGraves() {
        return getPlugin().getStorageManager().getAllActiveGraves();
    }

    public static boolean removeGrave(UUID graveId) {
        GraveData grave = getGrave(graveId);
        if (grave != null) {
            getPlugin().getGravestoneManager().removeGrave(grave);
            return true;
        }
        return false;
    }

    public static void instantLoot(Player player, GraveData grave) {
        getPlugin().getGravestoneManager().performInstantLoot(player, grave);
    }
}
