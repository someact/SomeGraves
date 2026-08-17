package com.someact.somegraves.api.event;

import com.someact.somegraves.model.GraveData;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player starts or stops compass tracking for a gravestone.
 */
public class GraveTrackEvent extends Event implements Cancellable {

    public enum TrackAction {
        START,
        STOP,
        ARRIVED
    }

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final GraveData graveData;
    private final TrackAction action;
    private boolean cancelled = false;

    public GraveTrackEvent(Player player, GraveData graveData, TrackAction action) {
        this.player = player;
        this.graveData = graveData;
        this.action = action;
    }

    public Player getPlayer() {
        return player;
    }

    public GraveData getGraveData() {
        return graveData;
    }

    public TrackAction getAction() {
        return action;
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
