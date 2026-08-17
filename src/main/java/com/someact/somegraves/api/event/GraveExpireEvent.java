package com.someact.somegraves.api.event;

import com.someact.somegraves.model.GraveData;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a gravestone's timer expires.
 */
public class GraveExpireEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final GraveData graveData;
    private boolean cancelled = false;

    public GraveExpireEvent(GraveData graveData) {
        this.graveData = graveData;
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
