package com.therynzo.rydungeon.manager;

import com.therynzo.rydungeon.RyDungeonPlugin;
import com.therynzo.rydungeon.gui.DungeonEditor;
import com.therynzo.rydungeon.model.DungeonData;
import com.therynzo.rydungeon.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class InputManager implements Listener {
    public enum InputType {
        BOSS_TYPE, BOSS_NAME, HEALTH_HEARTS, SPAWN_INTERVAL, ALIVE_TIME, REWARD,
        BOSSBAR_TITLE, ANNOUNCEMENT_TITLE, ANNOUNCEMENT_SUBTITLE, ANNOUNCEMENT_CHAT,
        SPAWN_COMMANDS
    }

    public record Pending(String dungeon, InputType type) {}

    private final RyDungeonPlugin plugin;
    private final DungeonManager manager;
    private final DungeonEditor editor;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public InputManager(RyDungeonPlugin plugin, DungeonManager manager, DungeonEditor editor) {
        this.plugin = plugin;
        this.manager = manager;
        this.editor = editor;
    }

    public void set(Player player, String dungeon, InputType type) {
        pending.put(player.getUniqueId(), new Pending(dungeon, type));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Pending p = pending.get(player.getUniqueId());
        if (p == null) return;

        event.setCancelled(true);
        String message = event.getMessage().trim();

        if (message.equalsIgnoreCase("cancel")) {
            pending.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(Text.color(plugin.msg("messages.input-cancelled")));
                editor.open(player, p.dungeon());
            });
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> apply(player, p, message));
    }

    private void apply(Player player, Pending pendingState, String input) {
        DungeonData d = manager.get(pendingState.dungeon());
        if (d == null) {
            pending.remove(player.getUniqueId());
            return;
        }

        String error = null;

        switch (pendingState.type()) {
            case BOSS_TYPE -> {
                if (!manager.validateBossType(input)) {
                    error = plugin.msg("messages.invalid-type");
                } else {
                    d.setBossType(input);
                }
            }
            case BOSS_NAME -> d.setBossName(input);
            case HEALTH_HEARTS -> {
                try {
                    double hearts = Double.parseDouble(input);
                    if (hearts <= 0) throw new NumberFormatException();
                    d.setMaxHealthHearts(hearts);
                    player.sendMessage(Text.color(plugin.msg("messages.health-set")
                            .replace("%hearts%", Text.formatNumber(hearts))
                            .replace("%hp%", Text.formatNumber(hearts * 2.0))));
                } catch (NumberFormatException ex) {
                    error = plugin.msg("messages.invalid-number");
                }
            }
            case SPAWN_INTERVAL -> {
                long duration = DungeonData.parseDuration(input, -1L);
                if (duration < 1000L) error = plugin.msg("messages.invalid-duration");
                else {
                    d.setSpawnIntervalMillis(duration);
                    d.setNextSpawnAt(System.currentTimeMillis() + duration);
                }
            }
            case ALIVE_TIME -> {
                long duration = DungeonData.parseDuration(input, -1L);
                if (duration < 1000L) error = plugin.msg("messages.invalid-duration");
                else d.setAliveTimeMillis(duration);
            }
            case REWARD -> {
                try {
                    double money = Double.parseDouble(input);
                    if (money < 0) throw new NumberFormatException();
                    d.setRewardMoney(money);
                } catch (NumberFormatException ex) {
                    error = plugin.msg("messages.invalid-number");
                }
            }
            case BOSSBAR_TITLE -> d.setBossbarTitle(input);
            case ANNOUNCEMENT_TITLE -> d.setAnnouncementTitle(input);
            case ANNOUNCEMENT_SUBTITLE -> d.setAnnouncementSubtitle(input);
            case ANNOUNCEMENT_CHAT -> d.setAnnouncementChat(input);
            case SPAWN_COMMANDS -> {
                if (input.equalsIgnoreCase("clear")) {
                    d.setSpawnCommands(List.of());
                    d.setSpawnCommandsEnabled(false);
                } else {
                    List<String> commands = Arrays.stream(input.split(";"))
                            .map(String::trim)
                            .filter(s -> !s.isBlank())
                            .toList();
                    d.setSpawnCommands(commands);
                    d.setSpawnCommandsEnabled(!commands.isEmpty());
                    player.sendMessage(Text.color(plugin.msg("messages.commands-saved")));
                }
            }
        }

        pending = null;
        this.pending.remove(player.getUniqueId());

        if (error != null) {
            player.sendMessage(Text.color(error));
            editor.open(player, d.getName());
            return;
        }

        manager.save(d);
        if (pendingState.type() == InputType.SPAWN_COMMANDS) {
            // already told the player
        } else {
            player.sendMessage(Text.color(plugin.msg("messages.saved").replace("%name%", d.getName())));
        }

        if (pendingState.type() == InputType.SPAWN_INTERVAL
                || pendingState.type() == InputType.ALIVE_TIME
                || pendingState.type() == InputType.REWARD
                || pendingState.type() == InputType.HEALTH_HEARTS
                || pendingState.type() == InputType.BOSS_TYPE
                || pendingState.type() == InputType.BOSSBAR_TITLE) {
            plugin.getBossBarManager().rebuild();
        }

        editor.open(player, d.getName());
    }
}
