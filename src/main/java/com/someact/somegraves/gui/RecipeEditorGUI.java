package com.someact.somegraves.gui;

import com.someact.somegraves.SomeGravesPlugin;
import com.someact.somegraves.config.ConfigManager;
import com.someact.somegraves.item.GraveScrollManager;
import com.someact.somegraves.sound.SoundManager;
import com.someact.somegraves.util.ItemBuilder;
import com.someact.somegraves.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 3x3 interactive crafting recipe editor GUI for Grave Teleport Scrolls with Shaped/Shapeless mode toggle.
 */
public class RecipeEditorGUI implements InventoryHolder {

    private final SomeGravesPlugin plugin;
    private final ConfigManager config;
    private final GraveScrollManager scrollManager;
    private final SoundManager soundManager;
    private final Player admin;
    private final Inventory inventory;

    private static final int[] GRID_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int RESULT_SLOT = 24;
    private static final int ENABLE_TOGGLE_SLOT = 45;
    private static final int MODE_TOGGLE_SLOT = 47;
    private static final int SAVE_SLOT = 49;
    private static final int RESET_SLOT = 51;
    private static final int CANCEL_SLOT = 53;

    private boolean shapeless;
    private boolean saved = false;

    public RecipeEditorGUI(SomeGravesPlugin plugin, Player admin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.scrollManager = plugin.getScrollManager();
        this.soundManager = plugin.getSoundManager();
        this.admin = admin;

        this.shapeless = config.isScrollRecipeShapeless();

        Component title = MessageUtil.parse("<gradient:#9d4edd:#e0aaff><bold>Edit Grave Scroll Recipe</bold></gradient>");
        this.inventory = Bukkit.createInventory(this, 54, title);

        populate();
    }

    private void populate() {
        inventory.clear();

        // Filler
        ItemStack filler = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                .name(Component.empty())
                .build();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Clear grid slots
        for (int slot : GRID_SLOTS) {
            inventory.setItem(slot, null);
        }

        // Fill current recipe
        if (shapeless) {
            List<Material> list = config.getScrollRecipeShapelessIngredients();
            for (int i = 0; i < list.size() && i < GRID_SLOTS.length; i++) {
                inventory.setItem(GRID_SLOTS[i], new ItemStack(list.get(i)));
            }
        } else {
            List<String> shape = config.getScrollRecipeShape();
            Map<Character, Material> ingredients = config.getScrollRecipeIngredients();

            for (int row = 0; row < 3 && row < shape.size(); row++) {
                String rowStr = shape.get(row);
                for (int col = 0; col < 3 && col < rowStr.length(); col++) {
                    char c = rowStr.charAt(col);
                    if (c != ' ') {
                        Material mat = ingredients.get(c);
                        if (mat != null) {
                            int slotIndex = row * 3 + col;
                            inventory.setItem(GRID_SLOTS[slotIndex], new ItemStack(mat));
                        }
                    }
                }
            }
        }

        updateControls();
    }

    private void updateControls() {
        // Output Preview
        ItemStack scroll = scrollManager.createGraveScroll(1);
        inventory.setItem(RESULT_SLOT, scroll);

        // Arrow
        inventory.setItem(23, ItemBuilder.from(Material.ARROW)
                .name("<yellow><bold>Crafts Into →</bold></yellow>")
                .build());

        // Recipe Enable/Disable Toggle
        boolean recipeEnabled = config.isScrollRecipeEnabled();
        inventory.setItem(ENABLE_TOGGLE_SLOT, ItemBuilder.from(recipeEnabled ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("<gold><bold>Recipe Status:</bold></gold> " + (recipeEnabled ? "<green><bold>ENABLED</bold></green>" : "<red><bold>DISABLED</bold></red>"))
                .loreStrings(List.of(
                        "<gray>Controls whether players can craft Grave</gray>",
                        "<gray>Teleport Scrolls on crafting tables.</gray>",
                        "",
                        "<yellow>[Click to Toggle Enable/Disable]</yellow>"
                ))
                .glow(recipeEnabled)
                .build());

        // Mode Toggle Button
        if (shapeless) {
            inventory.setItem(MODE_TOGGLE_SLOT, ItemBuilder.from(Material.SLIME_BALL)
                    .name("<gold><bold>Recipe Mode:</bold></gold> <green><bold>SHAPELESS</bold></green>")
                    .loreStrings(List.of(
                            "<gray>Ingredients can be placed anywhere</gray>",
                            "<gray>in the 3x3 crafting matrix.</gray>",
                            "",
                            "<yellow>[Click to switch to SHAPED]</yellow>"
                    ))
                    .glow(true)
                    .build());
        } else {
            inventory.setItem(MODE_TOGGLE_SLOT, ItemBuilder.from(Material.COMPASS)
                    .name("<gold><bold>Recipe Mode:</bold></gold> <aqua><bold>SHAPED (Fixed 3x3)</bold></aqua>")
                    .loreStrings(List.of(
                            "<gray>Requires the exact 3x3 layout</gray>",
                            "<gray>arranged in the grid slots.</gray>",
                            "",
                            "<yellow>[Click to switch to SHAPELESS]</yellow>"
                    ))
                    .glow(true)
                    .build());
        }

        // Instructions
        inventory.setItem(4, ItemBuilder.from(Material.BOOK)
                .name("<gold><bold>Recipe Editor Instructions</bold></gold>")
                .loreStrings(List.of(
                        "<gray>• Place ingredients in the 3x3 grid.</gray>",
                        "<gray>• Toggle between <green>Shapeless</green> or <aqua>Shaped</aqua> mode.</gray>",
                        "<gray>• Click <green>Save Recipe</green> when finished.</gray>"
                ))
                .build());

        // Save Button
        inventory.setItem(SAVE_SLOT, ItemBuilder.from(Material.EMERALD_BLOCK)
                .name("<green><bold>Save & Apply Recipe</bold></green>")
                .loreStrings(List.of(
                        "<gray>Saves the current recipe mode and</gray>",
                        "<gray>ingredients to configuration.</gray>",
                        "",
                        "<yellow>[Click to Save]</yellow>"
                ))
                .glow(true)
                .build());

        // Reset Button
        inventory.setItem(RESET_SLOT, ItemBuilder.from(Material.REDSTONE_BLOCK)
                .name("<yellow><bold>Reset to Default</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Resets the recipe back to default.</gray>"
                ))
                .build());

        // Cancel Button
        inventory.setItem(CANCEL_SLOT, ItemBuilder.from(Material.BARRIER)
                .name("<red><bold>Cancel</bold></red>")
                .build());
    }

    public void handleClick(InventoryClickEvent event) {
        int rawSlot = event.getRawSlot();

        if (rawSlot >= 54) return; // allow player inventory interaction

        for (int gridSlot : GRID_SLOTS) {
            if (rawSlot == gridSlot) return; // allow grid interaction
        }

        event.setCancelled(true);

        if (rawSlot == ENABLE_TOGGLE_SLOT) {
            config.setScrollRecipeEnabled(!config.isScrollRecipeEnabled());
            config.save();
            scrollManager.reloadRecipe();
            updateControls();
            soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
        } else if (rawSlot == MODE_TOGGLE_SLOT) {
            shapeless = !shapeless;
            updateControls();
            soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
        } else if (rawSlot == SAVE_SLOT) {
            saveRecipe();
        } else if (rawSlot == RESET_SLOT) {
            resetDefault();
        } else if (rawSlot == CANCEL_SLOT) {
            admin.closeInventory();
        }
    }

    private void saveRecipe() {
        if (shapeless) {
            List<Material> materials = new ArrayList<>();
            for (int slot : GRID_SLOTS) {
                ItemStack item = inventory.getItem(slot);
                if (item != null && !item.getType().isAir()) {
                    materials.add(item.getType());
                }
            }

            if (materials.isEmpty()) {
                MessageUtil.sendMessage(admin, config.getPrefix() + "<red>Cannot save an empty crafting recipe!</red>");
                soundManager.playSound(admin, "error", Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                return;
            }

            saved = true;
            config.setScrollRecipeShapeless(true);
            config.setScrollRecipeShapelessIngredients(materials);
            config.save();
            scrollManager.reloadRecipe();

            MessageUtil.sendMessage(admin, config.getPrefix() + "<green>Saved Grave Scroll as a <bold>SHAPELESS</bold> crafting recipe (" + materials.size() + " items)!</green>");
        } else {
            Map<Material, Character> matToChar = new HashMap<>();
            char nextChar = 'A';
            List<String> shapeLines = new ArrayList<>();
            Map<Character, Material> ingredients = new HashMap<>();

            for (int row = 0; row < 3; row++) {
                StringBuilder sb = new StringBuilder();
                for (int col = 0; col < 3; col++) {
                    int slot = GRID_SLOTS[row * 3 + col];
                    ItemStack item = inventory.getItem(slot);
                    if (item == null || item.getType().isAir()) {
                        sb.append(' ');
                    } else {
                        Material mat = item.getType();
                        Character ch = matToChar.get(mat);
                        if (ch == null) {
                            ch = nextChar++;
                            matToChar.put(mat, ch);
                            ingredients.put(ch, mat);
                        }
                        sb.append(ch);
                    }
                }
                shapeLines.add(sb.toString());
            }

            if (ingredients.isEmpty()) {
                MessageUtil.sendMessage(admin, config.getPrefix() + "<red>Cannot save an empty crafting recipe!</red>");
                soundManager.playSound(admin, "error", Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                return;
            }

            saved = true;
            config.setScrollRecipeShapeless(false);
            config.setScrollRecipeShape(shapeLines);
            config.setScrollRecipeIngredients(ingredients);
            config.save();
            scrollManager.reloadRecipe();

            MessageUtil.sendMessage(admin, config.getPrefix() + "<green>Saved Grave Scroll as a <bold>SHAPED</bold> 3x3 crafting recipe!</green>");
        }

        soundManager.playSound(admin, "grave-instant-loot", Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        admin.closeInventory();
    }

    private void resetDefault() {
        shapeless = false;
        config.setScrollRecipeShapeless(false);
        config.setScrollRecipeShape(List.of(" R ", "RSR", " R "));
        Map<Character, Material> def = new HashMap<>();
        def.put('R', Material.REDSTONE);
        def.put('S', Material.PAPER);
        config.setScrollRecipeIngredients(def);
        config.setScrollRecipeShapelessIngredients(List.of(Material.REDSTONE, Material.REDSTONE, Material.REDSTONE, Material.REDSTONE, Material.PAPER));
        config.save();
        scrollManager.reloadRecipe();

        populate();
        MessageUtil.sendMessage(admin, config.getPrefix() + "<yellow>Grave Scroll recipe reset to default.</yellow>");
        soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    public void handleClose(InventoryCloseEvent event) {
        if (!saved) {
            for (int slot : GRID_SLOTS) {
                ItemStack item = inventory.getItem(slot);
                if (item != null && !item.getType().isAir()) {
                    HashMap<Integer, ItemStack> overflow = admin.getInventory().addItem(item);
                    for (ItemStack drop : overflow.values()) {
                        admin.getWorld().dropItemNaturally(admin.getLocation(), drop);
                    }
                }
            }
        }
    }

    public void open() {
        admin.openInventory(inventory);
        soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
