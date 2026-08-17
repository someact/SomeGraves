package com.someact.somegraves.gui;

import com.someact.somegraves.SomeGravesPlugin;
import com.someact.somegraves.config.ConfigManager;
import com.someact.somegraves.grave.SomeGravesManager;
import com.someact.somegraves.model.GraveData;
import com.someact.somegraves.sound.SoundManager;
import com.someact.somegraves.util.ItemBuilder;
import com.someact.somegraves.util.MessageUtil;
import com.someact.somegraves.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Server administrator GUI for viewing and managing all active gravestones on the server.
 */
public class AdminGravesGUI implements InventoryHolder {

    private final SomeGravesPlugin plugin;
    private final ConfigManager config;
    private final SomeGravesManager graveManager;
    private final SoundManager soundManager;
    private final Player admin;
    private final Inventory inventory;

    private List<GraveData> allGraves;
    private int page = 0;

    public AdminGravesGUI(SomeGravesPlugin plugin, Player admin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.graveManager = plugin.getGravestoneManager();
        this.soundManager = plugin.getSoundManager();
        this.admin = admin;

        Component title = MessageUtil.parse("<gradient:#ff7675:#fab1a0><bold>Server Graves Manager</bold></gradient>");
        this.inventory = Bukkit.createInventory(this, 54, title);

        populate();
    }

    private void populate() {
        inventory.clear();
        this.allGraves = new ArrayList<>(plugin.getStorageManager().getAllActiveGraves());
        this.allGraves.sort((a, b) -> Long.compare(b.getDeathTimeMillis(), a.getDeathTimeMillis()));

        // Filler
        ItemStack filler = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                .name(Component.empty())
                .build();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm:ss");
        int startIndex = page * 45;

        for (int i = 0; i < 45 && (startIndex + i) < allGraves.size(); i++) {
            GraveData grave = allGraves.get(startIndex + i);
            Location loc = grave.getLocation();
            String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "world";

            ItemStack head = ItemBuilder.from(Material.PLAYER_HEAD)
                    .skullOwner(grave.getOwnerUuid(), grave.getOwnerName())
                    .name("<gradient:#ff7675:#fab1a0><bold>" + grave.getOwnerName() + "'s Grave</bold></gradient>")
                    .loreStrings(List.of(
                            "<gray>Location: <yellow>" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "</yellow> in <aqua>" + worldName + "</aqua></gray>",
                            "<gray>Killed by: <red>" + grave.getKillerName() + "</red> <dark_gray>(" + grave.getDeathCause() + ")</dark_gray></gray>",
                            "<gray>Weapon: <yellow>" + grave.getKillerWeapon() + "</yellow></gray>",
                            "<gray>Items: <yellow>" + grave.getItems().size() + "</yellow> | XP: <green>" + grave.getStoredXp() + "</green></gray>",
                            "<gray>Expires in: <red>" + TimeUtil.formatDuration(grave.getRemainingSeconds()) + "</red></gray>",
                            "<gray>Died: <dark_gray>" + sdf.format(new Date(grave.getDeathTimeMillis())) + "</dark_gray></gray>",
                            "<gray>Model: <white>" + grave.getModelType().name() + "</white></gray>",
                            "",
                            "<green>• Left-Click: Teleport to Grave</green>",
                            "<gold>• Shift+Left-Click: Open Grave Chest GUI</gold>",
                            "<red>• Right-Click: Delete Grave Instantly</red>"
                    ))
                    .build();

            inventory.setItem(i, head);
        }

        // Navigation
        if (page > 0) {
            inventory.setItem(45, ItemBuilder.from(Material.ARROW)
                    .name("<yellow><bold>← Previous Page</bold></yellow>")
                    .build());
        }

        if ((page + 1) * 45 < allGraves.size()) {
            inventory.setItem(53, ItemBuilder.from(Material.ARROW)
                    .name("<yellow><bold>Next Page →</bold></yellow>")
                    .build());
        }

        inventory.setItem(49, ItemBuilder.from(Material.BARRIER)
                .name("<red><bold>Close</bold></red>")
                .build());
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 49) {
            admin.closeInventory();
            return;
        }

        if (slot == 45 && page > 0) {
            page--;
            populate();
            soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        if (slot == 53 && (page + 1) * 45 < allGraves.size()) {
            page++;
            populate();
            soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        int targetIndex = page * 45 + slot;
        if (slot >= 0 && slot < 45 && targetIndex < allGraves.size()) {
            GraveData grave = allGraves.get(targetIndex);

            if (event.isRightClick()) {
                // Delete grave
                graveManager.removeGrave(grave);
                MessageUtil.sendMessage(admin, config.getPrefix() + "<red>Deleted " + grave.getOwnerName() + "'s gravestone.</red>");
                soundManager.playSound(admin, "grave-expire", Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                populate();
            } else if (event.isShiftClick()) {
                // Open loot GUI
                new GraveChestGUI(plugin, grave).open(admin);
            } else if (event.isLeftClick()) {
                // Teleport admin
                admin.closeInventory();
                Location dest = grave.getLocation().clone().add(0.5, 0.5, 0.5);
                admin.teleportAsync(dest).thenAccept(s -> {
                    soundManager.playSound(admin, "teleport-success", Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    MessageUtil.sendMessage(admin, config.getPrefix() + "<green>Teleported to " + grave.getOwnerName() + "'s grave.</green>");
                });
            }
        }
    }

    public void open() {
        admin.openInventory(inventory);
        soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
