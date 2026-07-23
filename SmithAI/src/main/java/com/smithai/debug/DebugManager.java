package com.smithai.debug;

import com.smithai.SmithAIPlugin;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Toggles debug output for specific players. When enabled, players receive extra
 * diagnostic messages about AI decisions, skill execution, and state changes.
 */
public class DebugManager {

    private final SmithAIPlugin plugin;
    private final Set<UUID> debugPlayers = new HashSet<>();
    private boolean globalDebug = false;

    public DebugManager(SmithAIPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean toggle(Player player) {
        UUID id = player.getUniqueId();
        if (debugPlayers.remove(id)) {
            player.sendMessage("§7[SmithAI Debug] Disabled for you.");
            return false;
        }
        debugPlayers.add(id);
        player.sendMessage("§7[SmithAI Debug] Enabled. You will see extra AI messages.");
        return true;
    }

    public boolean toggleGlobal(Player player) {
        globalDebug = !globalDebug;
        player.sendMessage("§7[SmithAI Debug] Global debug " + (globalDebug ? "enabled" : "disabled") + ".");
        return globalDebug;
    }

    public boolean isDebugEnabled(Player player) {
        return globalDebug || debugPlayers.contains(player.getUniqueId());
    }

    public boolean isGlobalDebug() {
        return globalDebug;
    }

    public void sendDebug(Player player, String message) {
        if (isDebugEnabled(player)) {
            player.sendMessage("§7[SmithAI Debug] §f" + message);
        }
    }

    public void broadcastDebug(String message) {
        if (globalDebug) {
            plugin.getServer().getOnlinePlayers().forEach(p -> p.sendMessage("§7[SmithAI Debug] §f" + message));
        }
        plugin.getLogger().log(Level.FINE, "[Debug] " + message);
    }

    public int countDebugPlayers() {
        return debugPlayers.size();
    }
}
