package com.therynzo.rydungeon.listener;

import com.therynzo.rydungeon.RyDungeonPlugin;
import com.therynzo.rydungeon.manager.DungeonManager;
import com.therynzo.rydungeon.model.DungeonData;
import com.therynzo.rydungeon.util.Text;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class DungeonPlayerListener implements Listener {
    private final RyDungeonPlugin plugin;
    private final DungeonManager manager;

    public DungeonPlayerListener(RyDungeonPlugin plugin, DungeonManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getBossBarManager().addPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getBossBarManager().removePlayer(event.getPlayer());
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!manager.isDungeonBoss(event.getEntity())) return;

        DungeonData d = manager.dungeonForBoss(event.getEntity());
        if (d == null) return;

        manager.bossDied(d, event.getEntity().getKiller());
    }
}
