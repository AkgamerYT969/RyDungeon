package com.therynzo.rydungeon.model;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DungeonData {
    private static final Pattern DURATION = Pattern.compile("(\\d+)(ms|s|m|h|d)");

    private final String name;

    private Location spawnPoint;
    private String bossType;
    private String bossName;
    private double maxHealth;
    private long spawnIntervalMillis;
    private long aliveTimeMillis;
    private double rewardMoney;
    private boolean glowing;
    private boolean invulnerable;
    private boolean showName;

    private boolean bossbarEnabled;
    private String bossbarMode;
    private String bossbarTitle;
    private String bossbarColor;
    private String bossbarStyle;
    private String progressMode;
    private int stackOrder;
    private boolean darkenSky;
    private boolean playBossMusic;
    private boolean createFog;

    private boolean announcementEnabled;
    private String announcementTitle;
    private String announcementSubtitle;
    private String announcementChat;

    private boolean effectsEnabled;
    private boolean lightning;
    private String particle;
    private int particleCount;
    private String spawnSound;
    private float spawnVolume;
    private float spawnPitch;

    private boolean spawnCommandsEnabled;
    private List<String> spawnCommands = new ArrayList<>();

    private long nextSpawnAt;

    public DungeonData(String name) {
        this.name = name;
    }

    public static DungeonData defaults(JavaPlugin plugin, String name) {
        DungeonData d = new DungeonData(name);
        var c = plugin.getConfig();

        d.bossType = c.getString("defaults.boss.type", "WITHER");
        d.bossName = c.getString("defaults.boss.name", "&c&lDungeon Boss");
        d.maxHealth = Math.max(2.0, c.getDouble("defaults.boss.max-health-hearts", 150.0) * 2.0);
        d.spawnIntervalMillis = parseDuration(c.getString("defaults.boss.spawn-interval", "30m"), 1800000L);
        d.aliveTimeMillis = parseDuration(c.getString("defaults.boss.alive-time", "10m"), 600000L);
        d.rewardMoney = Math.max(0.0, c.getDouble("defaults.boss.reward-money", 5000.0));
        d.glowing = c.getBoolean("defaults.boss.glowing", true);
        d.invulnerable = c.getBoolean("defaults.boss.invulnerable", false);
        d.showName = c.getBoolean("defaults.boss.show-name", true);

        d.bossbarEnabled = c.getBoolean("defaults.bossbar.enabled", true);
        d.bossbarMode = c.getString("defaults.bossbar.mode", "BOSSBAR");
        d.bossbarTitle = c.getString("defaults.bossbar.title", "&c&lDUNGEON &8• &f%boss% &8• &e%status% %time%");
        d.bossbarColor = c.getString("defaults.bossbar.color", "RED");
        d.bossbarStyle = c.getString("defaults.bossbar.style", "SOLID");
        d.progressMode = c.getString("defaults.bossbar.progress-mode", "HEALTH");
        d.stackOrder = clamp(c.getInt("defaults.bossbar.stack-order", 1), 1, 7);
        d.darkenSky = c.getBoolean("defaults.bossbar.darken-sky", false);
        d.playBossMusic = c.getBoolean("defaults.bossbar.play-boss-music", false);
        d.createFog = c.getBoolean("defaults.bossbar.create-fog", false);

        d.announcementEnabled = c.getBoolean("defaults.announcement.enabled", true);
        d.announcementTitle = c.getString("defaults.announcement.title", "&c&lDUNGEON BOSS SPAWNED!");
        d.announcementSubtitle = c.getString("defaults.announcement.subtitle", "&f%boss% &7has spawned in &e%name%");
        d.announcementChat = c.getString("defaults.announcement.chat", "&8[&cDUNGEON&8] &f%boss% &chas spawned in &e%name%&c!");

        d.effectsEnabled = c.getBoolean("defaults.effects.enabled", true);
        d.lightning = c.getBoolean("defaults.effects.lightning", true);
        d.particle = c.getString("defaults.effects.particle", "EXPLOSION");
        d.particleCount = Math.max(1, c.getInt("defaults.effects.particle-count", 80));
        d.spawnSound = c.getString("defaults.effects.sound", "ENTITY_WITHER_SPAWN");
        d.spawnVolume = (float) c.getDouble("defaults.effects.volume", 2.0);
        d.spawnPitch = (float) c.getDouble("defaults.effects.pitch", 1.0);

        d.spawnCommandsEnabled = c.getBoolean("defaults.spawn-commands.enabled", false);
        d.spawnCommands = new ArrayList<>(c.getStringList("defaults.spawn-commands.commands"));

        d.nextSpawnAt = System.currentTimeMillis() + d.spawnIntervalMillis;
        return d;
    }

    public void load(File file) {
        YamlConfiguration c = YamlConfiguration.loadConfiguration(file);

        spawnPoint = readLocation(c.getConfigurationSection("spawn-point"));

        bossType = c.getString("boss.type", bossType);
        bossName = c.getString("boss.name", bossName);
        maxHealth = Math.max(2.0, c.getDouble("boss.max-health", maxHealth));
        spawnIntervalMillis = parseDuration(c.getString("boss.spawn-interval", formatDuration(spawnIntervalMillis)), spawnIntervalMillis);
        aliveTimeMillis = parseDuration(c.getString("boss.alive-time", formatDuration(aliveTimeMillis)), aliveTimeMillis);
        rewardMoney = Math.max(0.0, c.getDouble("boss.reward-money", rewardMoney));
        glowing = c.getBoolean("boss.glowing", glowing);
        invulnerable = c.getBoolean("boss.invulnerable", invulnerable);
        showName = c.getBoolean("boss.show-name", showName);

        bossbarEnabled = c.getBoolean("bossbar.enabled", bossbarEnabled);
        bossbarMode = c.getString("bossbar.mode", bossbarMode);
        bossbarTitle = c.getString("bossbar.title", bossbarTitle);
        bossbarColor = c.getString("bossbar.color", bossbarColor);
        bossbarStyle = c.getString("bossbar.style", bossbarStyle);
        progressMode = c.getString("bossbar.progress-mode", progressMode);
        stackOrder = clamp(c.getInt("bossbar.stack-order", stackOrder), 1, 7);
        darkenSky = c.getBoolean("bossbar.darken-sky", darkenSky);
        playBossMusic = c.getBoolean("bossbar.play-boss-music", playBossMusic);
        createFog = c.getBoolean("bossbar.create-fog", createFog);

        announcementEnabled = c.getBoolean("announcement.enabled", announcementEnabled);
        announcementTitle = c.getString("announcement.title", announcementTitle);
        announcementSubtitle = c.getString("announcement.subtitle", announcementSubtitle);
        announcementChat = c.getString("announcement.chat", announcementChat);

        effectsEnabled = c.getBoolean("effects.enabled", effectsEnabled);
        lightning = c.getBoolean("effects.lightning", lightning);
        particle = c.getString("effects.particle", particle);
        particleCount = Math.max(1, c.getInt("effects.particle-count", particleCount));
        spawnSound = c.getString("effects.sound", spawnSound);
        spawnVolume = (float) c.getDouble("effects.volume", spawnVolume);
        spawnPitch = (float) c.getDouble("effects.pitch", spawnPitch);

        spawnCommandsEnabled = c.getBoolean("spawn-commands.enabled", spawnCommandsEnabled);
        spawnCommands = new ArrayList<>(c.getStringList("spawn-commands.commands"));

        nextSpawnAt = c.getLong("runtime.next-spawn-at", System.currentTimeMillis() + spawnIntervalMillis);
        if (nextSpawnAt <= 0) nextSpawnAt = System.currentTimeMillis() + spawnIntervalMillis;
    }

    public void save(File file) throws IOException {
        YamlConfiguration c = new YamlConfiguration();

        writeLocation(c, "spawn-point", spawnPoint);

        c.set("boss.type", bossType);
        c.set("boss.name", bossName);
        c.set("boss.max-health", maxHealth);
        c.set("boss.spawn-interval", formatDuration(spawnIntervalMillis));
        c.set("boss.alive-time", formatDuration(aliveTimeMillis));
        c.set("boss.reward-money", rewardMoney);
        c.set("boss.glowing", glowing);
        c.set("boss.invulnerable", invulnerable);
        c.set("boss.show-name", showName);

        c.set("bossbar.enabled", bossbarEnabled);
        c.set("bossbar.mode", bossbarMode);
        c.set("bossbar.title", bossbarTitle);
        c.set("bossbar.color", bossbarColor);
        c.set("bossbar.style", bossbarStyle);
        c.set("bossbar.progress-mode", progressMode);
        c.set("bossbar.stack-order", stackOrder);
        c.set("bossbar.darken-sky", darkenSky);
        c.set("bossbar.play-boss-music", playBossMusic);
        c.set("bossbar.create-fog", createFog);

        c.set("announcement.enabled", announcementEnabled);
        c.set("announcement.title", announcementTitle);
        c.set("announcement.subtitle", announcementSubtitle);
        c.set("announcement.chat", announcementChat);

        c.set("effects.enabled", effectsEnabled);
        c.set("effects.lightning", lightning);
        c.set("effects.particle", particle);
        c.set("effects.particle-count", particleCount);
        c.set("effects.sound", spawnSound);
        c.set("effects.volume", spawnVolume);
        c.set("effects.pitch", spawnPitch);

        c.set("spawn-commands.enabled", spawnCommandsEnabled);
        c.set("spawn-commands.commands", spawnCommands);

        c.set("runtime.next-spawn-at", nextSpawnAt);

        file.getParentFile().mkdirs();
        c.save(file);
    }

    private static Location readLocation(ConfigurationSection s) {
        if (s == null || !s.getBoolean("set", false)) return null;
        String worldName = s.getString("world", "");
        org.bukkit.World w = org.bukkit.Bukkit.getWorld(worldName);
        if (w == null) return null;
        return new Location(w,
                s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                (float) s.getDouble("yaw"), (float) s.getDouble("pitch"));
    }

    private static void writeLocation(YamlConfiguration c, String path, Location l) {
        if (l == null || l.getWorld() == null) {
            c.set(path + ".set", false);
            return;
        }
        c.set(path + ".set", true);
        c.set(path + ".world", l.getWorld().getName());
        c.set(path + ".x", l.getX());
        c.set(path + ".y", l.getY());
        c.set(path + ".z", l.getZ());
        c.set(path + ".yaw", l.getYaw());
        c.set(path + ".pitch", l.getPitch());
    }

    public static long parseDuration(String input, long fallback) {
        if (input == null || input.isBlank()) return fallback;
        String v = input.toLowerCase(Locale.ROOT).replace(" ", "");
        Matcher m = DURATION.matcher(v);

        long total = 0L;
        int lastEnd = 0;
        boolean matched = false;

        while (m.find()) {
            if (m.start() != lastEnd) return fallback;
            matched = true;
            long n;
            try {
                n = Long.parseLong(m.group(1));
            } catch (NumberFormatException ex) {
                return fallback;
            }

            long part;
            try {
                part = switch (m.group(2)) {
                    case "ms" -> n;
                    case "s" -> Math.multiplyExact(n, 1000L);
                    case "m" -> Math.multiplyExact(n, 60_000L);
                    case "h" -> Math.multiplyExact(n, 3_600_000L);
                    case "d" -> Math.multiplyExact(n, 86_400_000L);
                    default -> 0L;
                };
                total = Math.addExact(total, part);
            } catch (ArithmeticException ex) {
                return fallback;
            }
            lastEnd = m.end();
        }

        return matched && lastEnd == v.length() && total >= 1000L ? total : fallback;
    }

    public static String formatDuration(long millis) {
        long seconds = Math.max(1L, millis / 1000L);
        if (seconds % 86400 == 0 && seconds >= 86400) return (seconds / 86400) + "d";
        if (seconds % 3600 == 0 && seconds >= 3600) return (seconds / 3600) + "h";
        if (seconds % 60 == 0 && seconds >= 60) return (seconds / 60) + "m";
        return seconds + "s";
    }

    public String getName() { return name; }
    public Location getSpawnPoint() { return spawnPoint; }
    public void setSpawnPoint(Location v) { spawnPoint = v == null ? null : v.clone(); }

    public String getBossType() { return bossType; }
    public void setBossType(String v) { bossType = v.toUpperCase(Locale.ROOT); }
    public String getBossName() { return bossName; }
    public void setBossName(String v) { bossName = v; }
    public double getMaxHealth() { return maxHealth; }
    public double getMaxHealthHearts() { return maxHealth / 2.0; }
    public void setMaxHealthHearts(double hearts) { maxHealth = Math.max(1.0, hearts * 2.0); }

    public long getSpawnIntervalMillis() { return spawnIntervalMillis; }
    public void setSpawnIntervalMillis(long v) { spawnIntervalMillis = Math.max(1000L, v); }
    public long getAliveTimeMillis() { return aliveTimeMillis; }
    public void setAliveTimeMillis(long v) { aliveTimeMillis = Math.max(1000L, v); }
    public double getRewardMoney() { return rewardMoney; }
    public void setRewardMoney(double v) { rewardMoney = Math.max(0.0, v); }

    public boolean isGlowing() { return glowing; }
    public void setGlowing(boolean v) { glowing = v; }
    public boolean isInvulnerable() { return invulnerable; }
    public void setInvulnerable(boolean v) { invulnerable = v; }
    public boolean isShowName() { return showName; }
    public void setShowName(boolean v) { showName = v; }

    public boolean isBossbarEnabled() { return bossbarEnabled; }
    public void setBossbarEnabled(boolean v) { bossbarEnabled = v; }
    public String getBossbarMode() { return bossbarMode; }
    public void setBossbarMode(String v) { bossbarMode = v; }
    public String getBossbarTitle() { return bossbarTitle; }
    public void setBossbarTitle(String v) { bossbarTitle = v; }
    public String getBossbarColor() { return bossbarColor; }
    public void setBossbarColor(String v) { bossbarColor = v; }
    public String getBossbarStyle() { return bossbarStyle; }
    public void setBossbarStyle(String v) { bossbarStyle = v; }
    public String getProgressMode() { return progressMode; }
    public void setProgressMode(String v) { progressMode = v; }
    public int getStackOrder() { return stackOrder; }
    public void setStackOrder(int v) { stackOrder = Math.max(1, Math.min(7, v)); }
    public boolean isDarkenSky() { return darkenSky; }
    public void setDarkenSky(boolean v) { darkenSky = v; }
    public boolean isPlayBossMusic() { return playBossMusic; }
    public void setPlayBossMusic(boolean v) { playBossMusic = v; }
    public boolean isCreateFog() { return createFog; }
    public void setCreateFog(boolean v) { createFog = v; }

    public boolean isAnnouncementEnabled() { return announcementEnabled; }
    public void setAnnouncementEnabled(boolean v) { announcementEnabled = v; }
    public String getAnnouncementTitle() { return announcementTitle; }
    public void setAnnouncementTitle(String v) { announcementTitle = v; }
    public String getAnnouncementSubtitle() { return announcementSubtitle; }
    public void setAnnouncementSubtitle(String v) { announcementSubtitle = v; }
    public String getAnnouncementChat() { return announcementChat; }
    public void setAnnouncementChat(String v) { announcementChat = v; }

    public boolean isEffectsEnabled() { return effectsEnabled; }
    public void setEffectsEnabled(boolean v) { effectsEnabled = v; }
    public boolean isLightning() { return lightning; }
    public void setLightning(boolean v) { lightning = v; }
    public String getParticle() { return particle; }
    public void setParticle(String v) { particle = v; }
    public int getParticleCount() { return particleCount; }
    public void setParticleCount(int v) { particleCount = Math.max(1, v); }
    public String getSpawnSound() { return spawnSound; }
    public void setSpawnSound(String v) { spawnSound = v; }
    public float getSpawnVolume() { return spawnVolume; }
    public void setSpawnVolume(float v) { spawnVolume = v; }
    public float getSpawnPitch() { return spawnPitch; }
    public void setSpawnPitch(float v) { spawnPitch = v; }

    public boolean isSpawnCommandsEnabled() { return spawnCommandsEnabled; }
    public void setSpawnCommandsEnabled(boolean v) { spawnCommandsEnabled = v; }
    public List<String> getSpawnCommands() { return spawnCommands; }
    public void setSpawnCommands(List<String> v) { spawnCommands = new ArrayList<>(v); }

    public long getNextSpawnAt() { return nextSpawnAt; }
    public void setNextSpawnAt(long v) { nextSpawnAt = v; }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
