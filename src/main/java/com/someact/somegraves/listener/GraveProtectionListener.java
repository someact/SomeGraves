package com.someact.somegraves.listener;

import com.someact.somegraves.SomeGravesPlugin;
import com.someact.somegraves.config.ConfigManager;
import com.someact.somegraves.storage.GraveStorageManager;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;

/**
 * Protects gravestones against explosions, block breaking, pistons, and fluid flow.
 */
public class GraveProtectionListener implements Listener {

    private final SomeGravesPlugin plugin;
    private final ConfigManager config;
    private final GraveStorageManager storage;

    public GraveProtectionListener(SomeGravesPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.storage = plugin.getStorageManager();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!config.isProtectFromDamage()) return;
        if (event.getPlayer().hasPermission("somegraves.bypass.protection")) return;

        if (storage.getGraveAtLocation(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!config.isProtectFromDamage()) return;
        event.blockList().removeIf(block -> storage.getGraveAtLocation(block.getLocation()) != null);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!config.isProtectFromDamage()) return;
        event.blockList().removeIf(block -> storage.getGraveAtLocation(block.getLocation()) != null);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!config.isProtectFromDamage()) return;
        for (Block b : event.getBlocks()) {
            if (storage.getGraveAtLocation(b.getLocation()) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!config.isProtectFromDamage()) return;
        for (Block b : event.getBlocks()) {
            if (storage.getGraveAtLocation(b.getLocation()) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (!config.isProtectFromDamage()) return;
        if (storage.getGraveAtLocation(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (!config.isProtectFromDamage()) return;
        if (storage.getGraveAtLocation(event.getToBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }
}
