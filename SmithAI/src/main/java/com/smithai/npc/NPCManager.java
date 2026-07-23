package com.smithai.npc;

import com.smithai.SmithAIPlugin;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NPCManager {

    private final SmithAIPlugin plugin;
    private final Map<UUID, SmithNPC> npcs = new ConcurrentHashMap<>();
    private int tickTask = -1;

    public NPCManager(SmithAIPlugin plugin) {
        this.plugin = plugin;
        startTickTask();
    }

    private void startTickTask() {
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (SmithNPC npc : npcs.values()) {
                npc.tick(plugin.getPluginConfig().getFollowDistance());
            }
        }, 5L, 5L).getTaskId();
    }

    public SmithNPC spawn(Location location) {
        SmithNPC npc = NPCSpawner.spawn(plugin, location);
        if (npc != null) {
            npcs.put(npc.getId(), npc);
        }
        return npc;
    }

    public boolean despawn(UUID id) {
        SmithNPC npc = npcs.remove(id);
        if (npc != null) {
            npc.remove();
            return true;
        }
        return false;
    }

    public void despawnAll() {
        for (SmithNPC npc : npcs.values()) {
            npc.remove();
        }
        npcs.clear();
    }

    public SmithNPC getNPC(UUID id) {
        return npcs.get(id);
    }

    public List<SmithNPC> getNearbyNPCs(Location location, double radius) {
        List<SmithNPC> result = new ArrayList<>();
        double radiusSquared = radius * radius;
        for (SmithNPC npc : npcs.values()) {
            if (npc.getLocation() != null && npc.getLocation().getWorld().equals(location.getWorld())) {
                if (npc.getLocation().distanceSquared(location) <= radiusSquared) {
                    result.add(npc);
                }
            }
        }
        return result;
    }

    public List<SmithNPC> getAllNPCs() {
        return new ArrayList<>(npcs.values());
    }

    public void stop() {
        if (tickTask != -1) {
            plugin.getServer().getScheduler().cancelTask(tickTask);
        }
    }
}
