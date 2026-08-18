package com.someact.somegraves.grave;

import com.someact.somegraves.SomeGravesPlugin;
import com.someact.somegraves.api.event.GraveExpireEvent;
import com.someact.somegraves.api.event.GraveLootEvent;
import com.someact.somegraves.api.event.GraveSpawnEvent;
import com.someact.somegraves.config.ConfigManager;
import com.someact.somegraves.model.GraveData;
import com.someact.somegraves.sound.SoundManager;
import com.someact.somegraves.storage.GraveStorageManager;
import com.someact.somegraves.util.MessageUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Central orchestrator managing gravestone lifecycle, safe spawning, looting, and expiration.
 */
public class SomeGravesManager {

    private final SomeGravesPlugin plugin;
    private final ConfigManager config;
    private final GraveStorageManager storage;
    private final GraveVisualManager visualManager;
    private final SoundManager soundManager;

    private ScheduledTask tickerTask;

    public SomeGravesManager(SomeGravesPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.storage = plugin.getStorageManager();
        this.visualManager = new GraveVisualManager(plugin);
        this.soundManager = plugin.getSoundManager();
    }

    public void startTicker() {
        if (tickerTask != null) tickerTask.cancel();

        tickerTask = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            tickGraves();
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void restoreGraves() {
        for (GraveData grave : storage.getAllActiveGraves()) {
            Location loc = grave.getLocation();
            if (loc != null && loc.getWorld() != null && !grave.isLooted()) {
                Bukkit.getRegionScheduler().run(plugin, loc, task -> {
                    visualManager.spawnVisual(grave);
                });
            }
        }
    }

    public void stopTicker() {
        if (tickerTask != null) tickerTask.cancel();
    }

    public GraveData createGrave(Player player, Location deathLoc, List<ItemStack> items,
                                int lostXp, String deathCause, String killerName, String killerWeapon) {
        return createGrave(player, deathLoc, items, lostXp, deathCause, killerName, killerWeapon, null, null);
    }

    public GraveData createGrave(Player player, Location deathLoc, List<ItemStack> items,
                                int lostXp, String deathCause, String killerName, String killerWeapon,
                                String skinTexture, String skinSignature) {
        Location safeLoc = config.isAutoSafeLocation() ? findSafeLocation(deathLoc) : deathLoc;
        int storedXp = (int) Math.round(lostXp * config.getXpRetentionRate());
        long duration = config.getGraveDurationSeconds();

        GraveData grave = new GraveData(
                UUID.randomUUID(),
                player.getUniqueId(),
                player.getName(),
                safeLoc,
                items,
                storedXp,
                System.currentTimeMillis(),
                duration,
                deathCause,
                killerName,
                killerWeapon,
                config.getModelType()
        );
        grave.setSkinTextureValue(skinTexture);
        grave.setSkinTextureSignature(skinSignature);

        // Fire API event
        GraveSpawnEvent spawnEvent = new GraveSpawnEvent(player, grave);
        Bukkit.getPluginManager().callEvent(spawnEvent);
        if (spawnEvent.isCancelled()) {
            dropItemsNaturally(safeLoc, items, storedXp);
            return null;
        }

        // Spawn visual and hologram on region thread
        Bukkit.getRegionScheduler().run(plugin, safeLoc, t -> {
            visualManager.spawnVisual(grave);
        });

        // Add to storage
        storage.addGrave(grave);

        // Sound effect
        soundManager.playSound(player, "grave-spawn", Sound.BLOCK_BELL_USE, 1.0f, 0.8f);

        // Death notification with clickable tracking button
        TagResolver res = TagResolver.resolver(
                Placeholder.parsed("x", String.valueOf(safeLoc.getBlockX())),
                Placeholder.parsed("y", String.valueOf(safeLoc.getBlockY())),
                Placeholder.parsed("z", String.valueOf(safeLoc.getBlockZ())),
                Placeholder.parsed("world", safeLoc.getWorld().getName()),
                Placeholder.parsed("grave_id", grave.getGraveId().toString())
        );
        MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("death-notification",
                "<red>You died at <yellow><x>, <y>, <z></yellow> in <aqua><world></aqua>!</red>"), res);

        return grave;
    }

    public void performInstantLoot(Player player, GraveData grave) {
        if (grave.isLooted()) return;

        // Fire API event
        GraveLootEvent lootEvent = new GraveLootEvent(player, grave, GraveLootEvent.LootType.INSTANT_AUTO_EQUIP);
        Bukkit.getPluginManager().callEvent(lootEvent);
        if (lootEvent.isCancelled()) return;

        grave.setLooted(true);

        PlayerInventory inv = player.getInventory();
        List<ItemStack> remaining = new ArrayList<>();

        for (ItemStack item : grave.getItems()) {
            if (item == null || item.getType().isAir()) continue;

            String name = item.getType().name();
            if (name.endsWith("_HELMET") && (inv.getHelmet() == null || inv.getHelmet().getType().isAir())) {
                inv.setHelmet(item);
            } else if (name.endsWith("_CHESTPLATE") && (inv.getChestplate() == null || inv.getChestplate().getType().isAir())) {
                inv.setChestplate(item);
            } else if (name.endsWith("_LEGGINGS") && (inv.getLeggings() == null || inv.getLeggings().getType().isAir())) {
                inv.setLeggings(item);
            } else if (name.endsWith("_BOOTS") && (inv.getBoots() == null || inv.getBoots().getType().isAir())) {
                inv.setBoots(item);
            } else if (name.endsWith("_SHIELD") && (inv.getItemInOffHand().getType().isAir())) {
                inv.setItemInOffHand(item);
            } else {
                remaining.add(item);
            }
        }

        // Add remaining items to main inventory
        for (ItemStack item : remaining) {
            HashMap<Integer, ItemStack> overflow = inv.addItem(item);
            for (ItemStack drop : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }

        // Restore XP
        if (grave.getStoredXp() > 0) {
            player.giveExp(grave.getStoredXp());
        }

        // Visual & Sound celebrations
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        soundManager.playSound(player, "grave-instant-loot", Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);

        TagResolver res = Placeholder.parsed("xp_stored", String.valueOf(grave.getStoredXp()));
        MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("instant-loot-success",
                "<green>Instant looted! Equipped your gear and recovered <gold><xp_stored> XP</gold>.</green>"), res);

        removeGrave(grave);
    }

    public void removeGrave(GraveData grave) {
        Bukkit.getRegionScheduler().run(plugin, grave.getLocation(), t -> {
            visualManager.removeVisual(grave);
        });
        storage.removeGrave(grave);
    }

    private void tickGraves() {
        for (GraveData grave : storage.getAllActiveGraves()) {
            if (grave.isLooted()) continue;

            if (grave.isExpired()) {
                // Grave expired
                GraveExpireEvent expireEvent = new GraveExpireEvent(grave);
                Bukkit.getPluginManager().callEvent(expireEvent);
                if (expireEvent.isCancelled()) continue;

                if (config.getExpireAction().equalsIgnoreCase("DROP")) {
                    Bukkit.getRegionScheduler().run(plugin, grave.getLocation(), t -> {
                        dropItemsNaturally(grave.getLocation(), grave.getItems(), grave.getStoredXp());
                        soundManager.playSound(grave.getLocation(), "grave-expire", Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
                    });
                }
                removeGrave(grave);
            } else {
                // Update hologram text
                Bukkit.getRegionScheduler().run(plugin, grave.getLocation(), t -> {
                    visualManager.updateHologram(grave);
                });
            }
        }
    }

    private void dropItemsNaturally(Location loc, List<ItemStack> items, int xp) {
        if (loc.getWorld() == null) return;
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                loc.getWorld().dropItemNaturally(loc, item);
            }
        }
        if (xp > 0) {
            ExperienceOrb orb = loc.getWorld().spawn(loc, ExperienceOrb.class);
            orb.setExperience(xp);
        }
    }

    private Location findSafeLocation(Location rawLoc) {
        World world = rawLoc.getWorld();
        if (world == null) return rawLoc;

        int minHeight = world.getMinHeight();
        int maxHeight = world.getMaxHeight();

        int x = rawLoc.getBlockX();
        int y = Math.max(minHeight + 2, Math.min(maxHeight - 2, rawLoc.getBlockY()));
        int z = rawLoc.getBlockZ();

        // If in void
        if (rawLoc.getBlockY() < minHeight) {
            Block platform = world.getBlockAt(x, minHeight + 1, z);
            if (platform.getType().isAir()) platform.setType(Material.COBBLESTONE);
            return new Location(world, x, minHeight + 2, z);
        }

        // If inside lava or air, find solid ground below
        for (int curY = y; curY >= minHeight + 1; curY--) {
            Block check = world.getBlockAt(x, curY, z);
            if (check.getType().isSolid()) {
                Block above = world.getBlockAt(x, curY + 1, z);
                if (!above.getType().isSolid()) {
                    return new Location(world, x, curY + 1, z);
                }
            }
        }

        // Fallback: create safe cobblestone platform
        Block ground = world.getBlockAt(x, y - 1, z);
        if (!ground.getType().isSolid()) {
            ground.setType(Material.COBBLESTONE);
        }
        return new Location(world, x, y, z);
    }

    public GraveVisualManager getVisualManager() {
        return visualManager;
    }
}
