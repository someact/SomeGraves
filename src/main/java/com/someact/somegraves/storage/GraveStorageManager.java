package com.someact.somegraves.storage;

import com.someact.somegraves.SomeGravesPlugin;
import com.someact.somegraves.model.GraveData;
import com.someact.somegraves.model.GraveModelType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise storage manager for persistent gravestone data with crash-safe atomic file writes.
 */
public class GraveStorageManager {

    private final SomeGravesPlugin plugin;
    private final File dataFolder;
    private final Map<UUID, GraveData> gravesById = new ConcurrentHashMap<>();
    private final Map<Location, GraveData> gravesByLocation = new ConcurrentHashMap<>();

    public GraveStorageManager(SomeGravesPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
    }

    public void init() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        loadAllGraves();
    }

    public void addGrave(GraveData grave) {
        gravesById.put(grave.getGraveId(), grave);
        gravesByLocation.put(normalizeLocation(grave.getLocation()), grave);
        saveGraveAsync(grave);
    }

    public void removeGrave(GraveData grave) {
        gravesById.remove(grave.getGraveId());
        gravesByLocation.remove(normalizeLocation(grave.getLocation()));

        File file = new File(dataFolder, grave.getGraveId().toString() + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }

    public GraveData getGraveAtLocation(Location loc) {
        if (loc == null) return null;
        GraveData found = gravesByLocation.get(normalizeLocation(loc));
        if (found != null) return found;

        // Fallback matching by coordinates
        for (GraveData g : gravesById.values()) {
            Location gLoc = g.getLocation();
            if (gLoc != null && gLoc.getWorld() != null && loc.getWorld() != null &&
                    gLoc.getWorld().getName().equals(loc.getWorld().getName()) &&
                    gLoc.getBlockX() == loc.getBlockX() &&
                    gLoc.getBlockY() == loc.getBlockY() &&
                    gLoc.getBlockZ() == loc.getBlockZ()) {
                gravesByLocation.put(normalizeLocation(gLoc), g);
                return g;
            }
        }
        return null;
    }

    public GraveData getGraveById(UUID id) {
        return gravesById.get(id);
    }

    public List<GraveData> getActiveGravesForPlayer(UUID playerUuid) {
        List<GraveData> list = new ArrayList<>();
        for (GraveData grave : gravesById.values()) {
            if (grave.getOwnerUuid().equals(playerUuid) && !grave.isLooted()) {
                list.add(grave);
            }
        }
        list.sort((a, b) -> Long.compare(b.getDeathTimeMillis(), a.getDeathTimeMillis()));
        return list;
    }

    public Collection<GraveData> getAllActiveGraves() {
        return gravesById.values();
    }

    public void loadAllGraves() {
        gravesById.clear();
        gravesByLocation.clear();

        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                UUID graveId = UUID.fromString(yaml.getString("graveId"));
                UUID ownerUuid = UUID.fromString(yaml.getString("ownerUuid"));
                String ownerName = yaml.getString("ownerName", "Player");

                String worldName = yaml.getString("location.world", "world");
                double x = yaml.getDouble("location.x");
                double y = yaml.getDouble("location.y");
                double z = yaml.getDouble("location.z");
                World world = Bukkit.getWorld(worldName);
                Location loc = world != null ? new Location(world, x, y, z) : null;

                int storedXp = yaml.getInt("storedXp", 0);
                long deathTime = yaml.getLong("deathTimeMillis", System.currentTimeMillis());
                long duration = yaml.getLong("durationSeconds", 1800L);
                String cause = yaml.getString("deathCause", "Died");
                String killer = yaml.getString("killerName", "Environment");
                String weapon = yaml.getString("killerWeapon", "None");
                GraveModelType modelType = GraveModelType.fromString(yaml.getString("modelType"), GraveModelType.PLAYER_HEAD);

                List<ItemStack> items = new ArrayList<>();
                String itemsBase64 = yaml.getString("itemsBase64");
                if (itemsBase64 != null && !itemsBase64.isEmpty()) {
                    byte[] bytes = Base64.getDecoder().decode(itemsBase64);
                    ItemStack[] deserialized = ItemStack.deserializeItemsFromBytes(bytes);
                    items.addAll(Arrays.asList(deserialized));
                }

                GraveData grave = new GraveData(graveId, ownerUuid, ownerName, worldName, x, y, z, items, storedXp, deathTime, duration, cause, killer, weapon, modelType);
                gravesById.put(graveId, grave);
                if (loc != null) {
                    gravesByLocation.put(normalizeLocation(loc), grave);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load grave from " + file.getName() + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + gravesById.size() + " active gravestones from disk.");
    }

    public void saveGraveAsync(GraveData grave) {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> saveGraveSync(grave));
    }

    public void saveGraveSync(GraveData grave) {
        File file = new File(dataFolder, grave.getGraveId().toString() + ".yml");
        File tempFile = new File(dataFolder, grave.getGraveId().toString() + ".tmp");

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("graveId", grave.getGraveId().toString());
        yaml.set("ownerUuid", grave.getOwnerUuid().toString());
        yaml.set("ownerName", grave.getOwnerName());
        yaml.set("location.world", grave.getWorldName());
        yaml.set("location.x", grave.getX());
        yaml.set("location.y", grave.getY());
        yaml.set("location.z", grave.getZ());
        yaml.set("storedXp", grave.getStoredXp());
        yaml.set("deathTimeMillis", grave.getDeathTimeMillis());
        yaml.set("durationSeconds", grave.getDurationSeconds());
        yaml.set("deathCause", grave.getDeathCause());
        yaml.set("killerName", grave.getKillerName());
        yaml.set("killerWeapon", grave.getKillerWeapon());
        yaml.set("modelType", grave.getModelType().name());

        try {
            if (!grave.getItems().isEmpty()) {
                ItemStack[] arr = grave.getItems().toArray(new ItemStack[0]);
                byte[] bytes = ItemStack.serializeItemsAsBytes(arr);
                yaml.set("itemsBase64", Base64.getEncoder().encodeToString(bytes));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to serialize items for grave " + grave.getGraveId() + ": " + e.getMessage());
        }

        try {
            yaml.save(tempFile);
            Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save grave file: " + e.getMessage());
        }
    }

    public void saveAllSync() {
        for (GraveData grave : gravesById.values()) {
            saveGraveSync(grave);
        }
    }

    private Location normalizeLocation(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
