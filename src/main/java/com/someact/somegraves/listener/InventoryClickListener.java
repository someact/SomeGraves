package com.someact.somegraves.listener;

import com.someact.somegraves.gui.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Dispatches inventory click and close events to SomeGraves custom GUI holders.
 */
public class InventoryClickListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof GraveChestGUI gui) {
            gui.handleClick(event);
        } else if (holder instanceof PlayerGravesGUI gui) {
            gui.handleClick(event);
        } else if (holder instanceof AdminGravesGUI gui) {
            gui.handleClick(event);
        } else if (holder instanceof AdminConfigGUI gui) {
            gui.handleClick(event);
        } else if (holder instanceof RecipeEditorGUI gui) {
            gui.handleClick(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof GraveChestGUI gui) {
            gui.handleClose(event);
        } else if (holder instanceof RecipeEditorGUI gui) {
            gui.handleClose(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof PlayerGravesGUI || holder instanceof AdminGravesGUI || holder instanceof AdminConfigGUI) {
            event.setCancelled(true);
        }
    }
}
