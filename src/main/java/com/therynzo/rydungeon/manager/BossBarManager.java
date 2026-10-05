package com.therynzo.rydungeon.manager;

import com.therynzo.rydungeon.RyDungeonPlugin;
import com.therynzo.rydungeon.model.DungeonData;
import com.therynzo.rydungeon.util.Text;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;

public final class BossBarManager {
    private final RyDungeonPlugin plugin;
    private final DungeonManager manager;
    private final Map<String, BossBar> bars = new HashMap<>();

    public BossBarManager(RyDungeonPlugin plugin, DungeonManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void rebuild() {
        for (BossBar bar : bars.values()) {
            bar.removeAll();
            bar.setVisible(false);
        }
        bars.clear();

        List<DungeonData> data = manager.all().stream()
                .filter(DungeonData::isBossbarEnabled)
                .filter(d -> d.getBossbarMode().equalsIgnoreCase("BOSSBAR"))
                .sorted(Comparator.comparingInt(DungeonData::getStackOrder)
                        .thenComparing(DungeonData::getName, String.CASE_INSENSITIVE_ORDER))
                .limit(plugin.getConfig().getInt("settings.max-visible-bossbars", 7))
                .toList();

        for (DungeonData d : data) {
            NamespacedKey key = new NamespacedKey(plugin, "bar_" + safeKey(d.getName()));
            Bukkit.removeBossBar(key);

            BossBar bar = Bukkit.createBossBar(
                    key,
                    Text.color(renderTitle(d)),
                    parseColor(d.getBossbarColor()),
                    parseStyle(d.getBossbarStyle())
            );
            applyFlags(bar, d);
            bars.put(d.getName(), bar);
        }

        tick();
    }

    public void tick() {
        // Update BOSSBAR mode globally, independent of world.
        for (DungeonData d : manager.all()) {
            if (!d.isBossbarEnabled() || !d.getBossbarMode().equalsIgnoreCase("BOSSBAR")) {
                hide(d);
                continue;
            }

            BossBar bar = bars.get(d.getName());
            if (bar == null) {
                continue;
            }

            bar.setTitle(Text.color(renderTitle(d)));
            bar.setColor(parseColor(d.getBossbarColor()));
            bar.setStyle(parseStyle(d.getBossbarStyle()));
            bar.setProgress(progress(d));
            applyFlags(bar, d);

            for (Player p : Bukkit.getOnlinePlayers()) {
                bar.addPlayer(p);
            }
            bar.setVisible(true);
        }

        // ACTIONBAR is global too. Native ActionBar is a single lower HUD lane,
        // so multiple ACTIONBAR-mode dungeons are rotated.
        List<DungeonData> actionbars = manager.all().stream()
                .filter(DungeonData::isBossbarEnabled)
                .filter(d -> d.getBossbarMode().equalsIgnoreCase("ACTIONBAR"))
                .sorted(Comparator.comparingInt(DungeonData::getStackOrder)
                        .thenComparing(DungeonData::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (!actionbars.isEmpty()) {
            int index = (int) ((System.currentTimeMillis() / 1500L) % actionbars.size());
            String text = renderTitle(actionbars.get(index));
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendActionBar(Text.component(text));
            }
        }
    }

    public void addPlayer(Player p) {
        for (BossBar bar : bars.values()) bar.addPlayer(p);
    }

    public void removePlayer(Player p) {
        for (BossBar bar : bars.values()) bar.removePlayer(p);
    }

    public void announceSpawn(DungeonData d, LivingEntity boss) {
        String title = Text.apply(d.getAnnouncementTitle(), d, "LIVE", d.getAliveTimeMillis());
        String subtitle = Text.apply(d.getAnnouncementSubtitle(), d, "LIVE", d.getAliveTimeMillis());
        String chat = Text.apply(d.getAnnouncementChat(), d, "LIVE", d.getAliveTimeMillis());

        if (!d.isAnnouncementEnabled()) return;

        Title built = Title.title(
                Text.component(title),
                Text.component(subtitle),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(4), Duration.ofMillis(500))
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(built);
            p.sendMessage(Text.color(chat));
        }
    }

    public void playSpawnEffects(DungeonData d, Location loc) {
        if (!d.isEffectsEnabled() || loc == null || loc.getWorld() == null) return;

        World world = loc.getWorld();

        if (d.isLightning()) {
            world.strikeLightningEffect(loc);
        }

        try {
            Particle particle = Particle.valueOf(d.getParticle().toUpperCase(Locale.ROOT));
            world.spawnParticle(
                    particle,
                    loc,
                    d.getParticleCount(),
                    1.2, 1.5, 1.2,
                    0.12
            );
        } catch (IllegalArgumentException ignored) {
            world.spawnParticle(Particle.EXPLOSION, loc, d.getParticleCount(), 1.0, 1.0, 1.0, 0.1);
        }

        try {
            Sound sound = Sound.valueOf(d.getSpawnSound().toUpperCase(Locale.ROOT));
            world.playSound(loc, sound, d.getSpawnVolume(), d.getSpawnPitch());
        } catch (IllegalArgumentException ignored) {
            world.playSound(loc, Sound.ENTITY_WITHER_SPAWN, d.getSpawnVolume(), d.getSpawnPitch());
        }
    }

    private double progress(DungeonData d) {
        LivingEntity boss = manager.activeBoss(d);

        if (boss != null && !boss.isDead()) {
            if (d.getProgressMode().equalsIgnoreCase("HEALTH")) {
                double max = Math.max(1.0, boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                        ? boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()
                        : d.getMaxHealth());
                return clamp(boss.getHealth() / max);
            }
            if (d.getProgressMode().equalsIgnoreCase("TIMER")) {
                long end = d.getNextSpawnAt() - d.getSpawnIntervalMillis() + d.getAliveTimeMillis();
                return clamp((end - System.currentTimeMillis()) / (double) d.getAliveTimeMillis());
            }
            return 1.0;
        }

        if (d.getProgressMode().equalsIgnoreCase("TIMER")) {
            return clamp((d.getNextSpawnAt() - System.currentTimeMillis()) /
                    (double) Math.max(1000L, d.getSpawnIntervalMillis()));
        }
        return 1.0;
    }

    private String renderTitle(DungeonData d) {
        LivingEntity boss = manager.activeBoss(d);
        boolean active = boss != null && !boss.isDead();

        long now = System.currentTimeMillis();
        long remaining;
        String status;

        if (active) {
            long end = d.getNextSpawnAt() - d.getSpawnIntervalMillis() + d.getAliveTimeMillis();
            remaining = Math.max(0L, end - now);
            status = "LIVE";
        } else {
            remaining = Math.max(0L, d.getNextSpawnAt() - now);
            status = "SPAWN IN";
        }

        String result = Text.apply(d.getBossbarTitle(), d, status, remaining);

        if (active) {
            result = result.replace("%health%", Text.formatNumber(boss.getHealth()));
            result = result.replace("%max-health%", Text.formatNumber(
                    boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                            ? boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()
                            : d.getMaxHealth()));
            result = result.replace("%boss-hp%", Text.formatNumber(boss.getHealth()));
            result = result.replace("%boss-hearts%", Text.formatNumber(boss.getHealth() / 2.0));
        } else {
            result = result.replace("%health%", "0");
            result = result.replace("%max-health%", Text.formatNumber(d.getMaxHealth()));
            result = result.replace("%boss-hp%", "0");
            result = result.replace("%boss-hearts%", "0");
        }

        return result;
    }

    private void applyFlags(BossBar bar, DungeonData d) {
        setFlag(bar, BarFlag.DARKEN_SKY, d.isDarkenSky());
        setFlag(bar, BarFlag.PLAY_BOSS_MUSIC, d.isPlayBossMusic());
        setFlag(bar, BarFlag.CREATE_FOG, d.isCreateFog());
    }

    private void setFlag(BossBar bar, BarFlag flag, boolean enabled) {
        if (enabled && !bar.hasFlag(flag)) bar.addFlag(flag);
        if (!enabled && bar.hasFlag(flag)) bar.removeFlag(flag);
    }

    private void hide(DungeonData d) {
        BossBar bar = bars.get(d.getName());
        if (bar != null) bar.setVisible(false);
    }

    public void removeDungeon(String name) {
        BossBar bar = bars.remove(name);
        if (bar != null) {
            bar.removeAll();
            bar.setVisible(false);
        }
        Bukkit.removeBossBar(new NamespacedKey(plugin, "bar_" + safeKey(name)));
    }

    public void shutdown() {
        for (BossBar b : bars.values()) {
            b.removeAll();
            b.setVisible(false);
        }
        bars.clear();
    }

    private String safeKey(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
    }

    private BarColor parseColor(String raw) {
        try { return BarColor.valueOf(raw.toUpperCase(Locale.ROOT)); }
        catch (Exception ignored) { return BarColor.RED; }
    }

    private BarStyle parseStyle(String raw) {
        try { return BarStyle.valueOf(raw.toUpperCase(Locale.ROOT)); }
        catch (Exception ignored) { return BarStyle.SOLID; }
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
