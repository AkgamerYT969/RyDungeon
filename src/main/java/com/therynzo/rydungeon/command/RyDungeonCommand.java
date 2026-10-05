package com.therynzo.rydungeon.command;

import com.therynzo.rydungeon.RyDungeonPlugin;
import com.therynzo.rydungeon.gui.DungeonEditor;
import com.therynzo.rydungeon.manager.DungeonManager;
import com.therynzo.rydungeon.model.DungeonData;
import com.therynzo.rydungeon.util.Text;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public final class RyDungeonCommand implements CommandExecutor, TabCompleter {
    private final RyDungeonPlugin plugin;
    private final DungeonManager manager;
    private final DungeonEditor editor;

    public RyDungeonCommand(RyDungeonPlugin plugin, DungeonManager manager, DungeonEditor editor) {
        this.plugin = plugin;
        this.manager = manager;
        this.editor = editor;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Text.color(plugin.msg("messages.usage")));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("join")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Text.color(plugin.msg("messages.player-only")));
                return true;
            }
            if (!player.hasPermission("rydungeon.join")) {
                player.sendMessage(Text.color(plugin.msg("messages.no-permission")));
                return true;
            }

            DungeonData d;
            if (args.length >= 2) {
                d = manager.get(args[1]);
            } else {
                if (manager.all().isEmpty()) {
                    player.sendMessage(Text.color(plugin.msg("messages.nothing-to-join")));
                    return true;
                }
                if (manager.all().size() > 1) {
                    player.sendMessage(Text.color(plugin.msg("messages.join-multiple")));
                    return true;
                }
                d = manager.all().iterator().next();
            }

            if (d == null) {
                player.sendMessage(Text.color(plugin.msg("messages.missing")
                        .replace("%name%", args.length >= 2 ? args[1] : "")));
                return true;
            }
            if (d.getSpawnPoint() == null) {
                player.sendMessage(Text.color(plugin.msg("messages.no-spawn")));
                return true;
            }

            player.teleport(d.getSpawnPoint());
            player.sendMessage(Text.color(plugin.msg("messages.joined").replace("%name%", d.getName())));
            player.playSound(player.getLocation(),
                    org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            return true;
        }

        if (!sender.hasPermission("rydungeon.admin")) {
            sender.sendMessage(Text.color(plugin.msg("messages.no-permission")));
            return true;
        }

        switch (sub) {
            case "create" -> {
                if (args.length < 2) {
                    sender.sendMessage(Text.color("/rydungeon create <name>"));
                    return true;
                }
                String name = args[1];
                if (!DungeonManager.validName(name)) {
                    sender.sendMessage(Text.color("&cName must be 1-32 characters using letters, numbers, _ or -."));
                    return true;
                }
                if (manager.get(name) != null) {
                    sender.sendMessage(Text.color(plugin.msg("messages.exists").replace("%name%", name)));
                    return true;
                }

                Player player = sender instanceof Player ? (Player) sender : null;
                if (!manager.create(name, player)) {
                    sender.sendMessage(Text.color(plugin.msg("messages.max-dungeons")));
                    return true;
                }

                sender.sendMessage(Text.color(plugin.msg("messages.created").replace("%name%", name)));
                if (player != null) editor.open(player, name);
                else sender.sendMessage(Text.color(plugin.msg("messages.console-created")));
            }
            case "edit" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Text.color(plugin.msg("messages.player-only")));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(Text.color("/rydungeon edit <name>"));
                    return true;
                }
                if (manager.get(args[1]) == null) {
                    player.sendMessage(Text.color(plugin.msg("messages.missing").replace("%name%", args[1])));
                    return true;
                }
                editor.open(player, args[1]);
            }
            case "delete" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Text.color(plugin.msg("messages.player-only")));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(Text.color("/rydungeon delete <name>"));
                    return true;
                }
                if (manager.get(args[1]) == null) {
                    player.sendMessage(Text.color(plugin.msg("messages.missing").replace("%name%", args[1])));
                    return true;
                }
                editor.openDeleteConfirm(player, args[1]);
            }
            case "list" -> {
                sender.sendMessage(Text.color("&b&lRyDungeon &8• &7Dungeons"));
                for (DungeonData d : manager.all()) {
                    String status = manager.activeBoss(d) != null ? "&aLIVE" :
                            "&e" + Text.formatDuration(Math.max(0L, d.getNextSpawnAt() - System.currentTimeMillis()));
                    sender.sendMessage(Text.color("&7- &f" + d.getName()
                            + " &8| &7Boss: &f" + Text.strip(d.getBossName())
                            + " &8| &7Status: " + status));
                }
            }
            case "reload" -> {
                plugin.reloadConfig();
                manager.reloadAll();
                manager.setupEconomy();
                plugin.getBossBarManager().rebuild();
                sender.sendMessage(Text.color(plugin.msg("messages.reloaded")));
            }
            default -> sender.sendMessage(Text.color(plugin.msg("messages.usage")));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("create", "edit", "delete", "join", "list", "reload").stream()
                    .filter(v -> v.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && Set.of("edit", "delete", "join").contains(args[0].toLowerCase(Locale.ROOT))) {
            return manager.all().stream()
                    .map(DungeonData::getName)
                    .filter(v -> v.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        return List.of();
    }
}
