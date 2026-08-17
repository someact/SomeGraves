# SomeGraves

A modern, high-performance gravestone and death recovery plugin built specifically for PaperMC servers running Minecraft 1.20.5 through 1.21.4 and 26.2+ on Java 21 or newer.

SomeGraves replaces vanilla item drops upon player death with secure, customizable gravestones. It provides players with recovery tools like live compass tracking, instant auto-equipping loot mechanics, craftable teleportation scrolls, and custom death holograms while giving server administrators full control over models, sounds, and permissions.

---

## Features

### Visual Models and Customization
* Multiple Grave Styles: Choose between Player Head (with owner skin), Chest, Barrel, Item Display (with CustomModelData support for custom 3D resource pack models), Block Display, or Armor Stand.
* Text Display Holograms: Lightweight floating text using Minecraft's modern Display Entity system with customizable render view distance, text drop shadows, and transparent backdrops.
* Rich Death Metadata: Holograms and menus display detailed death context including killer name, killer weapon, damage cause, time remaining, stored experience, and exact coordinates.

### Player Experience
* Live Compass Tracker: Players can run `/sg track` to activate a live actionbar compass pointer guiding them directly to their death location.
* Instant Auto-Loot: Sneak and right-click a grave to instantly retrieve items and automatically re-equip armor into the correct armor slots and off-hand.
* Multi-Page Grave Overview: Use `/sg` to browse active and expired gravestones, check stored items, and view death statistics.
* Grave Teleport Scrolls: Craftable consumable scrolls that teleport players back to their active graves after a configurable warmup countdown.

### Administrative Control
* In-Game Control Panel: Manage visual models, timers, view distances, sound effects, and crafting recipes directly inside the `/sg config` GUI.
* Comment-Preserving Configuration: When saving settings in-game, all configuration headers, sections, and documentation comments in `setting.conf` are preserved.
* Interactive 3x3 Recipe Editor: Change the crafting recipe for Grave Teleport Scrolls visually inside the admin GUI with support for both shaped and shapeless modes.
* World Filtering: Configure blacklist or whitelist world filters in `setting.conf` to prevent graves from spawning in minigame or PvP arenas.
* Safe Location Finder: Automatically relocates gravestones to a solid, safe block if a player dies in the void, lava, or mid-air.

### Developer API
* Direct Java access via `SomeGravesAPI` provider.
* Custom cancellable Bukkit events:
  * `GraveSpawnEvent`
  * `GraveLootEvent`
  * `GraveExpireEvent`
  * `GraveTeleportEvent`
  * `GraveTrackEvent`

---

## Requirements

* Server Software: Paper, Purpur, or Folia (Version 1.20.5, 1.20.6, 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 26.2+)
* Java Runtime: Java 21 or higher (JDK 21+)

---

## Commands

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/somegraves` or `/sg` | `somegraves.use` | Opens the player's active gravestones menu. |
| `/sg help` | `somegraves.use` | Displays the formatted command guide. |
| `/sg track [grave_id]` | `somegraves.track` | Starts live on-screen compass tracking to the grave. |
| `/sg untrack` | `somegraves.track` | Stops the active compass tracker. |
| `/sg admin` | `somegraves.admin` | Opens the server-wide grave manager and inspection GUI. |
| `/sg config` | `somegraves.admin` | Opens the in-game settings panel and recipe editor. |
| `/sg reload` | `somegraves.admin` | Reloads `setting.conf` and re-registers crafting recipes. |
| `/sg givescroll <player> [amount]` | `somegraves.admin` | Gives Grave Teleport Scrolls to the specified player. |

---

## Permissions

| Permission | Default | Description |
| :--- | :--- | :--- |
| `somegraves.use` | `true` | Allows player to use base grave commands and view their own graves. |
| `somegraves.loot.own` | `true` | Allows player to loot their own gravestones. |
| `somegraves.loot.others` | `true` | Allows player to loot other players' gravestones. |
| `somegraves.instantloot.own` | `true` | Allows sneak + right-click instant auto-equipping loot on own graves. |
| `somegraves.instantloot.others` | `false` | Allows instant-looting other players' graves if enabled in configuration. |
| `somegraves.teleport` | `true` | Allows player to teleport to graves using Grave Scrolls. |
| `somegraves.track` | `true` | Allows player to use the live on-screen compass tracker. |
| `somegraves.bypass.protection` | `op` | Allows interacting with or breaking protected gravestones. |
| `somegraves.bypass.world` | `op` | Spawns a gravestone even if the death occurs in a blacklisted world. |
| `somegraves.admin` | `op` | Grants access to administrative commands (`/sg admin`, `/sg config`, `/sg reload`, `/sg givescroll`). |

---

## Developer API

To integrate SomeGraves into your plugin, access the public API class:

```java
import com.someact.somegraves.api.SomeGravesAPI;
import com.someact.somegraves.model.GraveData;

// Retrieve all active graves for a player
List<GraveData> graves = SomeGravesAPI.getGravesForPlayer(player.getUniqueId());

// Start tracking a grave programmatically
SomeGravesAPI.startTracking(player, graveData);

// Listen to custom events
@EventHandler
public void onGraveSpawn(GraveSpawnEvent event) {
    Player player = event.getPlayer();
    GraveData grave = event.getGraveData();
    // Custom logic here
}
```

---

## Building from Source

Clone the repository and compile using the Gradle wrapper:

```bash
git clone https://github.com/someact/SomeGraves.git
cd SomeGraves
./gradlew build
```

The compiled JAR file will be located at `build/libs/SomeGraves-1.0.0.jar`.

---

## Author

Created and maintained by **someact**.
