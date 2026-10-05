package com.therynzo.rydungeon.listener;

import com.therynzo.rydungeon.RyDungeonPlugin;
import com.therynzo.rydungeon.gui.DungeonEditor;
import com.therynzo.rydungeon.gui.DungeonMenuHolder;
import com.therynzo.rydungeon.manager.InputManager;
import com.therynzo.rydungeon.manager.DungeonManager;
import com.therynzo.rydungeon.model.DungeonData;
import com.therynzo.rydungeon.util.Text;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Locale;

public final class DungeonGuiListener implements Listener {
    private final RyDungeonPlugin plugin;
    private final DungeonManager manager;
    private final DungeonEditor editor;

    public DungeonGuiListener(RyDungeonPlugin plugin, DungeonManager manager, DungeonEditor editor) {
        this.plugin = plugin;
        this.manager = manager;
        this.editor = editor;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof DungeonMenuHolder holder)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;

        click(player);

        if (holder.type() == DungeonMenuHolder.Type.DELETE_CONFIRM) {
            if (slot == 11) {
                String name = holder.dungeon();
                if (manager.delete(name)) {
                    plugin.getBossBarManager().removeDungeon(name);
                    player.closeInventory();
                    player.sendMessage(Text.color(plugin.msg("messages.deleted").replace("%name%", name)));
                    success(player);
                } else {
                    error(player);
                }
            } else if (slot == 15) {
                editor.open(player, holder.dungeon());
            }
            return;
        }

        DungeonData d = manager.get(holder.dungeon());
        if (d == null) {
            player.closeInventory();
            return;
        }

        int s;

        s = cfg("spawn-point-slot", 10);
        if (slot == s) {
            d.setSpawnPoint(player.getLocation());
            d.setNextSpawnAt(System.currentTimeMillis() + d.getSpawnIntervalMillis());
            manager.save(d);
            player.sendMessage(Text.color(plugin.msg("messages.spawn-set")));
            editor.open(player, d.getName());
            return;
        }

        s = cfg("boss-type-slot", 11);
        if (slot == s) {
            input(player, d, InputManager.InputType.BOSS_TYPE);
            return;
        }

        s = cfg("boss-name-slot", 12);
        if (slot == s) {
            input(player, d, InputManager.InputType.BOSS_NAME);
            return;
        }

        s = cfg("health-slot", 13);
        if (slot == s) {
            input(player, d, InputManager.InputType.HEALTH_HEARTS);
            return;
        }

        s = cfg("spawn-interval-slot", 14);
        if (slot == s) {
            input(player, d, InputManager.InputType.SPAWN_INTERVAL);
            return;
        }

        s = cfg("alive-time-slot", 15);
        if (slot == s) {
            input(player, d, InputManager.InputType.ALIVE_TIME);
            return;
        }

        s = cfg("reward-slot", 16);
        if (slot == s) {
            input(player, d, InputManager.InputType.REWARD);
            return;
        }

        s = cfg("bossbar-slot", 19);
        if (slot == s) {
            d.setBossbarEnabled(!d.isBossbarEnabled());
            manager.save(d);
            plugin.getBossBarManager().rebuild();
            editor.open(player, d.getName());
            return;
        }

        s = cfg("bossbar-mode-slot", 20);
        if (slot == s) {
            d.setBossbarMode(next(d.getBossbarMode(), "BOSSBAR", "ACTIONBAR", "HIDDEN"));
            manager.save(d);
            plugin.getBossBarManager().rebuild();
            editor.open(player, d.getName());
            return;
        }

        s = cfg("bossbar-title-slot", 18);
        if (slot == s) {
            input(player, d, InputManager.InputType.BOSSBAR_TITLE);
            return;
        }

        s = cfg("bossbar-color-slot", 21);
        if (slot == s) {
            d.setBossbarColor(next(d.getBossbarColor(), "PINK", "BLUE", "RED", "GREEN", "YELLOW", "PURPLE", "WHITE"));
            manager.save(d);
            plugin.getBossBarManager().rebuild();
            editor.open(player, d.getName());
            return;
        }

        s = cfg("bossbar-style-slot", 22);
        if (slot == s) {
            d.setBossbarStyle(next(d.getBossbarStyle(), "SOLID", "SEGMENTED_6", "SEGMENTED_10", "SEGMENTED_12", "SEGMENTED_20"));
            manager.save(d);
            plugin.getBossBarManager().rebuild();
            editor.open(player, d.getName());
            return;
        }

        s = cfg("bossbar-progress-slot", 23);
        if (slot == s) {
            d.setProgressMode(next(d.getProgressMode(), "HEALTH", "TIMER", "FULL"));
            manager.save(d);
            plugin.getBossBarManager().rebuild();
            editor.open(player, d.getName());
            return;
        }

        s = cfg("stack-order-slot", 24);
        if (slot == s) {
            d.setStackOrder(d.getStackOrder() >= 7 ? 1 : d.getStackOrder() + 1);
            manager.save(d);
            plugin.getBossBarManager().rebuild();
            editor.open(player, d.getName());
            return;
        }

        s = cfg("darken-sky-slot", 25);
        if (slot == s) {
            d.setDarkenSky(!d.isDarkenSky());
            manager.save(d);
            plugin.getBossBarManager().rebuild();
            editor.open(player, d.getName());
            return;
        }

        s = cfg("boss-music-slot", 28);
        if (slot == s) {
            d.setPlayBossMusic(!d.isPlayBossMusic());
            manager.save(d);
            plugin.getBossBarManager().rebuild();
            editor.open(player, d.getName());
            return;
        }

        s = cfg("boss-fog-slot", 29);
        if (slot == s) {
            d.setCreateFog(!d.isCreateFog());
            manager.save(d);
            plugin.getBossBarManager().rebuild();
            editor.open(player, d.getName());
            return;
        }

        s = cfg("announcement-slot", 30);
        if (slot == s) {
            d.setAnnouncementEnabled(!d.isAnnouncementEnabled());
            manager.save(d);
            editor.open(player, d.getName());
            return;
        }

        s = cfg("announcement-title-slot", 31);
        if (slot == s) {
            input(player, d, InputManager.InputType.ANNOUNCEMENT_TITLE);
            return;
        }

        s = cfg("announcement-subtitle-slot", 32);
        if (slot == s) {
            input(player, d, InputManager.InputType.ANNOUNCEMENT_SUBTITLE);
            return;
        }

        s = cfg("announcement-chat-slot", 33);
        if (slot == s) {
            input(player, d, InputManager.InputType.ANNOUNCEMENT_CHAT);
            return;
        }

        s = cfg("effects-slot", 34);
        if (slot == s) {
            d.setEffectsEnabled(!d.isEffectsEnabled());
            manager.save(d);
            editor.open(player, d.getName());
            return;
        }

        s = cfg("particle-slot", 35);
        if (slot == s) {
            d.setParticle(next(d.getParticle(), "EXPLOSION", "FLAME", "SOUL_FIRE_FLAME", "END_ROD", "TOTEM_OF_UNDYING", "HEART", "ENCHANT"));
            manager.save(d);
            editor.open(player, d.getName());
            return;
        }

        s = cfg("spawn-sound-slot", 36);
        if (slot == s) {
            d.setSpawnSound(next(d.getSpawnSound(),
                    "ENTITY_WITHER_SPAWN",
                    "ENTITY_LIGHTNING_BOLT_THUNDER",
                    "ENTITY_ENDER_DRAGON_GROWL",
                    "ENTITY_WARDEN_EMERGE",
                    "ENTITY_WITHER_DEATH"));
            manager.save(d);
            editor.open(player, d.getName());
            return;
        }

        s = cfg("spawn-command-slot", 37);
        if (slot == s) {
            input(player, d, InputManager.InputType.SPAWN_COMMANDS);
            return;
        }

        s = cfg("save-slot", 45);
        if (slot == s) {
            manager.save(d);
            plugin.getBossBarManager().rebuild();
            player.sendMessage(Text.color(plugin.msg("messages.saved").replace("%name%", d.getName())));
            success(player);
            editor.open(player, d.getName());
            return;
        }

        s = cfg("join-slot", 46);
        if (slot == s) {
            if (d.getSpawnPoint() == null) {
                player.sendMessage(Text.color(plugin.msg("messages.no-spawn")));
                error(player);
                return;
            }
            player.teleport(d.getSpawnPoint());
            player.playSound(player.getLocation(), sound("gui.sounds.teleport", Sound.ENTITY_ENDERMAN_TELEPORT), 1f, 1f);
            player.sendMessage(Text.color(plugin.msg("messages.joined").replace("%name%", d.getName())));
            return;
        }

        s = cfg("delete-slot", 47);
        if (slot == s) {
            player.sendMessage(Text.color(plugin.msg("messages.delete-warning")));
            editor.openDeleteConfirm(player, d.getName());
            return;
        }

        s = cfg("close-slot", 49);
        if (slot == s) {
            player.closeInventory();
        }
    }

    private void input(Player p, DungeonData d, InputManager.InputType type) {
        p.closeInventory();
        p.sendMessage(Text.color(plugin.getConfig().getString("input-prefix", "&7Type value or cancel.")));
        plugin.getInputManager().set(p, d.getName(), type);
    }

    private int cfg(String key, int fallback) {
        return plugin.getConfig().getInt("gui.editor." + key, fallback);
    }

    private String next(String current, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(current)) return values[(i + 1) % values.length];
        }
        return values[0];
    }

    private Material material(String path, Material fallback) {
        Material m = Material.matchMaterial(plugin.getConfig().getString(path, fallback.name()));
        return m == null ? fallback : m;
    }

    private Sound sound(String path, Sound fallback) {
        try { return Sound.valueOf(plugin.getConfig().getString(path, fallback.name())); }
        catch (Exception ignored) { return fallback; }
    }

    private void click(Player p) {
        p.playSound(p.getLocation(), sound("gui.sounds.click", Sound.UI_BUTTON_CLICK), 1f, 1f);
    }

    private void success(Player p) {
        p.playSound(p.getLocation(), sound("gui.sounds.success", Sound.ENTITY_PLAYER_LEVELUP), 1f, 1f);
    }

    private void error(Player p) {
        p.playSound(p.getLocation(), sound("gui.sounds.error", Sound.ENTITY_VILLAGER_NO), 1f, 1f);
    }
}
