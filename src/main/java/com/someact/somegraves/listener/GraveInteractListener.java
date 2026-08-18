package com.someact.somegraves.listener;

import com.someact.somegraves.SomeGravesPlugin;
import com.someact.somegraves.config.ConfigManager;
import com.someact.somegraves.grave.SomeGravesManager;
import com.someact.somegraves.gui.GraveChestGUI;
import com.someact.somegraves.gui.PlayerGravesGUI;
import com.someact.somegraves.item.GraveScrollManager;
import com.someact.somegraves.model.GraveData;
import com.someact.somegraves.sound.SoundManager;
import com.someact.somegraves.storage.GraveStorageManager;
import com.someact.somegraves.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Handles player interactions with gravestone blocks, display entities, and Grave Scrolls.
 */
public class GraveInteractListener implements Listener {

    private final SomeGravesPlugin plugin;
    private final ConfigManager config;
    private final SomeGravesManager graveManager;
    private final GraveStorageManager storage;
    private final GraveScrollManager scrollManager;
    private final SoundManager soundManager;

    public GraveInteractListener(SomeGravesPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.graveManager = plugin.getGravestoneManager();
        this.storage = plugin.getStorageManager();
        this.scrollManager = plugin.getScrollManager();
        this.soundManager = plugin.getSoundManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (config.isScrollEnabled() && scrollManager != null) {
            player.getScheduler().run(plugin, task -> {
                try {
                    player.discoverRecipe(scrollManager.getRecipeKey());
                } catch (Exception ignored) {}
            }, null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();

        // 1. Right Click with Grave Scroll
        if (scrollManager.isGraveScroll(held)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                new PlayerGravesGUI(plugin, player).open();
                return;
            }
        }

        // 2. Right Click on Grave Block
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block == null) return;

            GraveData grave = storage.getGraveAtLocation(block.getLocation());
            if (grave != null && !grave.isLooted()) {
                event.setCancelled(true);
                handleGraveClick(player, grave);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Entity entity = event.getRightClicked();
        Location loc = entity.getLocation().getBlock().getLocation();

        GraveData grave = storage.getGraveAtLocation(loc);
        if (grave != null && !grave.isLooted()) {
            event.setCancelled(true);
            handleGraveClick(event.getPlayer(), grave);
        }
    }

    private void handleGraveClick(Player player, GraveData grave) {
        boolean isOwner = grave.getOwnerUuid().equals(player.getUniqueId());
        boolean hasAdminBypass = player.hasPermission("somegraves.admin") || player.hasPermission("somegraves.bypass.protection");

        // Permission check for looting
        if (isOwner) {
            if (!player.hasPermission("somegraves.loot.own")) {
                MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("no-permission",
                        "<red>You do not have permission to execute this command.</red>"));
                return;
            }
        } else {
            boolean canLootOthers = (config.isAllowLootOthers() && player.hasPermission("somegraves.loot.others")) || hasAdminBypass;
            if (!canLootOthers) {
                soundManager.playSound(player, "error", Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("loot-denied",
                        "<red>You are not allowed to loot other players' gravestones.</red>"));
                return;
            }
        }

        // Instant Auto-Equip Loot Check
        boolean canInstantLoot;
        if (isOwner) {
            canInstantLoot = config.isSneakInstantLoot() && player.hasPermission("somegraves.instantloot.own");
        } else {
            canInstantLoot = config.isSneakInstantLoot() && config.isAllowOthersInstantLoot() && player.hasPermission("somegraves.instantloot.others");
        }

        if (player.isSneaking() && (canInstantLoot || hasAdminBypass)) {
            graveManager.performInstantLoot(player, grave);
        } else {
            // Open 54-slot chest container
            new GraveChestGUI(plugin, grave, player).open(player);
        }
    }

}
