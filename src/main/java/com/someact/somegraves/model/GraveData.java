package com.someact.somegraves.model;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Enterprise data model for a gravestone instance.
 */
public class GraveData {

    private final UUID graveId;
    private final UUID ownerUuid;
    private final String ownerName;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private Location location;
    private final List<ItemStack> items;
    private int storedXp;
    private final long deathTimeMillis;
    private final long durationSeconds;
    private final String deathCause;
    private final String killerName;
    private final String killerWeapon;
    private GraveModelType modelType;

    private UUID displayEntityUuid;
    private UUID visualEntityUuid;
    private boolean isLooted;

    public GraveData(UUID graveId, UUID ownerUuid, String ownerName, Location location,
                     List<ItemStack> items, int storedXp, long deathTimeMillis,
                     long durationSeconds, String deathCause, String killerName,
                     String killerWeapon, GraveModelType modelType) {
        this(graveId, ownerUuid, ownerName,
                location != null && location.getWorld() != null ? location.getWorld().getName() : "world",
                location != null ? location.getX() : 0.0,
                location != null ? location.getY() : 0.0,
                location != null ? location.getZ() : 0.0,
                items, storedXp, deathTimeMillis, durationSeconds, deathCause, killerName, killerWeapon, modelType);
        this.location = location;
    }

    public GraveData(UUID graveId, UUID ownerUuid, String ownerName, String worldName,
                     double x, double y, double z,
                     List<ItemStack> items, int storedXp, long deathTimeMillis,
                     long durationSeconds, String deathCause, String killerName,
                     String killerWeapon, GraveModelType modelType) {
        this.graveId = graveId != null ? graveId : UUID.randomUUID();
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.worldName = worldName != null ? worldName : "world";
        this.x = x;
        this.y = y;
        this.z = z;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.storedXp = storedXp;
        this.deathTimeMillis = deathTimeMillis;
        this.durationSeconds = durationSeconds;
        this.deathCause = deathCause != null ? deathCause : "Died";
        this.killerName = killerName != null ? killerName : "Environment";
        this.killerWeapon = killerWeapon != null ? killerWeapon : "None";
        this.modelType = modelType != null ? modelType : GraveModelType.PLAYER_HEAD;
        this.isLooted = false;
    }

    public UUID getGraveId() {
        return graveId;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public Location getLocation() {
        if (location == null || location.getWorld() == null) {
            org.bukkit.World w = org.bukkit.Bukkit.getWorld(worldName);
            if (w != null) {
                location = new Location(w, x, y, z);
            }
        }
        return location;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public int getStoredXp() {
        return storedXp;
    }

    public void setStoredXp(int storedXp) {
        this.storedXp = storedXp;
    }

    public long getDeathTimeMillis() {
        return deathTimeMillis;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public String getDeathCause() {
        return deathCause;
    }

    public String getKillerName() {
        return killerName;
    }

    public String getKillerWeapon() {
        return killerWeapon;
    }

    public GraveModelType getModelType() {
        return modelType;
    }

    public void setModelType(GraveModelType modelType) {
        this.modelType = modelType;
    }

    public UUID getDisplayEntityUuid() {
        return displayEntityUuid;
    }

    public void setDisplayEntityUuid(UUID displayEntityUuid) {
        this.displayEntityUuid = displayEntityUuid;
    }

    public UUID getVisualEntityUuid() {
        return visualEntityUuid;
    }

    public void setVisualEntityUuid(UUID visualEntityUuid) {
        this.visualEntityUuid = visualEntityUuid;
    }

    public boolean isLooted() {
        return isLooted;
    }

    public void setLooted(boolean looted) {
        this.isLooted = looted;
    }

    public long getRemainingSeconds() {
        if (durationSeconds <= 0) {
            return Long.MAX_VALUE; // Infinite
        }
        long elapsedSeconds = (System.currentTimeMillis() - deathTimeMillis) / 1000;
        return Math.max(0, durationSeconds - elapsedSeconds);
    }

    public boolean isExpired() {
        if (durationSeconds <= 0) return false;
        return getRemainingSeconds() <= 0;
    }
}
