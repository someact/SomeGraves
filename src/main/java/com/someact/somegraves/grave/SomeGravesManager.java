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
        return createGrave(player, deathLoc, items, null, lostXp, deathCause, killerName, killerWeapon, null, null);
    }

    public GraveData createGrave(Player player, Location deathLoc, List<ItemStack> items,
                                int lostXp, String deathCause, String killerName, String killerWeapon,
                                String skinTexture, String skinSignature) {
        return createGrave(player, deathLoc, items, null, lostXp, deathCause, killerName, killerWeapon, skinTexture, skinSignature);
    }

    public GraveData createGrave(Player player, Location deathLoc, List<ItemStack> items, Map<Integer, ItemStack> slotItems,
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
        if (slotItems != null) {
            grave.setSlotItems(slotItems);
        }

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
        Map<Integer, ItemStack> slotItems = new HashMap<>(grave.getSlotItems());
        List<ItemStack> unassignedItems = new ArrayList<>();

        // If grave has no slot mapping (e.g. legacy grave data), use items list directly
        if (slotItems.isEmpty()) {
            for (ItemStack item : grave.getItems()) {
                if (item != null && !item.getType().isAir()) {
                    unassignedItems.add(item.clone());
                }
            }
        }

        // 1. Auto-Equip Armor & Offhand from original slots (36..40)
        // Slot 39: Helmet
        ItemStack helmet = slotItems.remove(39);
        if (helmet != null && !helmet.getType().isAir()) {
            if (inv.getHelmet() == null || inv.getHelmet().getType().isAir()) {
                inv.setHelmet(helmet);
            } else {
                unassignedItems.add(helmet);
            }
        }

        // Slot 38: Chestplate
        ItemStack chest = slotItems.remove(38);
        if (chest != null && !chest.getType().isAir()) {
            if (inv.getChestplate() == null || inv.getChestplate().getType().isAir()) {
                inv.setChestplate(chest);
            } else {
                unassignedItems.add(chest);
            }
        }

        // Slot 37: Leggings
        ItemStack legs = slotItems.remove(37);
        if (legs != null && !legs.getType().isAir()) {
            if (inv.getLeggings() == null || inv.getLeggings().getType().isAir()) {
                inv.setLeggings(legs);
            } else {
                unassignedItems.add(legs);
            }
        }

        // Slot 36: Boots
        ItemStack boots = slotItems.remove(36);
        if (boots != null && !boots.getType().isAir()) {
            if (inv.getBoots() == null || inv.getBoots().getType().isAir()) {
                inv.setBoots(boots);
            } else {
                unassignedItems.add(boots);
            }
        }

        // Slot 40: Off-hand
        ItemStack offhand = slotItems.remove(40);
        if (offhand != null && !offhand.getType().isAir()) {
            if (inv.getItemInOffHand() == null || inv.getItemInOffHand().getType().isAir()) {
                inv.setItemInOffHand(offhand);
            } else {
                unassignedItems.add(offhand);
            }
        }

        // 2. Fallback Armor Auto-Equip for any armor in unassignedItems or main inventory slots if armor slots are still empty
        List<ItemStack> stillUnassigned = new ArrayList<>();
        for (ItemStack item : unassignedItems) {
            if (item == null || item.getType().isAir()) continue;
            String type = item.getType().name();
            if ((type.endsWith("_HELMET") || type.equals("TURTLE_HELMET") || type.equals("CARVED_PUMPKIN") || type.equals("PLAYER_HEAD")) && (inv.getHelmet() == null || inv.getHelmet().getType().isAir())) {
                inv.setHelmet(item);
            } else if ((type.endsWith("_CHESTPLATE") || type.equals("ELYTRA")) && (inv.getChestplate() == null || inv.getChestplate().getType().isAir())) {
                inv.setChestplate(item);
            } else if (type.endsWith("_LEGGINGS") && (inv.getLeggings() == null || inv.getLeggings().getType().isAir())) {
                inv.setLeggings(item);
            } else if (type.endsWith("_BOOTS") && (inv.getBoots() == null || inv.getBoots().getType().isAir())) {
                inv.setBoots(item);
            } else if (type.endsWith("_SHIELD") && (inv.getItemInOffHand() == null || inv.getItemInOffHand().getType().isAir())) {
                inv.setItemInOffHand(item);
            } else {
                stillUnassigned.add(item);
            }
        }

        // 3. Restore Exact Original Slots for Storage & Hotbar (Slots 0 to 35)
        for (Map.Entry<Integer, ItemStack> entry : slotItems.entrySet()) {
            int slot = entry.getKey();
            ItemStack item = entry.getValue();
            if (item == null || item.getType().isAir()) continue;

            if (slot >= 0 && slot < 36) {
                ItemStack current = inv.getItem(slot);
                if (current == null || current.getType().isAir()) {
                    inv.setItem(slot, item);
                } else {
                    stillUnassigned.add(item);
                }
            } else {
                stillUnassigned.add(item);
            }
        }

        // 4. Fill remaining open inventory slots and drop overflow
        for (ItemStack item : stillUnassigned) {
            HashMap<Integer, ItemStack> overflow = inv.addItem(item);
            for (ItemStack drop : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }

        // 5. Restore XP
        if (grave.getStoredXp() > 0) {
            player.giveExp(grave.getStoredXp());
        }

        // 6. Visual & Sound celebrations
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

            Location loc = grave.getLocation();
            if (loc == null || loc.getWorld() == null) continue;

            if (grave.isExpired()) {
                grave.setLooted(true); // Prevent re-triggering while queued on region thread
                Bukkit.getRegionScheduler().run(plugin, loc, t -> {
                    GraveExpireEvent expireEvent = new GraveExpireEvent(grave);
                    Bukkit.getPluginManager().callEvent(expireEvent);
                    if (expireEvent.isCancelled()) {
                        grave.setLooted(false);
                        return;
                    }

                    if (config.getExpireAction().equalsIgnoreCase("DROP")) {
                        dropItemsNaturally(loc, grave.getItems(), grave.getStoredXp());
                        soundManager.playSound(loc, "grave-expire", Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
                    }
                    removeGrave(grave);
                });
            } else {
                // Update hologram text
                Bukkit.getRegionScheduler().run(plugin, loc, t -> {
                    if (!grave.isLooted()) {
                        visualManager.updateHologram(grave);
                    }
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
