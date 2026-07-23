package com.smithai.skills;

import com.smithai.SmithAIPlugin;
import com.smithai.npc.SmithNPC;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SkillExecutor {

    private final SmithAIPlugin plugin;
    private final SkillDispatcher dispatcher;
    private final Queue<SkillTask> queue = new ConcurrentLinkedQueue<>();
    private SkillTask current = null;
    private int taskId = -1;

    public SkillExecutor(SmithAIPlugin plugin) {
        this.plugin = plugin;
        this.dispatcher = new SkillDispatcher(plugin);
        start();
    }

    private void start() {
        int delay = plugin.getPluginConfig().getSkillStepDelay();
        taskId = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (current == null || current.isDone()) {
                current = queue.poll();
                if (current != null) {
                    current.start();
                }
            }
            if (current != null) {
                current.tick();
            }
        }, delay, delay).getTaskId();
    }

    public void stop() {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
        }
        queue.clear();
        current = null;
    }

    public void queue(SmithNPC npc, String skillName, Map<String, Object> parameters, Player contextPlayer) {
        if (queue.size() >= plugin.getPluginConfig().getMaxSkillQueueSize()) {
            plugin.getLogger().warning("Skill queue full; dropping task: " + skillName);
            return;
        }
        queue.add(new SkillTask(npc, skillName, parameters, contextPlayer));
    }

    public void queuePlan(SmithNPC npc, List<String> plan, Player contextPlayer) {
        for (String skill : plan) {
            queue(npc, skill, Collections.emptyMap(), contextPlayer);
        }
    }

    public void cancelAll() {
        queue.clear();
        current = null;
    }

    public boolean isBusy() {
        return current != null || !queue.isEmpty();
    }

    public static class SkillTask {
        private final SmithNPC npc;
        private final String skillName;
        private final Map<String, Object> parameters;
        private final Player contextPlayer;
        private int ticks = 0;
        private boolean done = false;
        private boolean started = false;

        public SkillTask(SmithNPC npc, String skillName, Map<String, Object> parameters, Player contextPlayer) {
            this.npc = npc;
            this.skillName = skillName;
            this.parameters = parameters != null ? parameters : Collections.emptyMap();
            this.contextPlayer = contextPlayer;
        }

        public void start() {
            started = true;
            if (npc != null && npc.getEntity() != null && !npc.getEntity().isDead()) {
                SmithAIPlugin plugin = SmithAIPlugin.getInstance();
                if (plugin != null) {
                    new SkillDispatcher(plugin).execute(npc, skillName, parameters, contextPlayer);
                }
            }
        }

        public void tick() {
            ticks++;
            // Simple skills finish quickly; composite skills stay active longer.
            int duration = skillName.contains("conquer") || skillName.contains("master") || skillName.contains("build") || skillName.contains("raid") ? 60 : 20;
            if (ticks >= duration) {
                done = true;
            }
        }

        public boolean isDone() {
            return done;
        }
    }
}
