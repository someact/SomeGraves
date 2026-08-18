package com.someact.somegraves.grave;

import com.someact.somegraves.SomeGravesPlugin;
import com.someact.somegraves.config.ConfigManager;
import com.someact.somegraves.model.GraveData;
import com.someact.somegraves.model.GraveModelType;
import com.someact.somegraves.util.ItemBuilder;
import com.someact.somegraves.util.MessageUtil;
import com.someact.somegraves.util.TimeUtil;
import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Handles visual representation (5 model types) and floating text holograms for gravestones.
 */
public class GraveVisualManager {

    private final SomeGravesPlugin plugin;
    private final ConfigManager config;

    public GraveVisualManager(SomeGravesPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    public void spawnVisual(GraveData grave) {
        Location loc = grave.getLocation();
        if (loc.getWorld() == null) return;

        GraveModelType modelType = grave.getModelType();

        switch (modelType) {
            case PLAYER_HEAD -> spawnPlayerHeadBlock(grave);
            case CHEST -> loc.getBlock().setType(Material.CHEST, false);
            case BARREL -> loc.getBlock().setType(Material.BARREL, false);
            case ENDER_CHEST -> loc.getBlock().setType(Material.ENDER_CHEST, false);
            case ITEM_DISPLAY -> spawnItemDisplayEntity(grave);
            case BLOCK_DISPLAY -> spawnBlockDisplayEntity(grave);
            case ARMOR_STAND -> spawnArmorStandEntity(grave);
        }

        // Spawn TextDisplay Hologram
        if (config.isDisplayEnabled()) {
            spawnHologram(grave);
        }
    }

    private void spawnPlayerHeadBlock(GraveData grave) {
        Block block = grave.getLocation().getBlock();
        block.setType(Material.PLAYER_HEAD, false);

        if (block.getState() instanceof Skull skull) {
            PlayerProfile profile = Bukkit.createProfile(grave.getOwnerUuid(), grave.getOwnerName());
            if (grave.getSkinTextureValue() != null && !grave.getSkinTextureValue().isEmpty()) {
                profile.setProperty(new com.destroystokyo.paper.profile.ProfileProperty("textures",
                        grave.getSkinTextureValue(), grave.getSkinTextureSignature()));
                skull.setPlayerProfile(profile);
                skull.update(true, false);
            } else {
                // Async complete fallback for legacy graves
                skull.setPlayerProfile(profile);
                skull.update(true, false);
                Bukkit.getAsyncScheduler().runNow(plugin, t -> {
                    try {
                        profile.complete(true);
                        Location loc = grave.getLocation();
                        if (loc != null && loc.getWorld() != null) {
                            Bukkit.getRegionScheduler().run(plugin, loc, task -> {
                                if (loc.getBlock().getState() instanceof Skull s) {
                                    s.setPlayerProfile(profile);
                                    s.update(true, false);
                                }
                            });
                        }
                    } catch (Exception ignored) {}
                });
            }
        }
    }

    private void spawnItemDisplayEntity(GraveData grave) {
        Location spawnLoc = grave.getLocation().clone().add(0.5, 0.5, 0.5);
        ItemDisplay display = spawnLoc.getWorld().spawn(spawnLoc, ItemDisplay.class, entity -> {
            ItemStack item = ItemBuilder.from(config.getModelCustomItemMaterial(), 1)
                    .customModelData(config.getModelCustomModelData())
                    .build();
            entity.setItemStack(item);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setTransformation(new Transformation(
                    new Vector3f(),
                    new AxisAngle4f(),
                    new Vector3f((float) config.getDisplayScaleX(), (float) config.getDisplayScaleY(), (float) config.getDisplayScaleZ()),
                    new AxisAngle4f()
            ));
            entity.setPersistent(true);
        });
        grave.setVisualEntityUuid(display.getUniqueId());
    }

    private void spawnBlockDisplayEntity(GraveData grave) {
        Location spawnLoc = grave.getLocation().clone().add(0.5, 0.0, 0.5);
        BlockDisplay display = spawnLoc.getWorld().spawn(spawnLoc, BlockDisplay.class, entity -> {
            entity.setBlock(Material.COBBLESTONE.createBlockData());
            entity.setTransformation(new Transformation(
                    new Vector3f(),
                    new AxisAngle4f(),
                    new Vector3f((float) config.getDisplayScaleX(), (float) config.getDisplayScaleY(), (float) config.getDisplayScaleZ()),
                    new AxisAngle4f()
            ));
            entity.setPersistent(true);
        });
        grave.setVisualEntityUuid(display.getUniqueId());
    }

    private void spawnArmorStandEntity(GraveData grave) {
        Location spawnLoc = grave.getLocation().clone().add(0.5, 0.0, 0.5);
        ArmorStand stand = spawnLoc.getWorld().spawn(spawnLoc, ArmorStand.class, entity -> {
            entity.setVisible(config.isArmorStandVisible());
            entity.setSmall(config.isArmorStandSmall());
            entity.setArms(config.isArmorStandArms());
            entity.setBasePlate(false);
            entity.setGravity(false);
            entity.setInvulnerable(true);

            // Equip player head with textures
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            if (head.getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
                PlayerProfile profile = Bukkit.createProfile(grave.getOwnerUuid(), grave.getOwnerName());
                if (grave.getSkinTextureValue() != null && !grave.getSkinTextureValue().isEmpty()) {
                    profile.setProperty(new com.destroystokyo.paper.profile.ProfileProperty("textures",
                            grave.getSkinTextureValue(), grave.getSkinTextureSignature()));
                }
                skullMeta.setPlayerProfile(profile);
                head.setItemMeta(skullMeta);
            }
            entity.getEquipment().setHelmet(head);
            entity.setPersistent(true);
        });
        grave.setVisualEntityUuid(stand.getUniqueId());
    }

    public void spawnHologram(GraveData grave) {
        Location baseLoc = grave.getLocation();
        if (baseLoc == null || baseLoc.getWorld() == null) return;

        double offset = config.getDisplayHeightOffset();
        Location holoLoc = baseLoc.clone().add(0.5, offset, 0.5);

        // Remove existing display entity if any
        if (grave.getDisplayEntityUuid() != null) {
            Entity existing = Bukkit.getEntity(grave.getDisplayEntityUuid());
            if (existing != null && existing.isValid()) {
                existing.remove();
            }
        }
        for (Entity e : holoLoc.getWorld().getNearbyEntities(holoLoc, 0.6, 0.6, 0.6)) {
            if (e instanceof TextDisplay) {
                e.remove();
            }
        }

        TextDisplay textDisplay = holoLoc.getWorld().spawn(holoLoc, TextDisplay.class, display -> {
            display.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "grave_id"),
                    org.bukkit.persistence.PersistentDataType.STRING, grave.getGraveId().toString());
            display.setBillboard(Display.Billboard.valueOf(config.getDisplayBillboard()));
            display.setDefaultBackground(false);
            display.setShadowed(config.isDisplayShadowed());

            // View distance multiplier: 64 blocks is default client distance (1.0f)
            float viewRangeMultiplier = (float) config.getDisplayViewDistanceBlocks() / 64.0f;
            display.setViewRange(Math.max(0.1f, viewRangeMultiplier));

            String bgHex = config.getDisplayBackgroundColor();
            if (bgHex == null || bgHex.equalsIgnoreCase("00000000") || bgHex.equalsIgnoreCase("000000") || bgHex.isEmpty()) {
                display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            } else {
                try {
                    long argb = Long.parseLong(bgHex, 16);
                    display.setBackgroundColor(Color.fromARGB((int) argb));
                } catch (Exception e) {
                    display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                }
            }

            display.text(buildHologramText(grave));
            display.setPersistent(true);
        });

        grave.setDisplayEntityUuid(textDisplay.getUniqueId());
    }

    public Component buildHologramText(GraveData grave) {
        List<String> lines = config.getDisplayLines();
        List<Component> components = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = sdf.format(new Date(grave.getDeathTimeMillis()));
        String remainingTimeStr = TimeUtil.formatDuration(grave.getRemainingSeconds());

        TagResolver resolver = TagResolver.resolver(
                Placeholder.parsed("player_name", grave.getOwnerName()),
                Placeholder.parsed("time_left", remainingTimeStr),
                Placeholder.parsed("items_count", String.valueOf(grave.getItems().size())),
                Placeholder.parsed("xp_stored", String.valueOf(grave.getStoredXp())),
                Placeholder.parsed("death_cause", grave.getDeathCause()),
                Placeholder.parsed("death_date", formattedDate),
                Placeholder.parsed("killer_name", grave.getKillerName()),
                Placeholder.parsed("killer_weapon", grave.getKillerWeapon()),
                Placeholder.parsed("world", grave.getLocation().getWorld() != null ? grave.getLocation().getWorld().getName() : "world"),
                Placeholder.parsed("x", String.valueOf(grave.getLocation().getBlockX())),
                Placeholder.parsed("y", String.valueOf(grave.getLocation().getBlockY())),
                Placeholder.parsed("z", String.valueOf(grave.getLocation().getBlockZ()))
        );

        for (int i = 0; i < lines.size(); i++) {
            Component parsed = MessageUtil.parse(lines.get(i), resolver);
            components.add(parsed);
            if (i < lines.size() - 1) {
                components.add(Component.newline());
            }
        }

        Component full = Component.empty();
        for (Component c : components) {
            full = full.append(c);
        }
        return full;
    }

    public void updateHologram(GraveData grave) {
        if (grave.getDisplayEntityUuid() == null) return;
        Entity entity = Bukkit.getEntity(grave.getDisplayEntityUuid());
        if (entity instanceof TextDisplay display && entity.isValid()) {
            display.text(buildHologramText(grave));
        }
    }

    public void removeVisual(GraveData grave) {
        Location loc = grave.getLocation();
        if (loc.getWorld() == null) return;

        // Remove TextDisplay
        if (grave.getDisplayEntityUuid() != null) {
            Entity display = Bukkit.getEntity(grave.getDisplayEntityUuid());
            if (display != null && display.isValid()) {
                display.remove();
            }
        }

        // Remove Visual Entity (ItemDisplay, BlockDisplay, ArmorStand)
        if (grave.getVisualEntityUuid() != null) {
            Entity visual = Bukkit.getEntity(grave.getVisualEntityUuid());
            if (visual != null && visual.isValid()) {
                visual.remove();
            }
        }

        // Remove block if block model
        Block block = loc.getBlock();
        if (block.getType() == Material.PLAYER_HEAD || block.getType() == Material.CHEST ||
                block.getType() == Material.BARREL || block.getType() == Material.ENDER_CHEST) {
            block.setType(Material.AIR, false);
        }
    }
}
