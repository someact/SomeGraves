package com.someact.somegraves.config;

import com.someact.somegraves.SomeGravesPlugin;
import com.someact.somegraves.model.GraveModelType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Enterprise configuration manager for SomeGraves with full comment preservation on disk save.
 */
public class ConfigManager {

    private final SomeGravesPlugin plugin;
    private final File configFile;
    private final Map<String, Object> values = new LinkedHashMap<>();

    public ConfigManager(SomeGravesPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "setting.conf");
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!configFile.exists()) {
            saveDefaultConfig();
        }

        values.clear();

        // 1. Load defaults from JAR
        Map<String, Object> defaults = new LinkedHashMap<>();
        try (InputStream in = plugin.getResource("setting.conf")) {
            if (in != null) {
                parseStream(in, defaults);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not parse default config stream: " + e.getMessage());
        }

        // 2. Load disk values
        Map<String, Object> fileValues = new LinkedHashMap<>();
        try (InputStream in = new FileInputStream(configFile)) {
            parseStream(in, fileValues);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load setting.conf from disk: " + e.getMessage());
        }

        // 3. Merge defaults and disk overrides
        values.putAll(defaults);

        boolean hasCustomIngredients = fileValues.keySet().stream().anyMatch(k -> k.startsWith("scroll.recipe.ingredients."));
        if (hasCustomIngredients) {
            values.keySet().removeIf(k -> k.startsWith("scroll.recipe.ingredients."));
        }

        for (Map.Entry<String, Object> entry : fileValues.entrySet()) {
            if (entry.getValue() instanceof List<?> list) {
                if (!list.isEmpty()) {
                    values.put(entry.getKey(), entry.getValue());
                }
            } else {
                values.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private void saveDefaultConfig() {
        try (InputStream in = plugin.getResource("setting.conf")) {
            if (in != null) {
                Files.copy(in, configFile.toPath());
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save default setting.conf: " + e.getMessage());
        }
    }

    private void parseStream(InputStream in, Map<String, Object> targetMap) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String l;
            while ((l = reader.readLine()) != null) {
                lines.add(l);
            }
            parseLines(lines, targetMap);
        }
    }

    private void parseLines(List<String> lines, Map<String, Object> targetMap) {
        Deque<String> sectionStack = new ArrayDeque<>();

        for (int i = 0; i < lines.size(); i++) {
            String rawLine = lines.get(i).trim();
            if (rawLine.isEmpty() || rawLine.startsWith("#")) {
                continue;
            }

            String line = stripInlineComment(rawLine);
            if (line.isEmpty()) continue;

            if (line.endsWith("{")) {
                String sectionName = line.substring(0, line.length() - 1).trim();
                sectionStack.addLast(sectionName);
                continue;
            }

            if (line.equals("}")) {
                if (!sectionStack.isEmpty()) {
                    sectionStack.removeLast();
                }
                continue;
            }

            if (line.contains("=")) {
                int eqIdx = line.indexOf('=');
                String keyPart = line.substring(0, eqIdx).trim();
                String valPart = line.substring(eqIdx + 1).trim();

                String fullKey = buildKey(sectionStack, keyPart);

                if (valPart.startsWith("[")) {
                    List<String> listValues = new ArrayList<>();
                    if (valPart.endsWith("]") && valPart.length() > 1) {
                        String inner = valPart.substring(1, valPart.length() - 1).trim();
                        if (!inner.isEmpty()) {
                            for (String item : inner.split(",")) {
                                listValues.add(stripQuotes(stripInlineComment(item.trim())));
                            }
                        }
                    } else {
                        while (++i < lines.size()) {
                            String listLine = stripInlineComment(lines.get(i).trim());
                            if (listLine.startsWith("]")) {
                                break;
                            }
                            if (!listLine.isEmpty() && !listLine.startsWith("#")) {
                                String clean = listLine;
                                if (clean.endsWith(",")) {
                                    clean = clean.substring(0, clean.length() - 1).trim();
                                }
                                listValues.add(stripQuotes(clean));
                            }
                        }
                    }
                    targetMap.put(fullKey, listValues);
                } else {
                    targetMap.put(fullKey, parseScalar(valPart));
                }
            }
        }
    }

    private String stripInlineComment(String text) {
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') inQuotes = !inQuotes;
            if (c == '#' && !inQuotes) break;
            sb.append(c);
        }
        return sb.toString().trim();
    }

    private String buildKey(Deque<String> stack, String subKey) {
        if (stack.isEmpty()) return subKey;
        return String.join(".", stack) + "." + subKey;
    }

    private Object parseScalar(String val) {
        val = val.trim();
        if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
            return val.substring(1, val.length() - 1);
        }
        if (val.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (val.equalsIgnoreCase("false")) return Boolean.FALSE;
        try {
            if (val.contains(".")) return Double.parseDouble(val);
            return Long.parseLong(val);
        } catch (NumberFormatException ignored) {
            return val;
        }
    }

    private String stripQuotes(String str) {
        str = str.trim();
        if (str.startsWith("\"") && str.endsWith("\"") && str.length() >= 2) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    public synchronized void save() {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
            writer.println("# ==============================================================================");
            writer.println("#                    SOMEGRAVES ENTERPRISE CONFIGURATION");
            writer.println("# ==============================================================================");
            writer.println("# All text strings support Kyori Adventure MiniMessage formatting:");
            writer.println("# https://docs.advntr.dev/minimessage/format.html");
            writer.println("# Example: <gradient:#ff7675:#fab1a0>Text</gradient>, <gold><b>Bold Gold</b></gold>");
            writer.println("# Unicode glyphs and Resource Pack custom fonts are fully supported!");
            writer.println();

            writer.println("general {");
            writer.println("  # Silent startup in server console: if false, prints the SomeGraves banner on startup");
            writer.println("  silent-startup = " + isSilentStartup());
            writer.println();
            writer.println("  # Automatically find a solid safe block if player dies in lava, void, or mid-air");
            writer.println("  auto-safe-location = " + isAutoSafeLocation());
            writer.println();
            writer.println("  # Respect vanilla gamerule keepInventory: do not spawn grave if true");
            writer.println("  respect-keep-inventory = " + isRespectKeepInventory());
            writer.println();
            writer.println("  # Optional custom font namespace for GUI titles and text (leave empty for default)");
            writer.println("  # Example: \"minecraft:uniform\" or \"somegraves:gui\"");
            writer.println("  custom-ui-font = \"" + getCustomUiFont() + "\"");
            writer.println("}");
            writer.println();

            writer.println("worlds {");
            writer.println("  # Mode for world filtering (Configurable in setting.conf only):");
            writer.println("  # BLACKLIST - Graves will spawn in all worlds EXCEPT those listed below");
            writer.println("  # WHITELIST - Graves will ONLY spawn in the worlds listed below");
            writer.println("  mode = \"" + getWorldsMode() + "\"");
            writer.println();
            writer.println("  # List of world names to filter");
            writer.println("  list = [");
            for (String w : getWorldsList()) writer.println("    \"" + w + "\",");
            writer.println("  ]");
            writer.println("}");
            writer.println();

            writer.println("grave {");
            writer.println("  # Time in seconds before the gravestone expires. Set to 0 for infinite duration.");
            writer.println("  # Default: 1800 (30 minutes). Can also be configured via in-game /sg config.");
            writer.println("  duration-seconds = " + getGraveDurationSeconds());
            writer.println();
            writer.println("  # Action when the grave expires:");
            writer.println("  # DROP    - Drops all stored items on the ground where the grave was");
            writer.println("  # DESTROY - Deletes the items permanently");
            writer.println("  expire-action = \"" + getExpireAction() + "\"");
            writer.println();
            writer.println("  # Can other players open and loot another player's gravestone via the chest GUI?");
            writer.println("  # true  - Anyone can open and loot other players' graves");
            writer.println("  # false - Only the grave owner (or admins with somegraves.bypass.protection) can loot");
            writer.println("  allow-loot-others = " + isAllowLootOthers());
            writer.println();
            writer.println("  # Allow the grave OWNER to sneak + right-click for instant looting & auto-equipping");
            writer.println("  # This automatically puts armor back in armor slots and offhand in offhand.");
            writer.println("  sneak-instant-loot = " + isSneakInstantLoot());
            writer.println();
            writer.println("  # Can OTHER players also sneak + right-click to instant-loot another player's grave?");
            writer.println("  # false (Default) - Other players can NEVER instant-loot; they must open the chest GUI.");
            writer.println("  # true - Other players can also sneak + right-click to instant-loot if allow-loot-others is true.");
            writer.println("  allow-others-instant-loot = " + isAllowOthersInstantLoot());
            writer.println();
            writer.println("  # Percentage of lost XP to store inside the grave (0.0 to 1.0)");
            writer.println("  # 1.0 = 100% of lost XP is preserved in the grave");
            writer.println("  # 0.5 = 50% of lost XP is preserved");
            writer.println("  # 0.0 = No XP is preserved");
            writer.println("  xp-retention-rate = " + getXpRetentionRate());
            writer.println();
            writer.println("  # Protect gravestones from being destroyed by explosions, pistons, fire, lava, or water");
            writer.println("  protect-from-damage = " + isProtectFromDamage());
            writer.println("}");
            writer.println();

            writer.println("model {");
            writer.println("  # Visual appearance model for gravestones:");
            writer.println("  # PLAYER_HEAD   - Classic player head block with owner's skin (Default)");
            writer.println("  # CHEST         - Vanilla Chest block");
            writer.println("  # BARREL        - Vanilla Barrel block");
            writer.println("  # ENDER_CHEST   - Vanilla Ender Chest block");
            writer.println("  # ITEM_DISPLAY  - Modern Display Entity (ItemDisplay) supporting custom 3D models via CustomModelData");
            writer.println("  # BLOCK_DISPLAY - Modern Display Entity (BlockDisplay) with scalable block visuals");
            writer.println("  # ARMOR_STAND   - ArmorStand entity equipped with player head and armor");
            writer.println("  type = \"" + getModelType().name() + "\"");
            writer.println();
            writer.println("  # Base material when type = ITEM_DISPLAY");
            writer.println("  custom-item-material = \"" + getModelCustomItemMaterial().name() + "\"");
            writer.println();
            writer.println("  # CustomModelData integer when type = ITEM_DISPLAY (for 3D custom grave model resource packs)");
            writer.println("  custom-model-data = " + getModelCustomModelData());
            writer.println();
            writer.println("  # Scale [x, y, z] for ITEM_DISPLAY or BLOCK_DISPLAY entities");
            writer.println("  display-scale-x = " + getDisplayScaleX());
            writer.println("  display-scale-y = " + getDisplayScaleY());
            writer.println("  display-scale-z = " + getDisplayScaleZ());
            writer.println();
            writer.println("  # ArmorStand visual options (used when type = ARMOR_STAND)");
            writer.println("  armor-stand-visible = " + isArmorStandVisible());
            writer.println("  armor-stand-small = " + isArmorStandSmall());
            writer.println("  armor-stand-arms = " + isArmorStandArms());
            writer.println("}");
            writer.println();

            writer.println("display {");
            writer.println("  # Toggle floating hologram text above graves");
            writer.println("  enabled = " + isDisplayEnabled());
            writer.println();
            writer.println("  # Distance in blocks for players to see the floating hologram text (e.g. 16, 32, 48, 64 blocks)");
            writer.println("  # 16 blocks = 1 chunk, 32 blocks = 2 chunks, 48 blocks = 3 chunks, 64 blocks = 4 chunks");
            writer.println("  view-distance-blocks = " + getDisplayViewDistanceBlocks());
            writer.println();
            writer.println("  # Enable text drop shadow for maximum readability");
            writer.println("  shadowed = " + isDisplayShadowed());
            writer.println();
            writer.println("  # Height offset above the grave block for the TextDisplay hologram");
            writer.println("  height-offset = " + getDisplayHeightOffset());
            writer.println();
            writer.println("  # Billboard mode: CENTER (always faces player), VERTICAL, HORIZONTAL, FIXED");
            writer.println("  billboard = \"" + getDisplayBillboard() + "\"");
            writer.println();
            writer.println("  # Background color of the TextDisplay in ARGB hex format (00000000 for fully transparent, 66000000 for translucent dark backdrop)");
            writer.println("  background-color = \"" + getDisplayBackgroundColor() + "\"");
            writer.println();
            writer.println("  # Lines displayed on the gravestone hologram.");
            writer.println("  # Placeholders: <player_name>, <killer_name>, <killer_weapon>, <death_cause>, <death_date>, <time_left>, <items_count>, <xp_stored>, <world>, <x>, <y>, <z>");
            writer.println("  lines = [");
            for (String line : getDisplayLines()) writer.println("    \"" + line.replace("\"", "\\\"") + "\",");
            writer.println("  ]");
            writer.println("}");
            writer.println();

            writer.println("sounds {");
            writer.println("  # Master toggle to enable or disable all plugin sound effects");
            writer.println("  enabled = " + isSoundsEnabled());
            writer.println();
            writer.println("  # Granular sound customization for every in-game event:");
            for (String evt : List.of("grave-spawn", "grave-open-chest", "grave-instant-loot", "grave-expire", "teleport-success", "teleport-fail", "tracking-start", "tracking-arrived", "gui-click", "error")) {
                writer.println("  " + evt + " {");
                writer.println("    sound = \"" + getSoundEventName(evt) + "\"");
                writer.println("    volume = " + getSoundEventVolume(evt, 1.0f));
                writer.println("    pitch = " + getSoundEventPitch(evt, 1.0f));
                writer.println("    enabled = " + isSoundEventEnabled(evt));
                writer.println("  }");
            }
            writer.println("}");
            writer.println();

            writer.println("features {");
            writer.println("  # Enable the Grave Teleport Scroll feature");
            writer.println("  teleportation-enabled = " + isTeleportationEnabled());
            writer.println();
            writer.println("  # Enable the on-screen live compass tracking feature");
            writer.println("  tracking-enabled = " + isTrackingEnabled());
            writer.println("}");
            writer.println();

            writer.println("scroll {");
            writer.println("  # Is the Grave Teleport Scroll enabled?");
            writer.println("  enabled = " + isScrollEnabled());
            writer.println();
            writer.println("  # Item display name");
            writer.println("  name = \"" + getScrollName().replace("\"", "\\\"") + "\"");
            writer.println();
            writer.println("  # Item lore");
            writer.println("  lore = [");
            for (String line : getScrollLore()) writer.println("    \"" + line.replace("\"", "\\\"") + "\",");
            writer.println("  ]");
            writer.println();
            writer.println("  # Give the item an enchantment glint");
            writer.println("  glow = " + isScrollGlow());
            writer.println();
            writer.println("  # Custom Model Data (0 for none)");
            writer.println("  custom-model-data = " + getScrollCustomModelData());
            writer.println();
            writer.println("  # Base material for the scroll item");
            writer.println("  material = \"" + getScrollMaterial().name() + "\"");
            writer.println();
            writer.println("  # Crafting Recipe configuration (can also be toggled and modified via in-game /somegraves config)");
            writer.println("  recipe {");
            writer.println("    shapeless = " + isScrollRecipeShapeless());
            writer.println("    shape = [");
            for (String s : getScrollRecipeShape()) writer.println("      \"" + s + "\",");
            writer.println("    ]");
            writer.println("    ingredients {");
            for (Map.Entry<Character, Material> entry : getScrollRecipeIngredients().entrySet()) {
                writer.println("      " + entry.getKey() + " = \"" + entry.getValue().name() + "\"");
            }
            writer.println("    }");
            writer.println("    shapeless-ingredients = [");
            for (Material mat : getScrollRecipeShapelessIngredients()) {
                writer.println("      \"" + mat.name() + "\",");
            }
            writer.println("    ]");
            writer.println("  }");
            writer.println("}");
            writer.println();

            writer.println("tracking {");
            writer.println("  # How often the tracking Actionbar updates (in server ticks, 20 ticks = 1 second)");
            writer.println("  update-ticks = " + getTrackingUpdateTicks());
            writer.println();
            writer.println("  # Format for the live Actionbar compass");
            writer.println("  # Placeholders: <player_name>, <distance>, <direction_arrow>, <x>, <y>, <z>, <world>");
            writer.println("  actionbar-format = \"" + getTrackingActionbarFormat().replace("\"", "\\\"") + "\"");
            writer.println();
            writer.println("  # Distance in blocks at which the player is considered to have arrived at the grave");
            writer.println("  arrived-distance = " + getTrackingArrivedDistance());
            writer.println("}");
            writer.println();

            writer.println("messages {");
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                if (entry.getKey().startsWith("messages.")) {
                    String sub = entry.getKey().substring("messages.".length());
                    writer.println("  " + sub + " = \"" + entry.getValue().toString().replace("\"", "\\\"") + "\"");
                }
            }
            writer.println("}");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save setting.conf: " + e.getMessage());
        }
    }

    // Getters & Setters
    public boolean isSilentStartup() {
        Object val = values.get("general.silent-startup");
        if (val == null) val = values.get("general.SilentStartup");
        if (val == null) val = values.get("SilentStartup");
        if (val instanceof Boolean b) return b;
        if (val != null) return Boolean.parseBoolean(val.toString());
        return false;
    }

    public void setSilentStartup(boolean silent) {
        values.put("general.silent-startup", silent);
    }

    public boolean isAutoSafeLocation() {
        Object val = values.get("general.auto-safe-location");
        if (val instanceof Boolean b) return b;
        return true;
    }

    public boolean isRespectKeepInventory() {
        Object val = values.get("general.respect-keep-inventory");
        if (val instanceof Boolean b) return b;
        return true;
    }

    public String getCustomUiFont() {
        Object val = values.get("general.custom-ui-font");
        return val != null ? val.toString() : "";
    }

    public String getWorldsMode() {
        Object val = values.get("worlds.mode");
        return val != null ? val.toString().toUpperCase() : "BLACKLIST";
    }

    @SuppressWarnings("unchecked")
    public List<String> getWorldsList() {
        Object val = values.get("worlds.list");
        if (val instanceof List<?> l) {
            List<String> res = new ArrayList<>();
            for (Object o : l) res.add(o.toString());
            return res;
        }
        return List.of("pvp_arena", "minigame_world");
    }

    public boolean isWorldAllowed(World world) {
        if (world == null) return true;
        String name = world.getName();
        List<String> list = getWorldsList();
        boolean listed = list.contains(name);

        if (getWorldsMode().equals("WHITELIST")) {
            return listed;
        } else {
            return !listed;
        }
    }

    public long getGraveDurationSeconds() {
        Object val = values.get("grave.duration-seconds");
        if (val instanceof Number n) return n.longValue();
        if (val != null) {
            try { return Long.parseLong(val.toString().trim()); } catch (NumberFormatException ignored) {}
        }
        return 1800L;
    }

    public void setGraveDurationSeconds(long seconds) {
        values.put("grave.duration-seconds", seconds);
    }

    public String getExpireAction() {
        Object val = values.get("grave.expire-action");
        return val != null ? val.toString() : "DROP";
    }

    public boolean isAllowLootOthers() {
        Object val = values.get("grave.allow-loot-others");
        if (val instanceof Boolean b) return b;
        return true;
    }

    public void setAllowLootOthers(boolean allow) {
        values.put("grave.allow-loot-others", allow);
    }

    public boolean isSneakInstantLoot() {
        Object val = values.get("grave.sneak-instant-loot");
        if (val instanceof Boolean b) return b;
        return true;
    }

    public void setSneakInstantLoot(boolean enable) {
        values.put("grave.sneak-instant-loot", enable);
    }

    public boolean isAllowOthersInstantLoot() {
        Object val = values.get("grave.allow-others-instant-loot");
        if (val instanceof Boolean b) return b;
        return false;
    }

    public void setAllowOthersInstantLoot(boolean allow) {
        values.put("grave.allow-others-instant-loot", allow);
    }

    public double getXpRetentionRate() {
        Object val = values.get("grave.xp-retention-rate");
        if (val instanceof Number n) return n.doubleValue();
        return 1.0;
    }

    public void setXpRetentionRate(double rate) {
        values.put("grave.xp-retention-rate", Math.max(0.0, Math.min(1.0, rate)));
    }

    public boolean isProtectFromDamage() {
        Object val = values.get("grave.protect-from-damage");
        if (val instanceof Boolean b) return b;
        return true;
    }

    // Model settings
    public GraveModelType getModelType() {
        Object val = values.get("model.type");
        return GraveModelType.fromString(val != null ? val.toString() : null, GraveModelType.PLAYER_HEAD);
    }

    public void setModelType(GraveModelType type) {
        values.put("model.type", type.name());
    }

    public Material getModelCustomItemMaterial() {
        Object val = values.get("model.custom-item-material");
        Material m = val != null ? Material.matchMaterial(val.toString()) : null;
        return m != null ? m : Material.NETHERITE_SWORD;
    }

    public int getModelCustomModelData() {
        Object val = values.get("model.custom-model-data");
        if (val instanceof Number n) return n.intValue();
        return 0;
    }

    public double getDisplayScaleX() {
        Object val = values.get("model.display-scale-x");
        if (val instanceof Number n) return n.doubleValue();
        return 1.0;
    }

    public double getDisplayScaleY() {
        Object val = values.get("model.display-scale-y");
        if (val instanceof Number n) return n.doubleValue();
        return 1.0;
    }

    public double getDisplayScaleZ() {
        Object val = values.get("model.display-scale-z");
        if (val instanceof Number n) return n.doubleValue();
        return 1.0;
    }

    public boolean isArmorStandVisible() {
        Object val = values.get("model.armor-stand-visible");
        if (val instanceof Boolean b) return b;
        return false;
    }

    public boolean isArmorStandSmall() {
        Object val = values.get("model.armor-stand-small");
        if (val instanceof Boolean b) return b;
        return false;
    }

    public boolean isArmorStandArms() {
        Object val = values.get("model.armor-stand-arms");
        if (val instanceof Boolean b) return b;
        return true;
    }

    // Display
    public boolean isDisplayEnabled() {
        Object val = values.get("display.enabled");
        if (val instanceof Boolean b) return b;
        return true;
    }

    public int getDisplayViewDistanceBlocks() {
        Object val = values.get("display.view-distance-blocks");
        if (val instanceof Number n) return n.intValue();
        if (val != null) {
            try { return Integer.parseInt(val.toString().trim()); } catch (NumberFormatException ignored) {}
        }
        return 48;
    }

    public void setDisplayViewDistanceBlocks(int blocks) {
        values.put("display.view-distance-blocks", Math.max(8, blocks));
    }

    public boolean isDisplayShadowed() {
        Object val = values.get("display.shadowed");
        if (val instanceof Boolean b) return b;
        return true;
    }

    public void setDisplayShadowed(boolean shadowed) {
        values.put("display.shadowed", shadowed);
    }

    public double getDisplayHeightOffset() {
        Object val = values.get("display.height-offset");
        if (val instanceof Number n) return n.doubleValue();
        return 1.25;
    }

    public String getDisplayBillboard() {
        Object val = values.get("display.billboard");
        return val != null ? val.toString() : "CENTER";
    }

    public String getDisplayBackgroundColor() {
        Object val = values.get("display.background-color");
        return val != null ? val.toString() : "00000000";
    }

    @SuppressWarnings("unchecked")
    public List<String> getDisplayLines() {
        Object val = values.get("display.lines");
        if (val instanceof List<?> l) {
            List<String> result = new ArrayList<>();
            for (Object o : l) result.add(o.toString());
            return result;
        }
        return List.of(
                "<gradient:#ff7675:#fab1a0><bold><player_name>'s Gravestone</bold></gradient>",
                "<gray>Killed by: <red><killer_name></red> <dark_gray>(<death_cause>)</dark_gray></gray>",
                "<gray>Items: <yellow><items_count></yellow> | XP: <green><xp_stored></green></gray>",
                "<dark_gray>Expires in: <red><time_left></red></dark_gray>",
                "<dark_purple><i>Right-Click to Loot | Shift+RClick for Instant Loot</i></dark_purple>"
        );
    }

    // Sounds
    public boolean isSoundsEnabled() {
        Object val = values.get("sounds.enabled");
        if (val instanceof Boolean b) return b;
        return true;
    }

    public void setSoundsEnabled(boolean enabled) {
        values.put("sounds.enabled", enabled);
    }

    public boolean isSoundEventEnabled(String eventKey) {
        Object val = values.get("sounds." + eventKey + ".enabled");
        if (val instanceof Boolean b) return b;
        return true;
    }

    public String getSoundEventName(String eventKey) {
        Object val = values.get("sounds." + eventKey + ".sound");
        return val != null ? val.toString() : "UI_BUTTON_CLICK";
    }

    public Sound getSoundEvent(String eventKey, Sound fallback) {
        String name = getSoundEventName(eventKey);
        if (name == null || name.trim().isEmpty()) return fallback;
        name = name.trim();

        // 1. Try Sound.valueOf directly (e.g. "BLOCK_BELL_USE")
        try {
            return Sound.valueOf(name.toUpperCase(Locale.ROOT).replace('.', '_'));
        } catch (IllegalArgumentException ignored) {}

        // 2. Try NamespacedKey from Registry.SOUNDS (e.g. "block.bell.use")
        try {
            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT).replace('_', '.'));
            Sound s = Registry.SOUNDS.get(key);
            if (s != null) return s;
        } catch (Exception ignored) {}

        try {
            return Registry.SOUNDS.get(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
        } catch (Exception ignored) {}

        return fallback;
    }

    public float getSoundEventVolume(String eventKey, float defaultVal) {
        Object val = values.get("sounds." + eventKey + ".volume");
        if (val instanceof Number n) return n.floatValue();
        return defaultVal;
    }

    public float getSoundEventPitch(String eventKey, float defaultVal) {
        Object val = values.get("sounds." + eventKey + ".pitch");
        if (val instanceof Number n) return n.floatValue();
        return defaultVal;
    }

    // Features
    public boolean isTeleportationEnabled() {
        Object val = values.get("features.teleportation-enabled");
        if (val instanceof Boolean b) return b;
        return true;
    }

    public void setTeleportationEnabled(boolean enabled) {
        values.put("features.teleportation-enabled", enabled);
    }

    public boolean isTrackingEnabled() {
        Object val = values.get("features.tracking-enabled");
        if (val instanceof Boolean b) return b;
        return true;
    }

    public void setTrackingEnabled(boolean enabled) {
        values.put("features.tracking-enabled", enabled);
    }

    // Scroll
    public boolean isScrollEnabled() {
        Object val = values.get("scroll.enabled");
        if (val instanceof Boolean b) return b;
        return true;
    }

    public String getScrollName() {
        Object val = values.get("scroll.name");
        return val != null ? val.toString() : "<gradient:#9d4edd:#e0aaff><bold>Grave Teleport Scroll</bold></gradient>";
    }

    @SuppressWarnings("unchecked")
    public List<String> getScrollLore() {
        Object val = values.get("scroll.lore");
        if (val instanceof List<?> l) {
            List<String> res = new ArrayList<>();
            for (Object o : l) res.add(o.toString());
            return res;
        }
        return List.of("<gray>Right-click to view and teleport</gray>", "<gray>to your active gravestones.</gray>", "", "<dark_gray>• Consumed upon teleportation</dark_gray>");
    }

    public boolean isScrollGlow() {
        Object val = values.get("scroll.glow");
        if (val instanceof Boolean b) return b;
        return true;
    }

    public int getScrollCustomModelData() {
        Object val = values.get("scroll.custom-model-data");
        if (val instanceof Number n) return n.intValue();
        return 0;
    }

    public Material getScrollMaterial() {
        Object val = values.get("scroll.material");
        Material mat = val != null ? Material.matchMaterial(val.toString()) : null;
        return mat != null ? mat : Material.PAPER;
    }

    public boolean isScrollRecipeShapeless() {
        Object val = values.get("scroll.recipe.shapeless");
        if (val instanceof Boolean b) return b;
        return false;
    }

    public void setScrollRecipeShapeless(boolean shapeless) {
        values.put("scroll.recipe.shapeless", shapeless);
    }

    @SuppressWarnings("unchecked")
    public List<String> getScrollRecipeShape() {
        Object val = values.get("scroll.recipe.shape");
        if (val instanceof List<?> l && l.size() == 3) {
            List<String> shape = new ArrayList<>();
            for (Object o : l) shape.add(o.toString());
            return shape;
        }
        return List.of(" R ", "RSR", " R ");
    }

    public void setScrollRecipeShape(List<String> shape) {
        values.put("scroll.recipe.shape", shape);
    }

    public Map<Character, Material> getScrollRecipeIngredients() {
        Map<Character, Material> map = new HashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getKey().startsWith("scroll.recipe.ingredients.")) {
                String charKey = entry.getKey().substring("scroll.recipe.ingredients.".length());
                if (!charKey.isEmpty()) {
                    char c = charKey.charAt(0);
                    Material mat = Material.matchMaterial(entry.getValue().toString());
                    if (mat != null) map.put(c, mat);
                }
            }
        }
        if (map.isEmpty()) {
            map.put('R', Material.REDSTONE);
            map.put('S', Material.PAPER);
        }
        return map;
    }

    public void setScrollRecipeIngredients(Map<Character, Material> ingredients) {
        values.keySet().removeIf(k -> k.startsWith("scroll.recipe.ingredients."));
        for (Map.Entry<Character, Material> entry : ingredients.entrySet()) {
            values.put("scroll.recipe.ingredients." + entry.getKey(), entry.getValue().name());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Material> getScrollRecipeShapelessIngredients() {
        Object val = values.get("scroll.recipe.shapeless-ingredients");
        List<Material> materials = new ArrayList<>();
        if (val instanceof List<?> l) {
            for (Object o : l) {
                Material m = Material.matchMaterial(o.toString());
                if (m != null) materials.add(m);
            }
        }
        if (materials.isEmpty()) {
            materials.addAll(List.of(Material.REDSTONE, Material.REDSTONE, Material.REDSTONE, Material.REDSTONE, Material.PAPER));
        }
        return materials;
    }

    public void setScrollRecipeShapelessIngredients(List<Material> ingredients) {
        List<String> strList = new ArrayList<>();
        for (Material m : ingredients) if (m != null) strList.add(m.name());
        values.put("scroll.recipe.shapeless-ingredients", strList);
    }

    // Tracking
    public long getTrackingUpdateTicks() {
        Object val = values.get("tracking.update-ticks");
        if (val instanceof Number n) return n.longValue();
        return 10L;
    }

    public String getTrackingActionbarFormat() {
        Object val = values.get("tracking.actionbar-format");
        return val != null ? val.toString() : "<gold><bold>✦ Grave Tracker ✦</bold></gold> <gray>|</gray> <aqua><distance>m</aqua> <yellow><direction_arrow></yellow> <gray>(X: <x>, Y: <y>, Z: <z>)</gray>";
    }

    public double getTrackingArrivedDistance() {
        Object val = values.get("tracking.arrived-distance");
        if (val instanceof Number n) return n.doubleValue();
        return 2.5;
    }

    public String getMessage(String key, String defaultMsg) {
        Object val = values.get("messages." + key);
        return val != null ? val.toString() : defaultMsg;
    }

    public String getPrefix() {
        return getMessage("prefix", "<dark_gray>[<gradient:#9d4edd:#e0aaff><bold>SomeGraves</bold></gradient>]</dark_gray> ");
    }
}
