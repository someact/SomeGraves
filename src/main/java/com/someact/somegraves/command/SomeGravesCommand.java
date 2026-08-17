package com.someact.somegraves.command;

import com.someact.somegraves.SomeGravesPlugin;
import com.someact.somegraves.config.ConfigManager;
import com.someact.somegraves.gui.AdminConfigGUI;
import com.someact.somegraves.gui.AdminGravesGUI;
import com.someact.somegraves.gui.PlayerGravesGUI;
import com.someact.somegraves.item.GraveScrollManager;
import com.someact.somegraves.model.GraveData;
import com.someact.somegraves.storage.GraveStorageManager;
import com.someact.somegraves.tracker.GraveTrackerManager;
import com.someact.somegraves.util.MessageUtil;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Modern Paper BasicCommand implementation for /somegraves and /sg.
 */
public class SomeGravesCommand implements BasicCommand, CommandExecutor, TabCompleter {

    private final SomeGravesPlugin plugin;

    public SomeGravesCommand(SomeGravesPlugin plugin) {
        this.plugin = plugin;
    }

    private ConfigManager config() {
        return plugin.getConfigManager();
    }

    private GraveStorageManager storage() {
        return plugin.getStorageManager();
    }

    private GraveTrackerManager tracker() {
        return plugin.getTrackerManager();
    }

    private GraveScrollManager scrollManager() {
        return plugin.getScrollManager();
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        processCommand(stack.getSender(), args);
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        return processTabComplete(stack.getSender(), args);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        processCommand(sender, args);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return processTabComplete(sender, args);
    }

    private void processCommand(CommandSender sender, String[] args) {
        ConfigManager cfg = config();
        if (cfg == null) {
            sender.sendMessage("SomeGraves plugin is not fully initialized.");
            return;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Usage: /sg [admin|config|reload|givescroll]");
                return;
            }
            if (!player.hasPermission("somegraves.use")) {
                MessageUtil.sendMessage(player, cfg.getPrefix() + cfg.getMessage("no-permission",
                        "<red>You do not have permission to execute this command.</red>"));
                return;
            }
            new PlayerGravesGUI(plugin, player).open();
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "list", "menu" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command can only be run by a player.");
                    return;
                }
                if (!player.hasPermission("somegraves.use")) {
                    MessageUtil.sendMessage(player, cfg.getPrefix() + cfg.getMessage("no-permission",
                            "<red>You do not have permission to execute this command.</red>"));
                    return;
                }
                new PlayerGravesGUI(plugin, player).open();
            }
            case "track" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command can only be run by a player.");
                    return;
                }
                if (!player.hasPermission("somegraves.track")) {
                    MessageUtil.sendMessage(player, cfg.getPrefix() + cfg.getMessage("no-permission",
                            "<red>You do not have permission to execute this command.</red>"));
                    return;
                }
                if (args.length >= 2) {
                    try {
                        UUID graveId = UUID.fromString(args[1]);
                        GraveData grave = storage().getGraveById(graveId);
                        if (grave != null && !grave.isLooted()) {
                            tracker().startTracking(player, grave);
                            return;
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
                // Track most recent active grave
                List<GraveData> graves = storage().getActiveGravesForPlayer(player.getUniqueId());
                if (graves.isEmpty()) {
                    MessageUtil.sendMessage(player, cfg.getPrefix() + cfg.getMessage("no-graves",
                            "<yellow>You do not have any active gravestones.</yellow>"));
                    return;
                }
                tracker().startTracking(player, graves.get(0));
            }
            case "untrack", "stoptrack" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command can only be run by a player.");
                    return;
                }
                tracker().stopTracking(player);
            }
            case "admin" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command can only be run by a player.");
                    return;
                }
                if (!player.hasPermission("somegraves.admin")) {
                    MessageUtil.sendMessage(player, cfg.getPrefix() + cfg.getMessage("no-permission",
                            "<red>You do not have permission to execute this command.</red>"));
                    return;
                }
                new AdminGravesGUI(plugin, player).open();
            }
            case "config" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command can only be run by a player.");
                    return;
                }
                if (!player.hasPermission("somegraves.admin")) {
                    MessageUtil.sendMessage(player, cfg.getPrefix() + cfg.getMessage("no-permission",
                            "<red>You do not have permission to execute this command.</red>"));
                    return;
                }
                new AdminConfigGUI(plugin, player).open();
            }
            case "reload" -> {
                if (!sender.hasPermission("somegraves.admin")) {
                    MessageUtil.sendMessage(sender, cfg.getPrefix() + cfg.getMessage("no-permission",
                            "<red>You do not have permission to execute this command.</red>"));
                    return;
                }
                cfg.load();
                if (scrollManager() != null) scrollManager().reloadRecipe();
                MessageUtil.sendMessage(sender, cfg.getPrefix() + cfg.getMessage("config-reloaded",
                        "<green>Configuration and recipes reloaded successfully!</green>"));
            }
            case "givescroll" -> {
                if (!sender.hasPermission("somegraves.admin")) {
                    MessageUtil.sendMessage(sender, cfg.getPrefix() + cfg.getMessage("no-permission",
                            "<red>You do not have permission to execute this command.</red>"));
                    return;
                }
                if (args.length < 2) {
                    MessageUtil.sendMessage(sender, cfg.getPrefix() + "<yellow>Usage: /sg givescroll <player> [amount]</yellow>");
                    return;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    MessageUtil.sendMessage(sender, cfg.getPrefix() + "<red>Player not found.</red>");
                    return;
                }
                int amount = 1;
                if (args.length >= 3) {
                    try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {}
                }
                ItemStack scroll = scrollManager().createGraveScroll(amount);
                target.getInventory().addItem(scroll);
                MessageUtil.sendMessage(sender, cfg.getPrefix() + "<green>Gave " + amount + "x Grave Scroll to " + target.getName() + ".</green>");
                MessageUtil.sendMessage(target, cfg.getPrefix() + "<green>You received " + amount + "x Grave Teleport Scroll!</green>");
            }
            case "help" -> {
                showHelp(sender);
            }
            default -> {
                if (sender instanceof Player player) {
                    new PlayerGravesGUI(plugin, player).open();
                } else {
                    showHelp(sender);
                }
            }
        }
    }

    private void showHelp(CommandSender sender) {
        boolean isAdmin = sender.hasPermission("somegraves.admin");

        sender.sendMessage(MessageUtil.parse("<dark_gray>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬</dark_gray>"));
        sender.sendMessage(MessageUtil.parse("           <gradient:#9d4edd:#e0aaff><bold>SomeGraves Command Guide</bold></gradient>"));
        sender.sendMessage(MessageUtil.parse("<dark_gray>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬</dark_gray>"));

        sender.sendMessage(MessageUtil.parse("<gold><bold>/sg</bold></gold> <gray>or</gray> <gold><bold>/sg menu</bold></gold> <dark_gray>-</dark_gray> <white>Opens your active gravestones menu.</white>"));
        sender.sendMessage(MessageUtil.parse("<gold><bold>/sg track [grave_id]</bold></gold> <dark_gray>-</dark_gray> <white>Starts live on-screen compass tracking.</white>"));
        sender.sendMessage(MessageUtil.parse("<gold><bold>/sg untrack</bold></gold> <dark_gray>-</dark_gray> <white>Stops active compass tracking.</white>"));
        sender.sendMessage(MessageUtil.parse("<gold><bold>/sg help</bold></gold> <dark_gray>-</dark_gray> <white>Displays this help menu.</white>"));

        if (isAdmin) {
            sender.sendMessage(MessageUtil.parse(""));
            sender.sendMessage(MessageUtil.parse("<yellow><bold>Administrator Commands:</bold></yellow>"));
            sender.sendMessage(MessageUtil.parse("<aqua><bold>/sg admin</bold></aqua> <dark_gray>-</dark_gray> <white>View and manage all active gravestones on the server.</white>"));
            sender.sendMessage(MessageUtil.parse("<aqua><bold>/sg config</bold></aqua> <dark_gray>-</dark_gray> <white>Open in-game config panel & 3x3 crafting recipe editor.</white>"));
            sender.sendMessage(MessageUtil.parse("<aqua><bold>/sg reload</bold></aqua> <dark_gray>-</dark_gray> <white>Reload configuration file and recipe registry.</white>"));
            sender.sendMessage(MessageUtil.parse("<aqua><bold>/sg givescroll <player> [amount]</bold></aqua> <dark_gray>-</dark_gray> <white>Give Grave Teleport Scrolls to a player.</white>"));
        }
        sender.sendMessage(MessageUtil.parse("<dark_gray>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬</dark_gray>"));
    }

    private List<String> processTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("help", "list", "menu", "track", "untrack"));
            if (sender.hasPermission("somegraves.admin")) {
                subs.addAll(List.of("admin", "config", "reload", "givescroll"));
            }
            for (String s : subs) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(s);
                }
            }
            return completions;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("givescroll") && sender.hasPermission("somegraves.admin")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
            }
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("givescroll")) {
            completions.addAll(List.of("1", "4", "16", "64"));
            return completions;
        }

        return completions;
    }
}
