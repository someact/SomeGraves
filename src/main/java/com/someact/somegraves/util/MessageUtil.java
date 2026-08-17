package com.someact.somegraves.util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adventure MiniMessage text utilities with support for custom fonts and resource pack UI tags.
 */
public final class MessageUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private MessageUtil() {}

    public static MiniMessage miniMessage() {
        return MINI_MESSAGE;
    }

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(input);
    }

    public static Component parse(String input, TagResolver... tagResolvers) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(input, tagResolvers);
    }

    public static Component parse(String input, Map<String, String> placeholders) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        List<TagResolver> resolvers = new ArrayList<>();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolvers.add(Placeholder.parsed(entry.getKey(), entry.getValue()));
        }
        return MINI_MESSAGE.deserialize(input, TagResolver.resolver(resolvers));
    }

    public static void sendMessage(Audience audience, String message) {
        if (message == null || message.isEmpty()) return;
        audience.sendMessage(parse(message));
    }

    public static void sendMessage(Audience audience, String message, TagResolver... tagResolvers) {
        if (message == null || message.isEmpty()) return;
        audience.sendMessage(parse(message, tagResolvers));
    }

    public static void sendActionBar(Player player, String message) {
        if (message == null || message.isEmpty()) return;
        player.sendActionBar(parse(message));
    }

    public static void sendActionBar(Player player, String message, TagResolver... tagResolvers) {
        if (message == null || message.isEmpty()) return;
        player.sendActionBar(parse(message, tagResolvers));
    }
}
