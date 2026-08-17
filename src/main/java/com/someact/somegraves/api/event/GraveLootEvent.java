package com.someact.somegraves.api.event;

import com.someact.somegraves.model.GraveData;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player attempts to loot a gravestone.
 */
public class GraveLootEvent extends Event implements Cancellable {

    public enum LootType {
        INSTANT_AUTO_EQUIP,
        CHEST_GUI,
        TAKE_ALL_BUTTON
    }

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final GraveData graveData;
    private final LootType lootType;
    private boolean cancelled = false;

    public GraveLootEvent(Player player, GraveData graveData, LootType lootType) {
        this.player = player;
        this.graveData = graveData;
        this.lootType = lootType;
    }

    public Player getPlayer() {
        return player;
    }

    public GraveData getGraveData() {
        return graveData;
    }

    public LootType getLootType() {
        return lootType;
    }

    public boolean isOwner() {
        return player.getUniqueId().equals(graveData.getOwnerUuid());
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
