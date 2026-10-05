package com.therynzo.rydungeon.util;

import com.therynzo.rydungeon.model.DungeonData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.Locale;

public final class Text {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Text() {}

    public static String color(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }

    public static Component component(String text) {
        return LEGACY.deserialize(text == null ? "" : text);
    }

    public static String strip(String text) {
        return ChatColor.stripColor(color(text));
    }

    public static String apply(String text, DungeonData d, String status, long remainingMillis) {
        String out = text == null ? "" : text;
        Location loc = d.getSpawnPoint();
        String boss = strip(d.getBossName());
        double hearts = d.getMaxHealth() / 2.0;

        out = out.replace("%name%", d.getName());
        out = out.replace("%boss%", boss);
        out = out.replace("%status%", status == null ? "" : status);
        out = out.replace("%time%", formatDuration(remainingMillis));
        out = out.replace("%reward%", money(d.getRewardMoney()));
        out = out.replace("%hearts%", formatNumber(hearts));
        out = out.replace("%max-hearts%", formatNumber(hearts));
        out = out.replace("%hp%", formatNumber(d.getMaxHealth()));
        out = out.replace("%max-health%", formatNumber(d.getMaxHealth()));
        out = out.replace("%spawn-interval%", DungeonData.formatDuration(d.getSpawnIntervalMillis()));
        out = out.replace("%alive-time%", DungeonData.formatDuration(d.getAliveTimeMillis()));

        if (loc != null) {
            out = out.replace("%world%", loc.getWorld() == null ? "" : loc.getWorld().getName());
            out = out.replace("%x%", String.valueOf(loc.getBlockX()));
            out = out.replace("%y%", String.valueOf(loc.getBlockY()));
            out = out.replace("%z%", String.valueOf(loc.getBlockZ()));
        } else {
            out = out.replace("%world%", "");
            out = out.replace("%x%", "");
            out = out.replace("%y%", "");
            out = out.replace("%z%", "");
        }
        return out;
    }

    public static String formatDuration(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        long secs = seconds % 60;

        if (days > 0) return String.format(Locale.US, "%dd %02dh %02dm", days, hours, minutes);
        if (hours > 0) return String.format(Locale.US, "%02dh %02dm %02ds", hours, minutes, secs);
        if (minutes > 0) return String.format(Locale.US, "%02dm %02ds", minutes, secs);
        return String.format(Locale.US, "%02ds", secs);
    }

    public static String money(double amount) {
        return String.format(Locale.US, "%,.2f", amount);
    }

    public static String formatNumber(double amount) {
        if (amount == Math.rint(amount)) return String.format(Locale.US, "%.0f", amount);
        return String.format(Locale.US, "%.2f", amount);
    }
}
