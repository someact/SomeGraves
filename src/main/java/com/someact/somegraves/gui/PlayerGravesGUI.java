package com.someact.somegraves.gui;

import com.someact.somegraves.SomeGravesPlugin;
import com.someact.somegraves.api.event.GraveTeleportEvent;
import com.someact.somegraves.config.ConfigManager;
import com.someact.somegraves.item.GraveScrollManager;
import com.someact.somegraves.model.GraveData;
import com.someact.somegraves.sound.SoundManager;
import com.someact.somegraves.tracker.GraveTrackerManager;
import com.someact.somegraves.util.ItemBuilder;
import com.someact.somegraves.util.MessageUtil;
import com.someact.somegraves.util.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
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
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * GUI showing a player's active gravestones with options to track or teleport.
 */
public class PlayerGravesGUI implements InventoryHolder {

    private final SomeGravesPlugin plugin;
    private final ConfigManager config;
    private final GraveTrackerManager tracker;
    private final GraveScrollManager scrollManager;
    private final SoundManager soundManager;
    private final Player player;
    private final Inventory inventory;

    private List<GraveData> playerGraves;
    private UUID pendingTeleportGraveId;

    public PlayerGravesGUI(SomeGravesPlugin plugin, Player player) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.tracker = plugin.getTrackerManager();
        this.scrollManager = plugin.getScrollManager();
        this.soundManager = plugin.getSoundManager();
        this.player = player;

        Component title = MessageUtil.parse("<gradient:#9d4edd:#e0aaff><bold>Your Active Gravestones</bold></gradient>");
        this.inventory = Bukkit.createInventory(this, 54, title);

        populate();
    }

    private void populate() {
        inventory.clear();
        this.playerGraves = plugin.getStorageManager().getActiveGravesForPlayer(player.getUniqueId());

        ItemStack filler = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                .name(Component.empty())
                .build();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm:ss");

        for (int i = 0; i < playerGraves.size() && i < 45; i++) {
            GraveData grave = playerGraves.get(i);
            Location loc = grave.getLocation();
            String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "world";
            double dist = player.getWorld().equals(loc.getWorld()) ? player.getLocation().distance(loc) : -1;
            String distStr = dist >= 0 ? String.format("%.1fm", dist) : "Different World";

            boolean isTracking = tracker.isTracking(player) && tracker.getTrackedGrave(player) != null
                    && tracker.getTrackedGrave(player).getGraveId().equals(grave.getGraveId());

            ItemStack head = ItemBuilder.from(Material.PLAYER_HEAD)
                    .skullOwner(grave.getOwnerUuid(), grave.getOwnerName())
                    .name("<gradient:#ff7675:#fab1a0><bold>Grave #" + (i + 1) + "</bold></gradient> " + (isTracking ? "<gold>[Tracking]</gold>" : ""))
                    .loreStrings(List.of(
                            "<gray>Location: <yellow>" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "</yellow> in <aqua>" + worldName + "</aqua></gray>",
                            "<gray>Distance: <white>" + distStr + "</white></gray>",
                            "<gray>Killed by: <red>" + grave.getKillerName() + "</red> <dark_gray>(" + grave.getDeathCause() + ")</dark_gray></gray>",
                            "<gray>Weapon: <yellow>" + grave.getKillerWeapon() + "</yellow></gray>",
                            "<gray>Items: <yellow>" + grave.getItems().size() + "</yellow> | XP: <green>" + grave.getStoredXp() + "</green></gray>",
                            "<gray>Expires in: <red>" + TimeUtil.formatDuration(grave.getRemainingSeconds()) + "</red></gray>",
                            "<gray>Died: <dark_gray>" + sdf.format(new Date(grave.getDeathTimeMillis())) + "</dark_gray></gray>",
                            "",
                            "<green>• Left-Click: Start / Stop Live Compass Tracking</green>",
                            "<light_purple>• Right-Click: Teleport (Consumes 1 Scroll)</light_purple>"
                    ))
                    .glow(isTracking)
                    .build();

            inventory.setItem(i, head);
        }

        if (playerGraves.isEmpty()) {
            inventory.setItem(22, ItemBuilder.from(Material.BARRIER)
                    .name("<yellow><bold>No Active Graves</bold></yellow>")
                    .loreStrings(List.of("<gray>You currently have no active gravestones.</gray>"))
                    .build());
        }

        inventory.setItem(49, ItemBuilder.from(Material.BARRIER)
                .name("<red><bold>Close Menu</bold></red>")
                .build());
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        if (slot >= 0 && slot < playerGraves.size()) {
            GraveData grave = playerGraves.get(slot);

            if (event.isLeftClick()) {
                // Toggle Tracking
                if (tracker.isTracking(player) && tracker.getTrackedGrave(player) != null
                        && tracker.getTrackedGrave(player).getGraveId().equals(grave.getGraveId())) {
                    tracker.stopTracking(player);
                } else {
                    tracker.startTracking(player, grave);
                }
                player.closeInventory();
            } else if (event.isRightClick()) {
                // Teleport
                if (!config.isTeleportationEnabled()) {
                    MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("teleport-disabled",
                            "<red>Teleportation to gravestones is disabled on this server.</red>"));
                    soundManager.playSound(player, "teleport-fail", Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    return;
                }

                if (!hasScroll(player)) {
                    MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("teleport-no-scroll",
                            "<red>You need a Grave Teleport Scroll in your inventory to teleport!</red>"));
                    soundManager.playSound(player, "teleport-fail", Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    return;
                }

                if (pendingTeleportGraveId != null && pendingTeleportGraveId.equals(grave.getGraveId())) {
                    // Confirmed teleport
                    executeTeleport(grave);
                } else {
                    // Prompt confirmation
                    pendingTeleportGraveId = grave.getGraveId();
                    TagResolver res = TagResolver.resolver(
                            Placeholder.parsed("x", String.valueOf(grave.getLocation().getBlockX())),
                            Placeholder.parsed("y", String.valueOf(grave.getLocation().getBlockY())),
                            Placeholder.parsed("z", String.valueOf(grave.getLocation().getBlockZ()))
                    );
                    MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("teleport-confirm",
                            "<yellow>Click again to confirm teleporting to grave at <white><x>, <y>, <z></white> (Consumes 1 Scroll).</yellow>"), res);
                    soundManager.playSound(player, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                }
            }
        }
    }

    private boolean hasScroll(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (scrollManager.isGraveScroll(item)) return true;
        }
        return false;
    }

    private void consumeScroll(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (scrollManager.isGraveScroll(item)) {
                item.setAmount(item.getAmount() - 1);
                break;
            }
        }
    }

    private void executeTeleport(GraveData grave) {
        GraveTeleportEvent event = new GraveTeleportEvent(player, grave);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        consumeScroll(player);
        player.closeInventory();

        Location dest = grave.getLocation().clone().add(0.5, 0.5, 0.5);
        player.teleportAsync(dest).thenAccept(success -> {
            if (success) {
                soundManager.playSound(player, "teleport-success", Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("teleport-success",
                        "<light_purple>Teleported to your gravestone!</light_purple>"));
            }
        });
    }

    public void open() {
        player.openInventory(inventory);
        soundManager.playSound(player, "gui-click", Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
