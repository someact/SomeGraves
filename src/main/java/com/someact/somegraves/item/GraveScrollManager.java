package com.someact.somegraves.item;

import com.someact.somegraves.SomeGravesPlugin;
import com.someact.somegraves.config.ConfigManager;
import com.someact.somegraves.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages the Grave Teleport Scroll item and its dynamic crafting recipes.
 */
public class GraveScrollManager {

    private final SomeGravesPlugin plugin;
    private final ConfigManager config;
    private final NamespacedKey scrollPdcKey;
    private final NamespacedKey recipeKey;

    public GraveScrollManager(SomeGravesPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.scrollPdcKey = new NamespacedKey(plugin, "grave_scroll");
        this.recipeKey = new NamespacedKey(plugin, "grave_scroll_recipe");
    }

    public NamespacedKey getRecipeKey() {
        return recipeKey;
    }

    public ItemStack createGraveScroll(int amount) {
        return ItemBuilder.from(config.getScrollMaterial(), amount)
                .name(config.getScrollName())
                .loreStrings(config.getScrollLore())
                .glow(config.isScrollGlow())
                .customModelData(config.getScrollCustomModelData())
                .pdc(scrollPdcKey, PersistentDataType.BYTE, (byte) 1)
                .build();
    }

    public boolean isGraveScroll(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(scrollPdcKey, PersistentDataType.BYTE);
    }

    public void registerRecipe() {
        if (!config.isScrollEnabled()) return;

        unregisterRecipe();

        try {
            ItemStack scroll = createGraveScroll(1);

            if (config.isScrollRecipeShapeless()) {
                // Shapeless recipe
                ShapelessRecipe recipe = new ShapelessRecipe(recipeKey, scroll);
                List<Material> ingredients = config.getScrollRecipeShapelessIngredients();
                for (Material mat : ingredients) {
                    if (mat != null && !mat.isAir()) {
                        recipe.addIngredient(new RecipeChoice.MaterialChoice(mat));
                    }
                }
                Bukkit.addRecipe(recipe);
                plugin.getLogger().info("Registered SHAPELESS Grave Scroll crafting recipe with " + ingredients.size() + " ingredient(s).");
            } else {
                // Shaped recipe
                ShapedRecipe recipe = new ShapedRecipe(recipeKey, scroll);
                List<String> shape = config.getScrollRecipeShape();
                recipe.shape(shape.get(0), shape.get(1), shape.get(2));

                Set<Character> usedSymbols = new HashSet<>();
                for (String line : shape) {
                    for (char c : line.toCharArray()) {
                        if (c != ' ') usedSymbols.add(c);
                    }
                }

                Map<Character, Material> ingredients = config.getScrollRecipeIngredients();
                for (Map.Entry<Character, Material> entry : ingredients.entrySet()) {
                    if (usedSymbols.contains(entry.getKey()) && entry.getValue() != null && !entry.getValue().isAir()) {
                        recipe.setIngredient(entry.getKey(), new RecipeChoice.MaterialChoice(entry.getValue()));
                    }
                }
                Bukkit.addRecipe(recipe);
                plugin.getLogger().info("Registered SHAPED Grave Scroll crafting recipe.");
            }

            try {
                Bukkit.updateRecipes();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.discoverRecipe(recipeKey);
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register Grave Scroll recipe: " + e.getMessage());
        }
    }

    public void unregisterRecipe() {
        try {
            Bukkit.removeRecipe(recipeKey);
            try {
                Bukkit.updateRecipes();
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    public void reloadRecipe() {
        unregisterRecipe();
        registerRecipe();
    }
}
