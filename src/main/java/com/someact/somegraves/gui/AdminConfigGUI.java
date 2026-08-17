package com.someact.somegraves.gui;

import com.someact.somegraves.SomeGravesPlugin;
import com.someact.somegraves.config.ConfigManager;
import com.someact.somegraves.model.GraveModelType;
import com.someact.somegraves.sound.SoundManager;
import com.someact.somegraves.util.ItemBuilder;
import com.someact.somegraves.util.MessageUtil;
import com.someact.somegraves.util.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Premium in-game administrator configuration control panel for SomeGraves with structured thematic layout.
 */
public class AdminConfigGUI implements InventoryHolder {

    private final SomeGravesPlugin plugin;
    private final ConfigManager config;
    private final SoundManager soundManager;
    private final Player admin;
    private final Inventory inventory;

    // Symmetrical Layout Slots
    // Row 1: Visuals & Hologram
    private static final int MODEL_TYPE_SLOT = 11;
    private static final int DURATION_SLOT = 13;
    private static final int VIEW_DISTANCE_SLOT = 15;

    // Row 2: Looting Rules
    private static final int LOOT_OTHERS_SLOT = 20;
    private static final int SNEAK_INSTANT_SLOT = 22;
    private static final int OTHERS_INSTANT_SLOT = 24;

    // Row 3: Features & Audio
    private static final int SOUNDS_SLOT = 29;
    private static final int TELEPORT_FEATURE_SLOT = 31;
    private static final int TRACKING_FEATURE_SLOT = 33;

    // Row 4: Crafting & Actions
    private static final int EDIT_RECIPE_SLOT = 38;
    private static final int RELOAD_SLOT = 42;

    // Row 5: Exit
    private static final int CLOSE_SLOT = 49;

    public AdminConfigGUI(SomeGravesPlugin plugin, Player admin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.soundManager = plugin.getSoundManager();
        this.admin = admin;

        Component title = MessageUtil.parse("<gradient:#9d4edd:#e0aaff><bold>SomeGraves Configuration</bold></gradient>");
        this.inventory = Bukkit.createInventory(this, 54, title);

        populate();
    }

    private void populate() {
        inventory.clear();

        // 1. Sleek Background Fillers
        ItemStack borderFiller = ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
                .name(Component.empty())
                .build();
        ItemStack innerFiller = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                .name(Component.empty())
                .build();

        for (int i = 0; i < 54; i++) {
            boolean isBorder = (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8);
            inventory.setItem(i, isBorder ? borderFiller : innerFiller);
        }

        // ==========================================
        // ROW 1: Visuals & Holograms
        // ==========================================

        // 1. Grave Visual Model Style
        GraveModelType currentModel = config.getModelType();
        inventory.setItem(MODEL_TYPE_SLOT, ItemBuilder.from(Material.PLAYER_HEAD)
                .name("<yellow><bold>Grave Visual Model</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Current Style: <gold><bold>" + currentModel.name() + "</bold></gold>",
                        "<gray>Styles: PLAYER_HEAD, CHEST, BARREL,</gray>",
                        "<gray>ITEM_DISPLAY (3D), BLOCK_DISPLAY, ARMOR_STAND</gray>",
                        "",
                        "<yellow>[Click to Cycle Model Style]</yellow>"
                ))
                .build());

        // 2. Grave Expiration Duration (Chat Prompt)
        long duration = config.getGraveDurationSeconds();
        inventory.setItem(DURATION_SLOT, ItemBuilder.from(Material.CLOCK)
                .name("<yellow><bold>Grave Duration (Chat Input)</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Current: <white>" + TimeUtil.formatDuration(duration) + "</white> (" + duration + "s)",
                        "<gray>Time before gravestones expire automatically.</gray>",
                        "",
                        "<yellow>[Click to Enter Duration in Chat]</yellow>"
                ))
                .build());

        // 3. Hologram View Distance
        int viewDistBlocks = config.getDisplayViewDistanceBlocks();
        int viewDistChunks = Math.round((float) viewDistBlocks / 16.0f);
        inventory.setItem(VIEW_DISTANCE_SLOT, ItemBuilder.from(Material.SPYGLASS)
                .name("<yellow><bold>Hologram View Distance</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Distance: <gold><bold>" + viewDistBlocks + " Blocks</bold></gold> <dark_gray>(~" + viewDistChunks + " Chunks)</dark_gray>",
                        "<gray>Maximum render distance for floating</gray>",
                        "<gray>hologram text displays above graves.</gray>",
                        "",
                        "<yellow>[Click to Cycle View Distance]</yellow>"
                ))
                .build());

        // ==========================================
        // ROW 2: Looting Rules & Permissions
        // ==========================================

        // 4. Allow Looting Others' Graves
        boolean lootOthers = config.isAllowLootOthers();
        inventory.setItem(LOOT_OTHERS_SLOT, ItemBuilder.from(lootOthers ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("<yellow><bold>Allow Looting Other Graves</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Status: " + (lootOthers ? "<green><bold>ENABLED</bold></green>" : "<red><bold>DISABLED</bold></red>"),
                        "<gray>When enabled, players can open and loot</gray>",
                        "<gray>items from other players' graves.</gray>",
                        "",
                        "<yellow>[Click to Toggle]</yellow>"
                ))
                .glow(lootOthers)
                .build());

        // 5. Owner Sneak Instant Auto-Equip
        boolean sneakLoot = config.isSneakInstantLoot();
        inventory.setItem(SNEAK_INSTANT_SLOT, ItemBuilder.from(sneakLoot ? Material.DIAMOND_CHESTPLATE : Material.CHAINMAIL_CHESTPLATE)
                .name("<yellow><bold>Owner Sneak Instant Auto-Equip</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Status: " + (sneakLoot ? "<green><bold>ENABLED</bold></green>" : "<red><bold>DISABLED</bold></red>"),
                        "<gray>Allows grave owner to Shift+Right-Click</gray>",
                        "<gray>to instantly equip armor & claim loot.</gray>",
                        "",
                        "<yellow>[Click to Toggle]</yellow>"
                ))
                .glow(sneakLoot)
                .build());

        // 6. Other Players Instant Loot
        boolean othersInstant = config.isAllowOthersInstantLoot();
        inventory.setItem(OTHERS_INSTANT_SLOT, ItemBuilder.from(othersInstant ? Material.ENDER_EYE : Material.ENDER_PEARL)
                .name("<yellow><bold>Other Players Instant Loot</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Status: " + (othersInstant ? "<green><bold>ENABLED</bold></green>" : "<red><bold>DISABLED</bold></red>"),
                        "<gray>Can other players also Shift+Right-Click</gray>",
                        "<gray>to instant loot graves they don't own?</gray>",
                        "",
                        "<yellow>[Click to Toggle]</yellow>"
                ))
                .glow(othersInstant)
                .build());

        // ==========================================
        // ROW 3: Features & Audio Engine
        // ==========================================

        // 7. Sound Effects Engine Master Switch
        boolean sounds = config.isSoundsEnabled();
        inventory.setItem(SOUNDS_SLOT, ItemBuilder.from(sounds ? Material.NOTE_BLOCK : Material.JUKEBOX)
                .name("<yellow><bold>Sound Effects Engine</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Status: " + (sounds ? "<green><bold>ENABLED</bold></green>" : "<red><bold>DISABLED</bold></red>"),
                        "<gray>Plays custom sound effects for in-game events.</gray>",
                        "",
                        "<yellow>[Click to Toggle]</yellow>"
                ))
                .glow(sounds)
                .build());

        // 8. Grave Teleport Scroll Feature
        boolean teleports = config.isTeleportationEnabled();
        inventory.setItem(TELEPORT_FEATURE_SLOT, ItemBuilder.from(teleports ? Material.ENDER_EYE : Material.BARRIER)
                .name("<yellow><bold>Grave Teleport Feature</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Status: " + (teleports ? "<green><bold>ENABLED</bold></green>" : "<red><bold>DISABLED</bold></red>"),
                        "<gray>Allows using Grave Scrolls to teleport.</gray>",
                        "",
                        "<yellow>[Click to Toggle]</yellow>"
                ))
                .glow(teleports)
                .build());

        // 9. Live Compass Tracking Feature
        boolean tracking = config.isTrackingEnabled();
        inventory.setItem(TRACKING_FEATURE_SLOT, ItemBuilder.from(tracking ? Material.RECOVERY_COMPASS : Material.COMPASS)
                .name("<yellow><bold>Live Compass Tracking</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Status: " + (tracking ? "<green><bold>ENABLED</bold></green>" : "<red><bold>DISABLED</bold></red>"),
                        "<gray>Actionbar compass navigation towards graves.</gray>",
                        "",
                        "<yellow>[Click to Toggle]</yellow>"
                ))
                .glow(tracking)
                .build());

        // ==========================================
        // ROW 4: Crafting & Administration
        // ==========================================

        // 10. Edit Grave Scroll Recipe (3x3 Grid Editor)
        inventory.setItem(EDIT_RECIPE_SLOT, ItemBuilder.from(Material.CRAFTING_TABLE)
                .name("<gold><bold>Edit Grave Scroll Recipe</bold></gold>")
                .loreStrings(List.of(
                        "<gray>Open the 3x3 crafting grid editor</gray>",
                        "<gray>to customize the Grave Scroll recipe.</gray>",
                        "",
                        "<yellow>[Click to Open Editor]</yellow>"
                ))
                .build());

        // 11. Reload Configuration & Recipes
        inventory.setItem(RELOAD_SLOT, ItemBuilder.from(Material.NETHER_STAR)
                .name("<green><bold>Reload Config & Recipes</bold></green>")
                .loreStrings(List.of(
                        "<gray>Reloads setting.conf from disk and</gray>",
                        "<gray>re-registers all crafting recipes.</gray>",
                        "",
                        "<yellow>[Click to Reload]</yellow>"
                ))
                .glow(true)
                .build());

        // ==========================================
        // ROW 5: Close
        // ==========================================
        inventory.setItem(CLOSE_SLOT, ItemBuilder.from(Material.BARRIER)
                .name("<red><bold>Close Menu</bold></red>")
                .build());
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == CLOSE_SLOT) {
            admin.closeInventory();
            return;
        }

        if (slot == MODEL_TYPE_SLOT) {
            GraveModelType[] types = GraveModelType.values();
            int nextIdx = (config.getModelType().ordinal() + 1) % types.length;
            config.setModelType(types[nextIdx]);
            config.save();
            populate();
            soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
        } else if (slot == DURATION_SLOT) {
            promptDurationInChat();
        } else if (slot == VIEW_DISTANCE_SLOT) {
            // Cycle view distance: 16 (1c) -> 32 (2c) -> 48 (3c) -> 64 (4c) -> 96 (6c) -> 128 (8c)
            int current = config.getDisplayViewDistanceBlocks();
            int next;
            if (current < 32) next = 32;
            else if (current < 48) next = 48;
            else if (current < 64) next = 64;
            else if (current < 96) next = 96;
            else if (current < 128) next = 128;
            else next = 16;

            config.setDisplayViewDistanceBlocks(next);
            config.save();
            populate();
            soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
        } else if (slot == LOOT_OTHERS_SLOT) {
            config.setAllowLootOthers(!config.isAllowLootOthers());
            config.save();
            populate();
            soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
        } else if (slot == SNEAK_INSTANT_SLOT) {
            config.setSneakInstantLoot(!config.isSneakInstantLoot());
            config.save();
            populate();
            soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
        } else if (slot == OTHERS_INSTANT_SLOT) {
            config.setAllowOthersInstantLoot(!config.isAllowOthersInstantLoot());
            config.save();
            populate();
            soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
        } else if (slot == SOUNDS_SLOT) {
            config.setSoundsEnabled(!config.isSoundsEnabled());
            config.save();
            populate();
            soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
        } else if (slot == TELEPORT_FEATURE_SLOT) {
            config.setTeleportationEnabled(!config.isTeleportationEnabled());
            config.save();
            populate();
            soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
        } else if (slot == TRACKING_FEATURE_SLOT) {
            config.setTrackingEnabled(!config.isTrackingEnabled());
            config.save();
            populate();
            soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
        } else if (slot == EDIT_RECIPE_SLOT) {
            new RecipeEditorGUI(plugin, admin).open();
        } else if (slot == RELOAD_SLOT) {
            config.load();
            plugin.getScrollManager().reloadRecipe();
            populate();
            soundManager.playSound(admin, "grave-instant-loot", Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            MessageUtil.sendMessage(admin, config.getPrefix() + config.getMessage("config-reloaded",
                    "<green>Configuration and recipes reloaded successfully!</green>"));
        }
    }

    private void promptDurationInChat() {
        admin.closeInventory();
        MessageUtil.sendMessage(admin, config.getPrefix() + config.getMessage("duration-prompt",
                "<yellow>Please enter the new grave duration in chat (e.g. <white>30m</white>, <white>1h 30m</white>, <white>2d</white>, <white>7200s</white>, or <white>0</white> for infinite). Type <red>cancel</red> to abort.</yellow>"));
        soundManager.playSound(admin, "gui-click", Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);

        plugin.getChatInputListener().requestInput(admin, text -> {
            if (text.equalsIgnoreCase("cancel")) {
                MessageUtil.sendMessage(admin, config.getPrefix() + config.getMessage("duration-cancelled",
                        "<yellow>Grave duration update cancelled.</yellow>"));
                soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                new AdminConfigGUI(plugin, admin).open();
                return;
            }

            try {
                long seconds = TimeUtil.parseDurationSeconds(text);
                config.setGraveDurationSeconds(seconds);
                config.save();

                TagResolver res = Placeholder.parsed("duration", TimeUtil.formatDuration(seconds));
                MessageUtil.sendMessage(admin, config.getPrefix() + config.getMessage("duration-updated",
                        "<green>Grave duration updated to: <gold><duration></gold>.</green>"), res);
                soundManager.playSound(admin, "grave-instant-loot", Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                new AdminConfigGUI(plugin, admin).open();
            } catch (IllegalArgumentException e) {
                MessageUtil.sendMessage(admin, config.getPrefix() + "<red>" + e.getMessage() + "</red>");
                soundManager.playSound(admin, "error", Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                new AdminConfigGUI(plugin, admin).open();
            }
        });
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
