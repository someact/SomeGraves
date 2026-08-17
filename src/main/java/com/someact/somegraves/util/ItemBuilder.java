package com.someact.somegraves.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Modern ItemBuilder utilizing Paper Data Components API and Adventure Components.
 */
public class ItemBuilder {

    private final ItemStack itemStack;

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.itemStack = ItemStack.of(material, amount);
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

    public ItemBuilder name(Component name) {
        itemStack.setData(DataComponentTypes.CUSTOM_NAME, name);
        return this;
    }

    public ItemBuilder name(String miniMessageText) {
        return name(MessageUtil.parse(miniMessageText));
    }

    public ItemBuilder lore(List<Component> lore) {
        itemStack.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return this;
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
        itemStack.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, glow);
        return this;
    }

    public ItemBuilder customModelData(int customModelData) {
        if (customModelData > 0) {
            itemStack.setData(DataComponentTypes.CUSTOM_MODEL_DATA,
                    CustomModelData.customModelData().addFloat((float) customModelData).build());
        }
        return this;
    }

    public <T, Z> ItemBuilder pdc(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
        itemStack.editPersistentDataContainer(pdc -> pdc.set(key, type, value));
        return this;
    }

    public ItemBuilder skullOwner(UUID ownerUuid, String ownerName) {
        if (itemStack.getType() != Material.PLAYER_HEAD) {
            itemStack.setType(Material.PLAYER_HEAD);
        }
        itemStack.editMeta(SkullMeta.class, skullMeta -> {
            PlayerProfile profile = Bukkit.createProfile(ownerUuid, ownerName);
            skullMeta.setPlayerProfile(profile);
        });
        return this;
    }

    public ItemBuilder skullOwner(OfflinePlayer player) {
        if (itemStack.getType() != Material.PLAYER_HEAD) {
            itemStack.setType(Material.PLAYER_HEAD);
        }
        itemStack.editMeta(SkullMeta.class, skullMeta -> {
            skullMeta.setOwningPlayer(player);
        });
        return this;
    }

    public ItemStack build() {
        return itemStack.clone();
    }
}
