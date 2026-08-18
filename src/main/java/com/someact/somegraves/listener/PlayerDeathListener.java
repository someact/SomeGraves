package com.someact.somegraves.listener;

import com.someact.somegraves.SomeGravesPlugin;
import com.someact.somegraves.config.ConfigManager;
import com.someact.somegraves.grave.SomeGravesManager;
import com.someact.somegraves.sound.SoundManager;
import com.someact.somegraves.util.MessageUtil;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles player death, respects keepInventory gamerule and world rules, and spawns gravestones.
 */
public class PlayerDeathListener implements Listener {

    private final SomeGravesPlugin plugin;
    private final ConfigManager config;
    private final SomeGravesManager graveManager;
    private final SoundManager soundManager;

    public PlayerDeathListener(SomeGravesPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.graveManager = plugin.getGravestoneManager();
        this.soundManager = plugin.getSoundManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Location deathLoc = player.getLocation();

        // 1. Check keepInventory GameRule
        if (config.isRespectKeepInventory()) {
            Boolean keepInv = deathLoc.getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY);
            if (Boolean.TRUE.equals(keepInv)) return;
        }

        // 2. Check World Blacklist / Whitelist
        if (!config.isWorldAllowed(deathLoc.getWorld()) && !player.hasPermission("somegraves.bypass.world")) {
            MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("world-blacklisted",
                    "<yellow>Gravestones are disabled in this world.</yellow>"));
            return;
        }

        // 3. Collect items with original inventory slots and XP
        Map<Integer, ItemStack> slotItems = new HashMap<>();
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();

        // Slots 0-35: Hotbar and Main Inventory
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && !item.getType().isAir()) {
                slotItems.put(i, item.clone());
            }
        }
        // Slot 36: Boots, 37: Leggings, 38: Chestplate, 39: Helmet
        if (inv.getBoots() != null && !inv.getBoots().getType().isAir()) {
            slotItems.put(36, inv.getBoots().clone());
        }
        if (inv.getLeggings() != null && !inv.getLeggings().getType().isAir()) {
            slotItems.put(37, inv.getLeggings().clone());
        }
        if (inv.getChestplate() != null && !inv.getChestplate().getType().isAir()) {
            slotItems.put(38, inv.getChestplate().clone());
        }
        if (inv.getHelmet() != null && !inv.getHelmet().getType().isAir()) {
            slotItems.put(39, inv.getHelmet().clone());
        }
        // Slot 40: Off-hand
        if (inv.getItemInOffHand() != null && !inv.getItemInOffHand().getType().isAir()) {
            slotItems.put(40, inv.getItemInOffHand().clone());
        }

        List<ItemStack> items = new ArrayList<>(slotItems.values());
        // Also capture any drop from event.getDrops() not already in slots
        for (ItemStack drop : event.getDrops()) {
            if (drop != null && !drop.getType().isAir() && !items.contains(drop)) {
                items.add(drop.clone());
            }
        }

        int droppedXp = event.getDroppedExp();
        if (items.isEmpty() && droppedXp == 0) return;

        // Clear vanilla drops
        event.getDrops().clear();
        event.setDroppedExp(0);

        // 4. Determine Death Cause, Killer, and Weapon
        String deathCause = "Died";
        String killerName = "Environment";
        String killerWeapon = "None";

        EntityDamageEvent lastDamage = player.getLastDamageCause();
        if (lastDamage != null) {
            deathCause = formatDamageCause(lastDamage.getCause());

            if (lastDamage instanceof EntityDamageByEntityEvent entityDamage) {
                org.bukkit.entity.Entity damager = entityDamage.getDamager();

                if (damager instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof org.bukkit.entity.Entity shooter) {
                    damager = shooter;
                }

                if (damager instanceof Player killerPlayer) {
                    killerName = killerPlayer.getName();
                    ItemStack weapon = killerPlayer.getInventory().getItemInMainHand();
                    if (!weapon.getType().isAir()) {
                        killerWeapon = formatItemName(weapon);
                    }
                } else if (damager instanceof LivingEntity living) {
                    killerName = living.getCustomName() != null ? living.getCustomName() : formatEntityTypeName(living.getType().name());
                    if (living.getEquipment() != null) {
                        ItemStack weapon = living.getEquipment().getItemInMainHand();
                        if (!weapon.getType().isAir()) {
                            killerWeapon = formatItemName(weapon);
                        }
                    }
                } else if (damager != null) {
                    killerName = formatEntityTypeName(damager.getType().name());
                }
            }
        }

        // 5. Extract skin texture properties
        String skinTexture = null;
        String skinSignature = null;
        try {
            com.destroystokyo.paper.profile.PlayerProfile profile = player.getPlayerProfile();
            for (com.destroystokyo.paper.profile.ProfileProperty prop : profile.getProperties()) {
                if ("textures".equals(prop.getName())) {
                    skinTexture = prop.getValue();
                    skinSignature = prop.getSignature();
                    break;
                }
            }
        } catch (Exception ignored) {}

        // 6. Create gravestone
        graveManager.createGrave(player, deathLoc, items, slotItems, droppedXp, deathCause, killerName, killerWeapon, skinTexture, skinSignature);
    }

    private String formatDamageCause(EntityDamageEvent.DamageCause cause) {
        if (cause == null) return "Unknown";
        return switch (cause) {
            case FALL -> "Fall";
            case LAVA -> "Lava";
            case FIRE, FIRE_TICK -> "Fire";
            case DROWNING -> "Drowning";
            case VOID -> "The Void";
            case SUFFOCATION -> "Suffocation";
            case STARVATION -> "Starvation";
            case LIGHTNING -> "Lightning";
            case SUICIDE -> "Suicide";
            case FREEZE -> "Freezing";
            case MAGIC -> "Magic";
            case WITHER -> "Wither";
            case SONIC_BOOM -> "Sonic Boom";
            case ENTITY_EXPLOSION, BLOCK_EXPLOSION -> "Explosion";
            case CONTACT -> "Cactus";
            case CRAMMING -> "Entity Cramming";
            case FLY_INTO_WALL -> "Kinetic Energy";
            case HOT_FLOOR -> "Magma Block";
            case FALLING_BLOCK -> "Falling Block";
            case DRAGON_BREATH -> "Dragon's Breath";
            default -> {
                String name = cause.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
                yield Character.toUpperCase(name.charAt(0)) + name.substring(1);
            }
        };
    }

    private String formatEntityTypeName(String typeName) {
        if (typeName == null || typeName.isEmpty()) return "Unknown";
        String formatted = typeName.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(formatted.charAt(0)) + formatted.substring(1);
    }

    private String formatItemName(ItemStack item) {
        if (item == null || item.getType().isAir()) return "None";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return MessageUtil.miniMessage().stripTags(MessageUtil.miniMessage().serialize(item.getItemMeta().displayName()));
        }
        String name = item.getType().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}

