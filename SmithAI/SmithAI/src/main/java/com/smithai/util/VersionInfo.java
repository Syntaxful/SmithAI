package com.smithai.util;

import org.bukkit.Bukkit;
import java.util.List;

/**
 * Detects the Minecraft server version at runtime so SmithAI can adapt its advice
 * to what actually exists in the world (e.g. deepslate and Y=-59 diamonds only
 * appear in 1.17+, while 1.12 Eaglercraft has classic Y=11 diamonds and stone).
 */
public class VersionInfo {

    private final int major;
    private final int minor;
    private final int patch;
    private final boolean eaglercraft;
    private final String rawBukkitVersion;
    private final String rawServerVersion;

    public VersionInfo() {
        this(Bukkit.getBukkitVersion(), Bukkit.getServer().getVersion());
    }

    VersionInfo(String bukkitVersion, String serverVersion) {
        this.rawBukkitVersion = bukkitVersion != null ? bukkitVersion : "";
        this.rawServerVersion = serverVersion != null ? serverVersion : "";
        int[] parsed = parseVersion(rawBukkitVersion);
        this.major = parsed[0];
        this.minor = parsed[1];
        this.patch = parsed[2];
        this.eaglercraft = rawServerVersion.toLowerCase().contains("eagler");
    }

    public boolean isAtLeast(int major, int minor) {
        if (this.major > major) return true;
        if (this.major == major) return this.minor >= minor;
        return false;
    }

    public boolean isAtLeast(int major, int minor, int patch) {
        if (isAtLeast(major, minor + 1)) return true;
        if (this.major == major && this.minor == minor) return this.patch >= patch;
        return false;
    }

    public boolean isLegacy() {
        return !isAtLeast(1, 17);
    }

    public boolean isEaglercraft() {
        return eaglercraft;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public String getMinecraftVersion() {
        return major + "." + minor + (patch > 0 ? "." + patch : "");
    }

    public String getRawBukkitVersion() {
        return rawBukkitVersion;
    }

    public String getRawServerVersion() {
        return rawServerVersion;
    }

    public String getFriendlyName() {
        String base = getMinecraftVersion();
        if (eaglercraft) {
            return base + " Eaglercraft";
        }
        return base + " Java Edition";
    }

    public int bestDiamondY() {
        return isAtLeast(1, 18) ? -59 : 11;
    }

    public int bestIronY() {
        return isAtLeast(1, 18) ? 16 : 40;
    }

    public int bestGoldY() {
        return isAtLeast(1, 18) ? -16 : 32;
    }

    public boolean hasDeepslate() {
        return isAtLeast(1, 17);
    }

    public boolean hasNetherite() {
        return isAtLeast(1, 16);
    }

    public boolean hasAxolotl() {
        return isAtLeast(1, 17);
    }

    public boolean hasWarden() {
        return isAtLeast(1, 19);
    }

    public boolean hasTrialChambers() {
        return isAtLeast(1, 21);
    }

    // --- GRACEFUL DEGRADATION ---

    /**
     * Whether this server supports 1.13+ blocks (barrel, blast_furnace, smoker, etc.).
     * Eaglercraft 1.8.x does NOT support these.
     */
    public boolean hasModernBlocks() {
        return isAtLeast(1, 13);
    }

    /**
     * Whether this server supports shields (added in 1.9).
     */
    public boolean hasShields() {
        return isAtLeast(1, 9);
    }

    /**
     * Whether this server supports elytra (added in 1.9).
     */
    public boolean hasElytra() {
        return isAtLeast(1, 9);
    }

    /**
     * Whether this server supports dolphins/phantoms/turtles (added in 1.13).
     */
    public boolean hasModernMobs() {
        return isAtLeast(1, 13);
    }

    /**
     * Whether this server supports debug stick, crossbow, or fletching table.
     */
    public boolean hasVillagePillage() {
        return isAtLeast(1, 14);
    }

    /**
     * Returns a human-readable list of features NOT available on this server.
     */
    public String getUnsupportedFeatures() {
        List<String> missing = new java.util.ArrayList<>();
        if (!hasModernBlocks()) missing.add("1.13+ blocks (barrels, blast furnaces, smoker)");
        if (!hasShields()) missing.add("shields");
        if (!hasElytra()) missing.add("elytra");
        if (!hasNetherite()) missing.add("netherite");
        if (!hasWarden()) missing.add("warden/deep dark");
        if (!hasTrialChambers()) missing.add("trial chambers");
        if (!hasModernMobs()) missing.add("modern mobs (dolphins, turtles, phantoms)");
        if (!hasVillagePillage()) missing.add("village & pillage features (crossbows, fletching tables)");
        return missing.isEmpty() ? "none — all features available" : String.join(", ", missing);
    }

    static int[] parseVersion(String version) {
        int[] result = {1, 8, 0}; // Safe fallback for very old/unknown servers
        if (version == null || version.isEmpty()) {
            return result;
        }
        try {
            // Strip snapshot/build metadata: "1.21.1-R0.1-SNAPSHOT" -> "1.21.1"
            String[] parts = version.split("-")[0].split("\\.");
            if (parts.length >= 2) {
                result[0] = Integer.parseInt(parts[0]);
                result[1] = Integer.parseInt(parts[1]);
                if (parts.length >= 3) {
                    result[2] = Integer.parseInt(parts[2]);
                }
            }
        } catch (NumberFormatException e) {
            // Unparseable version string, keep fallback
        }
        return result;
    }
}
