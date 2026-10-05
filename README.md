# RyDungeon 1.1.0

**Made By TheRynzo**

A Paper 1.21.11 dungeon boss plugin with a full GUI editor, scheduled bosses, global bossbars, announcements, effects, custom boss types, health-in-hearts, kill rewards and console spawn commands.

## Requirements

- Paper 1.21.11
- Java 21
- Vault + a Vault-compatible economy plugin for money rewards

## Commands

```text
/rydungeon create <name>
/rydungeon edit <name>
/rydungeon delete <name>
/rydungeon join
/rydungeon join <name>
/rydungeon list
/rydungeon reload
```

Aliases:

```text
/rydun
```

Permissions:

```text
rydungeon.admin
rydungeon.join
```

## Create a dungeon

Example:

```text
/rydungeon create Inferno
```

A player-created dungeon immediately opens the GUI. Click **BOSS SPAWN POINT** to save your current location as the boss spawn area.

## GUI editor

The editor controls:

- Boss Spawn Point — click to save the current player location.
- Custom Boss Mob — enter any valid living Bukkit `EntityType`.
- Custom Boss Name.
- Boss Health in hearts.
- Spawn interval.
- Boss live time.
- Kill reward money.
- Bossbar enabled/disabled.
- Bossbar mode: `BOSSBAR`, `ACTIONBAR`, `HIDDEN`.
- Bossbar title.
- Bossbar color.
- Bossbar style.
- Health / timer / full progress.
- Stack order 1-7 for multiple global bars.
- Darken sky.
- Boss music.
- Boss fog.
- Global announcement.
- Announcement title.
- Announcement subtitle.
- Announcement chat message.
- Spawn particles.
- Spawn sound.
- Lightning.
- Console spawn commands.
- Save.
- Join boss area.
- Delete confirmation.

## Time settings

The GUI accepts:

```text
1s
30s
2m
1h
1d
1h30m
2m30s
```

## Boss health

The GUI asks for hearts.

Examples:

```text
100
150
500
1000
```

`150 hearts = 300 HP`.

The built-in boss entity's real Minecraft health is updated, and the `HEALTH` bossbar progress follows the real current HP as the boss is damaged.

## Global bossbar

The native Paper/Bukkit BossBar is intentionally global:

- Every online player receives it.
- World does not matter.
- A player in world A and another player in world B can see the same dungeon bar.
- Up to 7 visible BOSSBAR-mode dungeons are supported by default.

A bossbar can show:

```text
SPAWN IN 09:59
LIVE 09:32
```

and can also use placeholders such as:

```text
%boss%
%name%
%status%
%time%
%reward%
%health%
%max-health%
%boss-hp%
%boss-hearts%
%world%
%x%
%y%
%z%
%hearts%
%max-hearts%
%spawn-interval%
%alive-time%
```

Example:

```yaml
title: "&c&lBOSS &8• &f%boss% &8• &c%boss-hearts%/%max-hearts% ❤ &8• &e%time%"
```

## Boss spawn announcement

When a boss spawns and announcements are enabled:

- Every online player receives the configured title.
- Every online player receives the configured subtitle.
- Every online player receives the configured chat message.

The announcement is server-wide and does not depend on the player's current world.

## Effects

At spawn:

- Lightning effect can be enabled.
- Particle can be selected.
- Particle count is configurable.
- Spawn sound can be selected.
- Volume and pitch are configurable.

## Spawn commands

The GUI can store multiple console commands.

Enter them separated by `;`.

Example:

```text
say The Inferno Boss has arrived!;give @a 1 diamond
```

Use:

```text
clear
```

to remove the command list.

Spawn command placeholders:

```text
%name%
%boss%
%world%
%x%
%y%
%z%
%reward%
%time%
%hearts%
%max-hearts%
%spawn-interval%
%alive-time%
```

## Boss reward

When the built-in dungeon boss is killed by a player:

- The player's kill is detected from the Bukkit death event.
- The configured Vault money is deposited to the killer.
- A reward message is sent.
- The bossbar remains usable for other dungeons.

If Vault/economy is missing, the plugin reports that money rewards cannot be paid.

## Join area

```text
/rydungeon join Inferno
```

teleports the player to the configured boss spawn point.

With exactly one dungeon:

```text
/rydungeon join
```

also works.

## Bossbar positioning limitation

Paper's native BossBar API does not expose arbitrary screen X/Y coordinates or a custom pixel width. Native bars occupy the bossbar HUD area.

RyDungeon provides:

- Up to 7 global bars.
- `stack-order: 1-7` to control logical ordering.
- Color and segment style.
- `ACTIONBAR` as a lower-screen text-only mode.

A literal custom screen coordinate requires a client-side HUD/mod/packet/NMS system and is not provided by the native API.

## Per-dungeon configuration

`config.yml` contains the default boss, bossbar, announcement, effect, GUI and sound configuration.

Each created dungeon gets its own file:

```text
plugins/RyDungeon/dungeons/<name>.yml
```

That file contains the complete current boss configuration and runtime spawn timer. This means every dungeon can have completely different settings while the main config controls defaults and GUI appearance.

## Build

Windows:

```text
build.bat
```

PowerShell:

```powershell
.\build.ps1
```

Linux:

```bash
chmod +x build.sh
./build.sh
```

Manual:

```bash
mvn clean package
```

Output:

```text
target/RyDungeon.jar
```

The build scripts also copy the JAR to:

```text
dist/RyDungeon.jar
```
