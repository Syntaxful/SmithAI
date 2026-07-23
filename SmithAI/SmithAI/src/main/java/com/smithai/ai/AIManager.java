package com.smithai.ai;

import com.smithai.SmithAIPlugin;
import com.smithai.config.Config;
import com.smithai.knowledge.KnowledgeBase;
import com.smithai.memory.Conversation;
import com.smithai.skills.SkillRegistry;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AIManager {

    private final SmithAIPlugin plugin;
    private final ExternalAIConnector externalAI;
    private final LocalMiniAI localAI;
    private final KnowledgeBase knowledgeBase;
    private final SkillRegistry skillRegistry;
    private boolean usingExternal = false;
    private boolean externalAvailable = false;
    private int reconnectTaskId = -1;

    public AIManager(SmithAIPlugin plugin) {
        this.plugin = plugin;
        this.externalAI = new ExternalAIConnector(plugin);
        this.localAI = new LocalMiniAI(plugin);
        this.knowledgeBase = new KnowledgeBase(plugin);
        this.skillRegistry = plugin.getSkillRegistry();
        reload();
    }

    public void reload() {
        Config config = plugin.getPluginConfig();
        if (reconnectTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(reconnectTaskId);
        }

        usingExternal = false;
        externalAvailable = false;

        if (config.isExternalEnabled()) {
            checkExternalAvailability();
            startReconnectTask();
        }
    }

    public void shutdown() {
        if (reconnectTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(reconnectTaskId);
        }
    }

    private void checkExternalAvailability() {
        Config config = plugin.getPluginConfig();
        if (!config.isExternalEnabled()) {
            return;
        }

        externalAI.checkHealth().thenAccept(available -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (available && !externalAvailable) {
                    externalAvailable = true;
                    usingExternal = true;
                    String url = config.getExternalUrl();
                    plugin.getLogger().info("Connected to SmithAI server at " + url);
                    if (config.isStatusMessage()) {
                        plugin.getServer().broadcastMessage("§a[SmithAI] Connected to " + config.getExternalModel() + " at " + url + ".");
                    }
                } else if (!available && externalAvailable) {
                    externalAvailable = false;
                    usingExternal = false;
                    if (config.isStatusMessage()) {
                        plugin.getServer().broadcastMessage("§e[SmithAI] SmithAI hosting is offline. Switched to Smith-Mini 1.0.");
                    }
                    plugin.getLogger().warning("External AI is unreachable. Switched to Smith-Mini 1.0.");
                }
            });
        });
    }

    private void startReconnectTask() {
        int interval = plugin.getPluginConfig().getReconnectInterval() * 20;
        reconnectTaskId = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            checkExternalAvailability();
        }, interval, interval).getTaskId();
    }

    public CompletableFuture<String> getResponse(Player player, String message, Conversation conversation, String task) {
        Config config = plugin.getPluginConfig();
        List<String> knowledge = knowledgeBase.findRelevant(message);
        List<String> skills = getAvailableSkills();
        if (config.isExternalEnabled() && usingExternal && externalAvailable) {
            return externalAI.chat(player, message, conversation, task, knowledge, skills)
                .exceptionally(ex -> {
                    plugin.getLogger().warning("External AI request failed: " + ex.getMessage());
                    usingExternal = false;
                    externalAvailable = false;
                    return fallbackMessage();
                });
        }
        return CompletableFuture.completedFuture(localAI.getResponse(player, message, conversation, task, knowledge, skills));
    }

    private String fallbackMessage() {
        return "I'm thinking locally now. Smith-Mini 1.0 is active.";
    }

    public boolean isExternalConnected() {
        return plugin.getPluginConfig().isExternalEnabled() && usingExternal && externalAvailable;
    }

    public String getActiveModelName() {
        if (isExternalConnected()) {
            return plugin.getPluginConfig().getExternalModel();
        }
        return "Smith-Mini 1.0";
    }

    public String getActiveSkillTier() {
        return skillRegistry.getTierForModel(getActiveModelName());
    }

    public List<String> getAvailableSkills() {
        return skillRegistry.getSkillsForActiveModel(getActiveModelName());
    }

    public KnowledgeBase getKnowledgeBase() {
        return knowledgeBase;
    }
}
