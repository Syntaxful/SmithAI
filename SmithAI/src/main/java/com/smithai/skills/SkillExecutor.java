package com.smithai.skills;

import com.smithai.SmithAIPlugin;
import com.smithai.npc.SmithNPC;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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
                if (current != null) {
                    clearActionBar(current.contextPlayer);
                }
                current = queue.poll();
                if (current != null) {
                    current.start();
                }
            }
            if (current != null) {
                current.tick(plugin, dispatcher);
                updateActionBar(current);
            }
        }, delay, delay).getTaskId();
    }

    private void updateActionBar(SkillTask task) {
        if (task.contextPlayer == null || !task.contextPlayer.isOnline()) return;
        String name = task.skillName.replace("_", " ");
        int remaining = queue.size();
        StringBuilder msg = new StringBuilder("§e").append(task.npc.getName()).append(" §f» §7").append(name);
        // For counted tasks (e.g. gather N of X), show live progress so the queue matches reality.
        if (task.requiredCount > 0) {
            msg.append(" §f").append(task.progress).append("/").append(task.requiredCount);
        }
        if (remaining > 0) {
            msg.append(" §7(§f").append(remaining).append("§7 queued)");
        }
        task.contextPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg.toString()));
    }

    private void clearActionBar(Player player) {
        if (player != null && player.isOnline()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));
        }
    }

    public void stop() {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
        }
        cancelAll();
    }

    public void queue(SmithNPC npc, String skillName, Map<String, Object> parameters, Player contextPlayer) {
        if (queue.size() >= plugin.getPluginConfig().getMaxSkillQueueSize()) {
            if (contextPlayer != null) {
                npc.sendMessage(contextPlayer, "Skill queue is full.");
            }
            return;
        }
        queue.add(new SkillTask(npc, skillName, parameters, contextPlayer));
    }

    public void queuePlan(SmithNPC npc, List<String> plan, Map<String, Integer> counts, Player contextPlayer) {
        Map<String, Integer> safeCounts = counts != null ? counts : Collections.emptyMap();
        for (String skill : plan) {
            int count = safeCounts.getOrDefault(skill, 0);
            Map<String, Object> params = new HashMap<>();
            if (count > 0) params.put("count", count);
            queue.add(new SkillTask(npc, skill, params, contextPlayer));
        }
    }

    public void queuePlan(SmithNPC npc, List<String> plan, Player contextPlayer) {
        queuePlan(npc, plan, Collections.emptyMap(), contextPlayer);
    }

    public void cancelAll() {
        queue.clear();
        if (current != null) {
            current.cancel();
        }
        current = null;
    }

    public boolean isBusy() {
        return current != null || !queue.isEmpty();
    }

    public List<String> getQueuedSkills() {
        List<String> result = new ArrayList<>();
        if (current != null && !current.isDone()) {
            String label = current.skillName;
            if (current.requiredCount > 0) {
                label = current.skillName + " " + current.progress + "/" + current.requiredCount;
            }
            result.add("[active] " + label);
        }
        for (SkillTask task : queue) {
            String label = task.skillName;
            if (task.requiredCount > 0) {
                label = task.skillName + " 0/" + task.requiredCount;
            }
            result.add(label);
        }
        return result;
    }

    public Map<String, Integer> getQueueStats() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("current_progress", current != null ? current.progress : 0);
        stats.put("current_required", current != null ? current.requiredCount : 0);
        stats.put("queued", queue.size());
        stats.put("total", getQueueSize());
        return stats;
    }

    public int getQueueSize() {
        return queue.size() + (current != null && !current.isDone() ? 1 : 0);
    }

    public SkillTask getCurrent() {
        return current;
    }

    public Queue<SkillTask> getQueue() {
        return queue;
    }

    public static class SkillTask {
        private final SmithNPC npc;
        private final String skillName;
        private final Map<String, Object> parameters;
        private final Player contextPlayer;
        private int ticks = 0;
        private boolean done = false;
        private boolean started = false;
        /** How many of the target material this task wants to gather. 0 means "one action only". */
        private int requiredCount = 0;
        /** Live progress: how many of requiredCount have already been gathered. */
        private int progress = 0;

        public SkillTask(SmithNPC npc, String skillName, Map<String, Object> parameters, Player contextPlayer) {
            this.npc = npc;
            this.skillName = skillName;
            this.parameters = parameters != null ? parameters : Collections.emptyMap();
            this.contextPlayer = contextPlayer;
            // Support count coming from the AI action tag ([action:gather_item,diamond:64]).
            try {
                Object count = this.parameters.get("count");
                if (count instanceof Number) {
                    this.requiredCount = Math.max(1, ((Number) count).intValue());
                } else {
                    Object target = this.parameters.get("target_count");
                    if (target instanceof Number) {
                        this.requiredCount = Math.max(1, ((Number) target).intValue());
                    }
                }
            } catch (Exception ignored) {}
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

        /**
         * Per-tick step. For counted tasks (gather N of X), keep running until the dispatcher
         * has incremented {@link #progress} to {@link #requiredCount}. For non-counted tasks
         * use a duration based on the skill kind.
         */
        public void tick(SmithAIPlugin plugin, SkillDispatcher dispatcher) {
            ticks++;
            if (requiredCount > 0) {
                // Each tick, attempt another break/place/craft until the desired count is met.
                if (progress < requiredCount && npc != null && npc.getEntity() != null && !npc.getEntity().isDead()) {
                    dispatcher.execute(npc, skillName, parameters, contextPlayer);
                }
                if (progress >= requiredCount) {
                    done = true;
                }
                if (ticks > 20 * 60 * 5 && progress < requiredCount) { // 5 minute safety timeout
                    done = true;
                }
                return;
            }
            int duration = skillName.contains("conquer") || skillName.contains("master") || skillName.contains("build") || skillName.contains("raid") || skillName.contains("explore")
                ? 80 : 25;
            if (ticks >= duration) {
                done = true;
            }
        }

        /** Used by SkillDispatcher to bump the live progress counter when an item is gathered. */
        public void incrementProgress(int amount) {
            progress += amount;
            if (requiredCount > 0 && progress >= requiredCount) {
                done = true;
            }
        }

        public boolean isDone() {
            return done;
        }

        public boolean isStarted() {
            return started;
        }

        public void cancel() {
            done = true;
        }

        public int getProgress() { return progress; }
        public int getRequiredCount() { return requiredCount; }
        public String getSkillName() { return skillName; }
        public Player getContextPlayer() { return contextPlayer; }
        public SmithNPC getNpc() { return npc; }
        public Map<String, Object> getParameters() { return parameters; }
    }
}
