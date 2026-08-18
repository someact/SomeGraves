package com.someact.somegraves.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Universal ItemBuilder utilizing standard Paper ItemMeta and Adventure Components.
 * Compatible across Minecraft 1.20.5, 1.21.0, 1.21.1, 1.21.2, 1.21.3, 1.21.4, and 26.2.
 */
public class ItemBuilder {

    private final ItemStack itemStack;

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.itemStack = new ItemStack(material, amount);
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
    }

    public static ItemBuilder from(Material material) {
        return new ItemBuilder(material);
    }

    public static ItemBuilder from(Material material, int amount) {
        return new ItemBuilder(material, amount);
    }

    public static ItemBuilder from(ItemStack itemStack) {
        return new ItemBuilder(itemStack);
    }

    public ItemBuilder amount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    public ItemBuilder editMeta(Consumer<ItemMeta> consumer) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            consumer.accept(meta);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public <T extends ItemMeta> ItemBuilder editMeta(Class<T> metaClass, Consumer<T> consumer) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && metaClass.isInstance(meta)) {
            consumer.accept(metaClass.cast(meta));
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder name(Component name) {
        return editMeta(meta -> meta.displayName(name));
    }

    public ItemBuilder name(String miniMessageText) {
        return name(MessageUtil.parse(miniMessageText));
    }

    public ItemBuilder lore(List<Component> lore) {
        return editMeta(meta -> meta.lore(lore));
    }

    public ItemBuilder lore(Component... lore) {
        return lore(Arrays.asList(lore));
    }

    public ItemBuilder loreStrings(List<String> lore) {
        List<Component> components = new ArrayList<>(lore.size());
        for (String line : lore) {
            components.add(MessageUtil.parse(line));
        }
        return lore(components);
    }

    public ItemBuilder glow(boolean glow) {
        return editMeta(meta -> {
            try {
                meta.setEnchantmentGlintOverride(glow);
            } catch (Throwable e) {
                if (glow) {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
            }
        });
    }

    public ItemBuilder customModelData(int customModelData) {
        if (customModelData > 0) {
            return editMeta(meta -> meta.setCustomModelData(customModelData));
        }
        return this;
    }

    public <T, Z> ItemBuilder pdc(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
        return editMeta(meta -> meta.getPersistentDataContainer().set(key, type, value));
    }

    public ItemBuilder skullOwner(UUID ownerUuid, String ownerName) {
        if (itemStack.getType() != Material.PLAYER_HEAD) {
            itemStack.setType(Material.PLAYER_HEAD);
        }
        return editMeta(SkullMeta.class, skullMeta -> {
            PlayerProfile profile = Bukkit.createProfile(ownerUuid, ownerName);
            skullMeta.setPlayerProfile(profile);
        });
    }

    public ItemBuilder skullOwner(OfflinePlayer player) {
        if (itemStack.getType() != Material.PLAYER_HEAD) {
            itemStack.setType(Material.PLAYER_HEAD);
        }
        return editMeta(SkullMeta.class, skullMeta -> {
            skullMeta.setOwningPlayer(player);
        });
    }

    public ItemStack build() {
        return itemStack.clone();
    }
}

