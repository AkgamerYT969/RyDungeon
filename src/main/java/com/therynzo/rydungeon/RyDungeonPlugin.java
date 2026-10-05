package com.therynzo.rydungeon;

import com.therynzo.rydungeon.command.RyDungeonCommand;
import com.therynzo.rydungeon.gui.DungeonEditor;
import com.therynzo.rydungeon.listener.DungeonGuiListener;
import com.therynzo.rydungeon.listener.DungeonPlayerListener;
import com.therynzo.rydungeon.manager.BossBarManager;
import com.therynzo.rydungeon.manager.DungeonManager;
import com.therynzo.rydungeon.manager.DungeonScheduler;
import com.therynzo.rydungeon.manager.InputManager;
import com.therynzo.rydungeon.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class RyDungeonPlugin extends JavaPlugin {
    private DungeonManager dungeonManager;
    private BossBarManager bossBarManager;
    private DungeonScheduler scheduler;
    private DungeonEditor editor;
    private InputManager inputManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        dungeonManager = new DungeonManager(this);
        dungeonManager.loadAll();
        dungeonManager.setupEconomy();

        bossBarManager = new BossBarManager(this, dungeonManager);
        editor = new DungeonEditor(this, dungeonManager);
        inputManager = new InputManager(this, dungeonManager, editor);
        scheduler = new DungeonScheduler(this, dungeonManager);

        RyDungeonCommand command = new RyDungeonCommand(this, dungeonManager, editor);
        getCommand("rydungeon").setExecutor(command);
        getCommand("rydungeon").setTabCompleter(command);

        Bukkit.getPluginManager().registerEvents(
                new DungeonGuiListener(this, dungeonManager, editor), this);
        Bukkit.getPluginManager().registerEvents(
                new DungeonPlayerListener(this, dungeonManager), this);
        Bukkit.getPluginManager().registerEvents(inputManager, this);

        bossBarManager.rebuild();
        scheduler.start();

        getLogger().info("RyDungeon enabled • Made By TheRynzo");
        getLogger().info("Loaded dungeons: " + dungeonManager.all().size());
    }

    @Override
    public void onDisable() {
        if (scheduler != null) scheduler.stop();
        if (bossBarManager != null) bossBarManager.shutdown();
        if (dungeonManager != null) dungeonManager.saveAll();
    }

    public DungeonManager getDungeonManager() { return dungeonManager; }
    public BossBarManager getBossBarManager() { return bossBarManager; }
    public DungeonEditor getEditor() { return editor; }
    public InputManager getInputManager() { return inputManager; }

    public String msg(String path) {
        return getConfig().getString(path, path);
    }
}
