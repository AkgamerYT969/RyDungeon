package com.therynzo.rydungeon.gui;

import com.therynzo.rydungeon.RyDungeonPlugin;
import com.therynzo.rydungeon.manager.DungeonManager;
import com.therynzo.rydungeon.model.DungeonData;
import com.therynzo.rydungeon.util.Text;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DungeonEditor {
    private final RyDungeonPlugin plugin;
    private final DungeonManager manager;

    public DungeonEditor(RyDungeonPlugin plugin, DungeonManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void open(Player player, String name) {
        DungeonData d = manager.get(name);
        if (d == null) {
            player.sendMessage(Text.color(plugin.msg("messages.missing").replace("%name%", name)));
            return;
        }

        int rows = Math.max(1, Math.min(6, plugin.getConfig().getInt("gui.rows", 6)));
        String title = Text.color(plugin.getConfig().getString("gui.title", "&8RyDungeon &7• &b%name%")
                .replace("%name%", d.getName()));

        Inventory inv = org.bukkit.Bukkit.createInventory(
                new DungeonMenuHolder(DungeonMenuHolder.Type.EDITOR, d.getName()),
                rows * 9,
                title
        );

        Material filler = material("gui.filler-material", Material.GRAY_STAINED_GLASS_PANE);
        Material accent = material("gui.accent-material", Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, new ItemStack(filler));
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, new ItemStack(accent));
            inv.setItem(inv.getSize() - 9 + i, new ItemStack(accent));
        }
        for (int row = 1; row < rows - 1; row++) {
            inv.setItem(row * 9, new ItemStack(accent));
            inv.setItem(row * 9 + 8, new ItemStack(accent));
        }

        set(inv, slot("spawn-point-slot", 10), configured("spawn-point", d, spawnValue(d)));
        set(inv, slot("boss-type-slot", 11), configured("boss-type", d, d.getBossType()));
        set(inv, slot("boss-name-slot", 12), configured("boss-name", d, Text.strip(d.getBossName())));
        set(inv, slot("health-slot", 13), configured("health", d,
                Text.formatNumber(d.getMaxHealthHearts()), d.getMaxHealth()));
        set(inv, slot("spawn-interval-slot", 14), configured("spawn-interval", d,
                DungeonData.formatDuration(d.getSpawnIntervalMillis())));
        set(inv, slot("alive-time-slot", 15), configured("alive-time", d,
                DungeonData.formatDuration(d.getAliveTimeMillis())));
        set(inv, slot("reward-slot", 16), configured("reward", d, Text.money(d.getRewardMoney())));

        set(inv, slot("bossbar-slot", 19), configured("bossbar", d,
                String.valueOf(d.isBossbarEnabled()), d.getBossbarMode()));
        set(inv, slot("bossbar-mode-slot", 20), configured("bossbar-mode", d, d.getBossbarMode()));
        set(inv, slot("bossbar-title-slot", 18), configured("bossbar-title", d, d.getBossbarTitle()));
        set(inv, slot("bossbar-color-slot", 21), configured("bossbar-color", d, d.getBossbarColor()));
        set(inv, slot("bossbar-style-slot", 22), configured("bossbar-style", d, d.getBossbarStyle()));
        set(inv, slot("bossbar-progress-slot", 23), configured("bossbar-progress", d, d.getProgressMode()));
        set(inv, slot("stack-order-slot", 24), configured("stack-order", d, d.getStackOrder()));
        set(inv, slot("darken-sky-slot", 25), configured("darken-sky", d, d.isDarkenSky()));

        set(inv, slot("boss-music-slot", 28), configured("boss-music", d, d.isPlayBossMusic()));
        set(inv, slot("boss-fog-slot", 29), configured("boss-fog", d, d.isCreateFog()));
        set(inv, slot("announcement-slot", 30), configured("announcement", d, d.isAnnouncementEnabled()));
        set(inv, slot("announcement-title-slot", 31), configured("announcement-title", d, d.getAnnouncementTitle()));
        set(inv, slot("announcement-subtitle-slot", 32), configured("announcement-subtitle", d, d.getAnnouncementSubtitle()));
        set(inv, slot("announcement-chat-slot", 33), configured("announcement-chat", d, d.getAnnouncementChat()));

        set(inv, slot("effects-slot", 34), configured("effects", d, d.isEffectsEnabled(), d.isLightning()));
        set(inv, slot("particle-slot", 35), configured("particle", d, d.getParticle()));
        set(inv, slot("spawn-sound-slot", 36), configured("spawn-sound", d, d.getSpawnSound()));
        set(inv, slot("spawn-command-slot", 37), configured("spawn-command", d,
                d.isSpawnCommandsEnabled(), d.getSpawnCommands().size()));

        set(inv, slot("save-slot", 45), configured("save", d, "SAVE"));
        set(inv, slot("join-slot", 46), configured("join", d, "JOIN"));
        set(inv, slot("delete-slot", 47), configured("delete", d, "DELETE"));
        set(inv, slot("close-slot", 49), configured("close", d, "CLOSE"));

        player.openInventory(inv);
        player.playSound(player.getLocation(), sound("gui.sounds.open", Sound.BLOCK_CHEST_OPEN), 1f, 1f);
    }

    public void openDeleteConfirm(Player player, String name) {
        Inventory inv = org.bukkit.Bukkit.createInventory(
                new DungeonMenuHolder(DungeonMenuHolder.Type.DELETE_CONFIRM, name),
                27,
                Text.color("&8Delete Dungeon &7• &c" + name)
        );

        Material filler = material("gui.accent-material", Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, new ItemStack(filler));

        inv.setItem(11, simple(Material.TNT, "&c&lCONFIRM DELETE",
                "&cPermanently delete the dungeon", "&cconfiguration and boss state."));
        inv.setItem(15, simple(Material.LIME_DYE, "&a&lCANCEL",
                "&7Return to the editor."));

        player.openInventory(inv);
        player.playSound(player.getLocation(), sound("gui.sounds.open", Sound.BLOCK_CHEST_OPEN), 1f, 1f);
    }

    private ItemStack configured(String key, DungeonData d, Object... values) {
        String base = "gui.items." + key + ".";
        Material material = material(base + "material", Material.PAPER);
        String name = plugin.getConfig().getString(base + "name", "&f" + key);

        List<String> lore = plugin.getConfig().getStringList(base + "lore");
        String value = values.length == 0 ? "" : String.valueOf(values[0]);
        String hp = values.length > 1 ? String.valueOf(values[1]) : Text.formatNumber(d.getMaxHealth());

        name = replace(name, value, hp, d);
        List<String> converted = new ArrayList<>();
        for (String line : lore) {
            converted.add(replace(line, value, hp, d));
        }

        return simple(material, name, converted.toArray(new String[0]));
    }

    private String replace(String text, String value, String hp, DungeonData d) {
        return text
                .replace("%value%", value)
                .replace("%hp%", hp)
                .replace("%world%", d.getSpawnPoint() == null || d.getSpawnPoint().getWorld() == null ? "NOT SET" : d.getSpawnPoint().getWorld().getName())
                .replace("%x%", d.getSpawnPoint() == null ? "-" : String.valueOf(d.getSpawnPoint().getBlockX()))
                .replace("%y%", d.getSpawnPoint() == null ? "-" : String.valueOf(d.getSpawnPoint().getBlockY()))
                .replace("%z%", d.getSpawnPoint() == null ? "-" : String.valueOf(d.getSpawnPoint().getBlockZ()))
                .replace("%mode%", d.getBossbarMode())
                .replace("%lightning%", String.valueOf(d.isLightning()))
                .replace("%particle%", d.getParticle())
                .replace("%count%", String.valueOf(d.getSpawnCommands().size()));
    }

    private String spawnValue(DungeonData d) {
        if (d.getSpawnPoint() == null || d.getSpawnPoint().getWorld() == null) return "NOT SET";
        return d.getSpawnPoint().getWorld().getName() + " "
                + d.getSpawnPoint().getBlockX() + ","
                + d.getSpawnPoint().getBlockY() + ","
                + d.getSpawnPoint().getBlockZ();
    }

    private ItemStack simple(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Text.color(name));
        meta.setLore(java.util.Arrays.stream(lore).map(Text::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    private void set(Inventory inv, int slot, ItemStack item) {
        if (slot >= 0 && slot < inv.getSize()) inv.setItem(slot, item);
    }

    private int slot(String key, int fallback) {
        return plugin.getConfig().getInt("gui.editor." + key, fallback);
    }

    private Material material(String path, Material fallback) {
        Material m = Material.matchMaterial(plugin.getConfig().getString(path, fallback.name()));
        return m == null ? fallback : m;
    }

    private Sound sound(String path, Sound fallback) {
        try { return Sound.valueOf(plugin.getConfig().getString(path, fallback.name())); }
        catch (Exception ignored) { return fallback; }
    }
}
