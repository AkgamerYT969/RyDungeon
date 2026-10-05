package com.therynzo.rydungeon.manager;

import com.therynzo.rydungeon.RyDungeonPlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public final class DungeonScheduler {
    private final RyDungeonPlugin plugin;
    private final DungeonManager manager;
    private BukkitTask dungeonTask;
    private BukkitTask bossbarTask;

    public DungeonScheduler(RyDungeonPlugin plugin, DungeonManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void start() {
        long dungeonPeriod = Math.max(20L, plugin.getConfig().getLong("settings.scheduler-ticks", 20L));
        long bossbarPeriod = Math.max(1L, plugin.getConfig().getLong("settings.bossbar-refresh-ticks", 5L));

        dungeonTask = Bukkit.getScheduler().runTaskTimer(plugin, manager::tick,
                dungeonPeriod, dungeonPeriod);

        bossbarTask = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> plugin.getBossBarManager().tick(),
                bossbarPeriod, bossbarPeriod);
    }

    public void stop() {
        if (dungeonTask != null) dungeonTask.cancel();
        if (bossbarTask != null) bossbarTask.cancel();
    }
}
