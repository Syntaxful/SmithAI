package com.smithai.health;

import com.smithai.SmithAIPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Tracks the health of major subsystems and provides graceful recovery hooks.
 */
public class SubsystemHealth {

    public enum Subsystem {
        AI, NPC, SKILLS, MEMORY, CHAT, TRAINING, FEEDBACK, EXTERNAL
    }

    public enum Status {
        HEALTHY, DEGRADED, FAILED, DISABLED
    }

    private final Map<Subsystem, Status> statuses = new HashMap<>();
    private final Map<Subsystem, String> messages = new HashMap<>();
    private final SmithAIPlugin plugin;

    public SubsystemHealth(SmithAIPlugin plugin) {
        this.plugin = plugin;
        for (Subsystem s : Subsystem.values()) {
            statuses.put(s, Status.HEALTHY);
        }
    }

    public void markHealthy(Subsystem subsystem, String message) {
        statuses.put(subsystem, Status.HEALTHY);
        messages.put(subsystem, message);
    }

    public void markDegraded(Subsystem subsystem, String message) {
        statuses.put(subsystem, Status.DEGRADED);
        messages.put(subsystem, message);
        log(Level.WARNING, "[SubsystemHealth] " + subsystem + " degraded: " + message);
    }

    public void markFailed(Subsystem subsystem, String message) {
        statuses.put(subsystem, Status.FAILED);
        messages.put(subsystem, message);
        log(Level.SEVERE, "[SubsystemHealth] " + subsystem + " failed: " + message);
    }

    private void log(Level level, String message) {
        if (plugin != null) {
            plugin.getLogger().log(level, message);
        }
    }

    public void markDisabled(Subsystem subsystem, String message) {
        statuses.put(subsystem, Status.DISABLED);
        messages.put(subsystem, message);
    }

    public Status getStatus(Subsystem subsystem) {
        return statuses.getOrDefault(subsystem, Status.HEALTHY);
    }

    public String getMessage(Subsystem subsystem) {
        return messages.getOrDefault(subsystem, "OK");
    }

    public boolean isHealthy() {
        return statuses.values().stream().allMatch(s -> s == Status.HEALTHY || s == Status.DISABLED);
    }

    public Map<Subsystem, String> getSummary() {
        Map<Subsystem, String> summary = new HashMap<>();
        for (Subsystem s : Subsystem.values()) {
            summary.put(s, statuses.get(s) + ": " + getMessage(s));
        }
        return summary;
    }
}
