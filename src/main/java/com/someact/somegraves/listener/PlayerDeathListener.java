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
import java.util.List;

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

        // 3. Collect items and XP
        List<ItemStack> items = new ArrayList<>(event.getDrops());
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
                    killerName = living.getCustomName() != null ? living.getCustomName() : living.getName();
                    if (living.getEquipment() != null) {
                        ItemStack weapon = living.getEquipment().getItemInMainHand();
                        if (!weapon.getType().isAir()) {
                            killerWeapon = formatItemName(weapon);
                        }
                    }
                } else if (damager != null) {
                    killerName = damager.getName();
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
        graveManager.createGrave(player, deathLoc, items, droppedXp, deathCause, killerName, killerWeapon, skinTexture, skinSignature);
    }

    private String formatDamageCause(EntityDamageEvent.DamageCause cause) {
        if (cause == null) return "Unknown";
        String name = cause.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private String formatItemName(ItemStack item) {
        if (item == null || item.getType().isAir()) return "None";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return MessageUtil.miniMessage().stripTags(MessageUtil.miniMessage().serialize(item.getItemMeta().displayName()));
        }
        String name = item.getType().name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
