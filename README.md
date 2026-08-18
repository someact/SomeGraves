# SomeGraves

SomeGraves replaces vanilla item drops upon player death with secure, customizable gravestones. It provides players with recovery tools like live compass tracking, instant auto-equipping loot mechanics, craftable teleportation scrolls, and custom death holograms while giving server administrators full control over models, sounds, and permissions.

---
## Download

link
link
link

---
## Commands

| Command                            | Permission         | Description                                               |
| :--------------------------------- | :----------------- | :-------------------------------------------------------- |
| `/somegraves` or `/sg`             | `somegraves.use`   | Opens the player's active gravestones menu.               |
| `/sg help`                         | `somegraves.use`   | Displays the formatted command guide.                     |
| `/sg track [grave_id]`             | `somegraves.track` | Starts live on-screen compass tracking to the grave.      |
| `/sg untrack`                      | `somegraves.track` | Stops the active compass tracker.                         |
| `/sg admin`                        | `somegraves.admin` | Opens the server-wide grave manager and inspection GUI.   |
| `/sg config`                       | `somegraves.admin` | Opens the in-game settings panel and recipe editor.       |
| `/sg reload`                       | `somegraves.admin` | Reloads `setting.conf` and re-registers crafting recipes. |
| `/sg givescroll <player> [amount]` | `somegraves.admin` | Gives Grave Teleport Scrolls to the specified player.     |

---

## Permissions

| Permission                      | Default | Description                                                                                           |
| :------------------------------ | :------ | :---------------------------------------------------------------------------------------------------- |
| `somegraves.use`                | `true`  | Allows player to use base grave commands and view their own graves.                                   |
| `somegraves.loot.own`           | `true`  | Allows player to loot their own gravestones.                                                          |
| `somegraves.loot.others`        | `true`  | Allows player to loot other players' gravestones.                                                     |
| `somegraves.instantloot.own`    | `true`  | Allows sneak + right-click instant auto-equipping loot on own graves.                                 |
| `somegraves.instantloot.others` | `false` | Allows instant-looting other players' graves if enabled in configuration.                             |
| `somegraves.teleport`           | `true`  | Allows player to teleport to graves using Grave Scrolls.                                              |
| `somegraves.track`              | `true`  | Allows player to use the live on-screen compass tracker.                                              |
| `somegraves.bypass.protection`  | `op`    | Allows interacting with or breaking protected gravestones.                                            |
| `somegraves.bypass.world`       | `op`    | Spawns a gravestone even if the death occurs in a blacklisted world.                                  |
| `somegraves.admin`              | `op`    | Grants access to administrative commands (`/sg admin`, `/sg config`, `/sg reload`, `/sg givescroll`). |

---

## Developer API
* Direct Java access via `SomeGravesAPI` provider.
* Custom cancellable Bukkit events:
  * `GraveSpawnEvent`
  * `GraveLootEvent`
  * `GraveExpireEvent`
  * `GraveTeleportEvent`
  * `GraveTrackEvent`

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
