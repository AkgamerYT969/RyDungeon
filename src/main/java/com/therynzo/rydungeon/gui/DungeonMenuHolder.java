package com.therynzo.rydungeon.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class DungeonMenuHolder implements InventoryHolder {
    public enum Type {
        EDITOR, DELETE_CONFIRM
    }

    private final Type type;
    private final String dungeon;

    public DungeonMenuHolder(Type type, String dungeon) {
        this.type = type;
        this.dungeon = dungeon;
    }

    public Type type() { return type; }
    public String dungeon() { return dungeon; }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
