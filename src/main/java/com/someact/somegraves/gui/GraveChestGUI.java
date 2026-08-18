package com.someact.somegraves.gui;

import com.someact.somegraves.SomeGravesPlugin;
import com.someact.somegraves.api.event.GraveLootEvent;
import com.someact.somegraves.config.ConfigManager;
import com.someact.somegraves.grave.SomeGravesManager;
import com.someact.somegraves.model.GraveData;
import com.someact.somegraves.sound.SoundManager;
import com.someact.somegraves.util.ItemBuilder;
import com.someact.somegraves.util.MessageUtil;
import com.someact.somegraves.util.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 54-slot chest container GUI for manual gravestone looting with a 'Take All' quick action button.
 */
public class GraveChestGUI implements InventoryHolder {

    private final SomeGravesPlugin plugin;
    private final ConfigManager config;
    private final SomeGravesManager graveManager;
    private final SoundManager soundManager;
    private final GraveData grave;
    private final Inventory inventory;
    private final Player viewer;

    private static final int TAKE_ALL_SLOT = 48;
    private static final int INFO_SLOT = 49;
    private static final int CLOSE_SLOT = 53;

    public GraveChestGUI(SomeGravesPlugin plugin, GraveData grave) {
        this(plugin, grave, null);
    }

    public GraveChestGUI(SomeGravesPlugin plugin, GraveData grave, Player viewer) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.graveManager = plugin.getGravestoneManager();
        this.soundManager = plugin.getSoundManager();
        this.grave = grave;
        this.viewer = viewer;

        Component title = MessageUtil.parse("<gradient:#ff7675:#fab1a0><bold>" + grave.getOwnerName() + "'s Grave Loot</bold></gradient>");
        this.inventory = Bukkit.createInventory(this, 54, title);

        populate();
    }

    private void populate() {
        inventory.clear();

        // Populate items in top 45 slots (0..44)
        List<ItemStack> items = grave.getItems();
        for (int i = 0; i < items.size() && i < 45; i++) {
            ItemStack item = items.get(i);
            if (item != null && !item.getType().isAir()) {
                inventory.setItem(i, item.clone());
            }
        }

        // Fill bottom control row (slots 45..53)
        ItemStack filler = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                .name(Component.empty())
                .build();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Check if the viewing player is allowed to instant-loot / Take All
        boolean allowTakeAll = true;
        if (viewer != null) {
            boolean isOwner = grave.getOwnerUuid().equals(viewer.getUniqueId());
            boolean hasAdminBypass = viewer.hasPermission("somegraves.admin") || viewer.hasPermission("somegraves.bypass.protection");
            boolean canInstantLoot = isOwner ? viewer.hasPermission("somegraves.instantloot.own") : (config.isAllowOthersInstantLoot() && viewer.hasPermission("somegraves.instantloot.others"));
            allowTakeAll = canInstantLoot || hasAdminBypass;
        }

        // Take All Button (Only shown if player has instant-loot permission)
        if (allowTakeAll) {
            inventory.setItem(TAKE_ALL_SLOT, ItemBuilder.from(Material.EMERALD_BLOCK)
                    .name("<green><bold>Take All & Auto-Equip</bold></green>")
                    .loreStrings(List.of(
                            "<gray>Transfers all items, restores original slots,</gray>",
                            "<gray>auto-equips armor, and recovers <gold>" + grave.getStoredXp() + " XP</gold>.</gray>",
                            "",
                            "<yellow>[Click to Take All]</yellow>"
                    ))
                    .glow(true)
                    .build());
        }

        // Grave Info
        inventory.setItem(INFO_SLOT, ItemBuilder.from(Material.PLAYER_HEAD)
                .skullOwner(grave.getOwnerUuid(), grave.getOwnerName())
                .name("<gold><bold>Grave Details</bold></gold>")
                .loreStrings(List.of(
                        "<gray>Owner: <white>" + grave.getOwnerName() + "</white></gray>",
                        "<gray>Killed by: <red>" + grave.getKilledByFormatted() + "</red></gray>",
                        "<gray>Weapon: <yellow>" + grave.getKillerWeapon() + "</yellow></gray>",
                        "<gray>Stored XP: <green>" + grave.getStoredXp() + " XP</green></gray>",
                        "<gray>Stored Items: <yellow>" + grave.getItems().size() + " item(s)</yellow></gray>",
                        "<gray>Expires in: <red>" + TimeUtil.formatDuration(grave.getRemainingSeconds()) + "</red></gray>"
                ))
                .build());

        // Close Button
        inventory.setItem(CLOSE_SLOT, ItemBuilder.from(Material.BARRIER)
                .name("<red><bold>Close</bold></red>")
                .build());
    }

    public void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int rawSlot = event.getRawSlot();

        if (rawSlot >= 45 && rawSlot < 54) {
            event.setCancelled(true);

            if (rawSlot == CLOSE_SLOT) {
                player.closeInventory();
                return;
            }

            if (rawSlot == TAKE_ALL_SLOT) {
                handleTakeAll(player);
                return;
            }

            return;
        }

        // Auto-save changes made to slots 0..44
        if (rawSlot < 45 || event.isShiftClick()) {
            Bukkit.getAsyncScheduler().runDelayed(plugin, task -> {
                saveRemainingItems(player);
            }, 50, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    private void handleTakeAll(Player player) {
        if (grave.isLooted()) {
            player.closeInventory();
            return;
        }

        boolean isOwner = grave.getOwnerUuid().equals(player.getUniqueId());
        boolean hasAdminBypass = player.hasPermission("somegraves.admin") || player.hasPermission("somegraves.bypass.protection");
        boolean canInstantLoot = isOwner ? player.hasPermission("somegraves.instantloot.own") : (config.isAllowOthersInstantLoot() && player.hasPermission("somegraves.instantloot.others"));

        if (!canInstantLoot && !hasAdminBypass) {
            soundManager.playSound(player, "error", Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("loot-denied",
                    "<red>You are not allowed to instant-loot other players' gravestones.</red>"));
            player.closeInventory();
            return;
        }

        // Synchronize current GUI state to grave items in case items were manually moved
        List<ItemStack> currentGuiItems = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) {
                currentGuiItems.add(item.clone());
            }
        }
        grave.getItems().clear();
        grave.getItems().addAll(currentGuiItems);

        player.closeInventory();
        graveManager.performInstantLoot(player, grave);
    }


    public void handleClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        saveRemainingItems(player);
    }

    private void saveRemainingItems(Player player) {
        if (grave.isLooted()) return;

        List<ItemStack> remaining = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) {
                remaining.add(item.clone());
            }
        }

        grave.getItems().clear();
        grave.getItems().addAll(remaining);

        if (grave.getItems().isEmpty() && grave.getStoredXp() == 0) {
            grave.setLooted(true);
            player.closeInventory();
            graveManager.removeGrave(grave);
            MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("loot-success",
                    "<green>You have successfully looted the gravestone!</green>"));
        } else {
            plugin.getStorageManager().saveGraveAsync(grave);
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
        soundManager.playSound(player, "grave-open-chest", Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
