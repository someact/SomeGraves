package com.someact.somegraves.listener;

import com.someact.somegraves.SomeGravesPlugin;
import com.someact.somegraves.util.MessageUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Captures chat input from players for interactive duration configuration.
 */
public class ChatInputListener implements Listener {

    private final SomeGravesPlugin plugin;
    private final Map<UUID, Consumer<String>> pendingInputs = new ConcurrentHashMap<>();

    public ChatInputListener(SomeGravesPlugin plugin) {
        this.plugin = plugin;
    }

    public void requestInput(Player player, Consumer<String> callback) {
        pendingInputs.put(player.getUniqueId(), callback);
    }

    public void cancelInput(Player player) {
        pendingInputs.remove(player.getUniqueId());
    }

    public boolean isPending(Player player) {
        return pendingInputs.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Consumer<String> callback = pendingInputs.remove(player.getUniqueId());

        if (callback != null) {
            event.setCancelled(true);

            String serialized = MessageUtil.miniMessage().serialize(event.message());
            String rawText = MessageUtil.miniMessage().stripTags(serialized).trim();

            player.getScheduler().run(plugin, task -> {
                callback.accept(rawText);
            }, null);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        pendingInputs.remove(event.getPlayer().getUniqueId());
    }
}
