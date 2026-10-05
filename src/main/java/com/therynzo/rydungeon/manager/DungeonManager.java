package com.therynzo.rydungeon.manager;

import com.therynzo.rydungeon.RyDungeonPlugin;
import com.therynzo.rydungeon.model.DungeonData;
import com.therynzo.rydungeon.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class DungeonManager {
    private final RyDungeonPlugin plugin;
    private final Map<String, DungeonData> dungeons = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, LivingEntity> activeBosses = new HashMap<>();

    private final NamespacedKey dungeonKey;
    private final NamespacedKey bossKey;

    private net.milkbowl.vault.economy.Economy economy;

    public DungeonManager(RyDungeonPlugin plugin) {
        this.plugin = plugin;
        dungeonKey = new NamespacedKey(plugin, "dungeon");
        bossKey = new NamespacedKey(plugin, "boss");
    }

    public void setupEconomy() {
        economy = null;
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return;
        var registration = Bukkit.getServicesManager()
                .getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (registration != null) economy = registration.getProvider();
    }

    public net.milkbowl.vault.economy.Economy getEconomy() {
        return economy;
    }

    public Collection<DungeonData> all() {
        return Collections.unmodifiableCollection(dungeons.values());
    }

    public DungeonData get(String name) {
        return dungeons.get(name);
    }

    public boolean create(String name, Player creator) {
        if (get(name) != null) return false;
        if (dungeons.size() >= plugin.getConfig().getInt("settings.max-dungeons", 50)) return false;

        DungeonData d = DungeonData.defaults(plugin, name);
        if (creator != null) d.setSpawnPoint(creator.getLocation());
        dungeons.put(name, d);
        save(d);
        return true;
    }

    public boolean delete(String name) {
        DungeonData d = get(name);
        if (d == null) return false;

        LivingEntity boss = activeBosses.remove(name);
        if (boss != null && !boss.isDead()) boss.remove();

        dungeons.remove(name);
        File file = fileOf(name);
        return !file.exists() || file.delete();
    }

    public void loadAll() {
        dungeons.clear();
        activeBosses.clear();

        File dir = dataFolder();
        File[] files = dir.listFiles((f, n) -> n.endsWith(plugin.getConfig().getString("storage.file-extension", ".yml")));
        if (files == null) return;

        for (File file : files) {
            String ext = plugin.getConfig().getString("storage.file-extension", ".yml");
            String name = file.getName().substring(0, file.getName().length() - ext.length());
            if (!validName(name)) continue;

            DungeonData d = DungeonData.defaults(plugin, name);
            d.load(file);
            dungeons.put(name, d);
        }
    }

    public void reloadAll() {
        // Reload configuration while preserving already-spawned entities.
        Map<String, LivingEntity> previousBosses = new HashMap<>(activeBosses);
        dungeons.clear();

        File dir = dataFolder();
        File[] files = dir.listFiles((f, n) -> n.endsWith(plugin.getConfig().getString("storage.file-extension", ".yml")));
        if (files != null) {
            String ext = plugin.getConfig().getString("storage.file-extension", ".yml");
            for (File file : files) {
                String name = file.getName().substring(0, file.getName().length() - ext.length());
                if (!validName(name)) continue;
                DungeonData d = DungeonData.defaults(plugin, name);
                d.load(file);
                dungeons.put(name, d);
            }
        }

        activeBosses.clear();
        for (Map.Entry<String, LivingEntity> e : previousBosses.entrySet()) {
            if (get(e.getKey()) != null && e.getValue() != null && !e.getValue().isDead()) {
                activeBosses.put(e.getKey(), e.getValue());
            }
        }
    }

    public void saveAll() {
        for (DungeonData d : dungeons.values()) save(d);
    }

    public void save(DungeonData d) {
        try {
            d.save(fileOf(d.getName()));
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save dungeon " + d.getName() + ": " + ex.getMessage());
        }
    }

    public void tick() {
        long now = System.currentTimeMillis();

        for (DungeonData d : dungeons.values()) {
            LivingEntity boss = activeBosses.get(d.getName());

            if (boss != null) {
                if (boss.isDead() || !boss.isValid()) {
                    activeBosses.remove(d.getName());
                    d.setNextSpawnAt(now + d.getSpawnIntervalMillis());
                    save(d);
                    continue;
                }

                if (now >= d.getNextSpawnAt() - d.getSpawnIntervalMillis() + d.getAliveTimeMillis()) {
                    expireBoss(d, boss);
                }
                continue;
            }

            if (d.getSpawnPoint() != null && now >= d.getNextSpawnAt()) {
                spawnBoss(d);
            }
        }
    }

    public LivingEntity activeBoss(DungeonData d) {
        LivingEntity boss = activeBosses.get(d.getName());
        if (boss == null || boss.isDead() || !boss.isValid()) return null;
        return boss;
    }

    public void spawnBoss(DungeonData d) {
        Location loc = d.getSpawnPoint();
        if (loc == null || loc.getWorld() == null) return;
        if (activeBoss(d) != null) return;

        EntityType type;
        try {
            type = EntityType.valueOf(d.getBossType().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid boss type for " + d.getName() + ": " + d.getBossType());
            Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(Text.color(plugin.msg("messages.boss-unavailable"))));
            d.setNextSpawnAt(System.currentTimeMillis() + d.getSpawnIntervalMillis());
            save(d);
            return;
        }

        if (!type.isAlive()) {
            plugin.getLogger().warning("Boss EntityType is not alive: " + type);
            return;
        }

        Entity entity = loc.getWorld().spawnEntity(loc, type);
        if (!(entity instanceof LivingEntity boss)) {
            entity.remove();
            return;
        }

        boss.setCustomName(Text.color(d.getBossName()));
        boss.setCustomNameVisible(d.isShowName());
        boss.setGlowing(d.isGlowing());
        boss.setInvulnerable(d.isInvulnerable());

        var maxHealthAttr = boss.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(Math.max(2.0, d.getMaxHealth()));
            boss.setHealth(Math.min(maxHealthAttr.getValue(), d.getMaxHealth()));
        }

        boss.getPersistentDataContainer().set(dungeonKey, PersistentDataType.STRING, d.getName());
        boss.getPersistentDataContainer().set(bossKey, PersistentDataType.BYTE, (byte) 1);

        activeBosses.put(d.getName(), boss);
        long now = System.currentTimeMillis();
        d.setNextSpawnAt(now + d.getSpawnIntervalMillis());
        save(d);

        runSpawnCommands(d, boss.getLocation());
        plugin.getBossBarManager().announceSpawn(d, boss);
        plugin.getBossBarManager().playSpawnEffects(d, boss.getLocation());
    }

    private void runSpawnCommands(DungeonData d, Location loc) {
        if (!d.isSpawnCommandsEnabled()) return;

        for (String raw : d.getSpawnCommands()) {
            if (raw == null || raw.isBlank()) continue;
            String cmd = Text.apply(raw, d, "LIVE", 0L);
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    private void expireBoss(DungeonData d, LivingEntity boss) {
        if (boss != null && !boss.isDead()) boss.remove();

        activeBosses.remove(d.getName());
        d.setNextSpawnAt(System.currentTimeMillis() + d.getSpawnIntervalMillis());
        save(d);

        String msg = plugin.msg("messages.boss-expired")
                .replace("%boss%", Text.strip(d.getBossName()))
                .replace("%name%", d.getName());
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(Text.color(msg));
    }

    public boolean isDungeonBoss(Entity entity) {
        Byte b = entity.getPersistentDataContainer().get(bossKey, PersistentDataType.BYTE);
        return b != null && b == (byte) 1;
    }

    public DungeonData dungeonForBoss(Entity entity) {
        String name = entity.getPersistentDataContainer().get(dungeonKey, PersistentDataType.STRING);
        return name == null ? null : get(name);
    }

    public void bossDied(DungeonData d, Player killer) {
        if (d == null) return;

        activeBosses.remove(d.getName());
        d.setNextSpawnAt(System.currentTimeMillis() + d.getSpawnIntervalMillis());
        save(d);

        if (killer != null && d.getRewardMoney() > 0.0) {
            if (economy == null) {
                killer.sendMessage(Text.color(plugin.msg("messages.no-vault")));
            } else {
                economy.depositPlayer(killer, d.getRewardMoney());
                killer.sendMessage(Text.color(plugin.msg("messages.reward-given")
                        .replace("%reward%", Text.money(d.getRewardMoney()))
                        .replace("%boss%", Text.strip(d.getBossName()))));
            }
        }

        String killed = plugin.msg("messages.boss-killed")
                .replace("%boss%", Text.strip(d.getBossName()))
                .replace("%name%", d.getName());
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(Text.color(killed));
    }

    private File dataFolder() {
        File dir = new File(plugin.getDataFolder(), plugin.getConfig().getString("storage.folder", "dungeons"));
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private File fileOf(String name) {
        return new File(dataFolder(), name + plugin.getConfig().getString("storage.file-extension", ".yml"));
    }

    public static boolean validName(String name) {
        return name != null && name.matches("[A-Za-z0-9_-]{1,32}");
    }
}
