package com.someact.somegraves.api.event;

import com.someact.somegraves.model.GraveData;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player attempts to teleport to a gravestone using a Grave Scroll.
 */
public class GraveTeleportEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final GraveData graveData;
    private boolean cancelled = false;

    public GraveTeleportEvent(Player player, GraveData graveData) {
        this.player = player;
        this.graveData = graveData;
    }

    public Player getPlayer() {
        return player;
    }

    public GraveData getGraveData() {
        return graveData;
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
