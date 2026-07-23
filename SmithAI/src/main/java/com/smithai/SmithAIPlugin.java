package com.smithai;

import com.smithai.ai.AIManager;
import com.smithai.chat.ChatManager;
import com.smithai.commands.SmithAICommand;
import com.smithai.commands.SmithAITabCompleter;
import com.smithai.commands.SmithAPICommand;
import com.smithai.config.Config;
import com.smithai.memory.MemoryManager;
import com.smithai.npc.NPCManager;
import com.smithai.skills.SkillExecutor;
import com.smithai.skills.SkillRegistry;
import com.smithai.training.TrainingManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class SmithAIPlugin extends JavaPlugin {

    private static SmithAIPlugin instance;
    private Config pluginConfig;
    private AIManager aiManager;
    private ChatManager chatManager;
    private MemoryManager memoryManager;
    private NPCManager npcManager;
    private SkillRegistry skillRegistry;
    private SkillExecutor skillExecutor;
    private TrainingManager trainingManager;
    private int reminderTaskId = -1;
    private final Random random = ThreadLocalRandom.current();

    @Override
    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();

        saveDefaultConfig();

        try {
            this.pluginConfig = new Config(getConfig());
            this.memoryManager = new MemoryManager(this);
            this.skillRegistry = new SkillRegistry(this);
            this.skillExecutor = new SkillExecutor(this);
            this.trainingManager = new TrainingManager(this);
            this.aiManager = new AIManager(this);
            this.npcManager = new NPCManager(this);
            this.chatManager = new ChatManager(this);

            getCommand("smithai").setExecutor(new SmithAICommand(this));
            getCommand("smithai").setTabCompleter(new SmithAITabCompleter());
            getCommand("smithapi").setExecutor(new SmithAPICommand(this));
            getCommand("smithapi").setTabCompleter(new SmithAITabCompleter());
            getServer().getPluginManager().registerEvents(chatManager, this);

            startApiKeyReminder();
            sendApiKeyStartupReminder();

            getLogger().info("SmithAI enabled in " + (System.currentTimeMillis() - startTime) + "ms");
            getLogger().info("Knowledge base loaded: " + aiManager.getKnowledgeBase().size() + " entries");
            getLogger().info("Skill library loaded: " + skillRegistry.count() + " skills");
            getLogger().info("Active skill tier: " + aiManager.getActiveSkillTier() + " (" + aiManager.getAvailableSkills().size() + " skills)");
            getLogger().info("Default brain: Smith-Mini 1.0");
            if (pluginConfig.isExternalEnabled()) {
                getLogger().info("External AI configured: " + pluginConfig.getExternalUrl());
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable SmithAI", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (reminderTaskId != -1) {
            Bukkit.getScheduler().cancelTask(reminderTaskId);
        }
        if (npcManager != null) {
            npcManager.stop();
            npcManager.despawnAll();
        }
        if (skillExecutor != null) {
            skillExecutor.stop();
        }
        if (aiManager != null) {
            aiManager.shutdown();
        }
        if (memoryManager != null) {
            memoryManager.saveAll();
        }
        saveConfig();
        getLogger().info("SmithAI disabled");
    }

    public void reload() {
        reloadConfig();
        pluginConfig.reload(getConfig());
        aiManager.reload();
        if (reminderTaskId != -1) {
            Bukkit.getScheduler().cancelTask(reminderTaskId);
        }
        startApiKeyReminder();
        getLogger().info("SmithAI configuration reloaded");
    }

    private void sendApiKeyStartupReminder() {
        if (!pluginConfig.isReminderEnabled() || !pluginConfig.isExternalEnabled()) {
            return;
        }
        String key = pluginConfig.getExternalApiKey();
        if (key != null && !key.isEmpty()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("smithai.api")) {
                player.sendMessage("§e[SmithAI] §fConnect the API: §e/SmithAPI set SMA-...");
                player.sendMessage("§e[SmithAI] §eGet your key from the SmithAI-Server console.");
            }
        }
    }

    private void startApiKeyReminder() {
        if (!pluginConfig.isReminderEnabled() || !pluginConfig.isExternalEnabled()) {
            return;
        }
        scheduleReminder();
    }

    private void scheduleReminder() {
        int min = pluginConfig.getReminderMinSeconds();
        int max = pluginConfig.getReminderMaxSeconds();
        if (min > max) {
            int tmp = min;
            min = max;
            max = tmp;
        }
        int delayTicks = (min + random.nextInt(max - min + 1)) * 20;
        reminderTaskId = Bukkit.getScheduler().runTaskLater(this, () -> {
            String key = pluginConfig.getExternalApiKey();
            if (key == null || key.isEmpty()) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("smithai.api")) {
                        player.sendMessage("§e[SmithAI] §fConnect the API: §e/SmithAPI set SMA-...");
                        player.sendMessage("§e[SmithAI] §eGet your key from the SmithAI-Server console.");
                    }
                }
            }
            scheduleReminder();
        }, delayTicks).getTaskId();
    }

    public static SmithAIPlugin getInstance() {
        return instance;
    }

    public Config getPluginConfig() {
        return pluginConfig;
    }

    public AIManager getAiManager() {
        return aiManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public MemoryManager getMemoryManager() {
        return memoryManager;
    }

    public NPCManager getNpcManager() {
        return npcManager;
    }

    public SkillRegistry getSkillRegistry() {
        return skillRegistry;
    }

    public SkillExecutor getSkillExecutor() {
        return skillExecutor;
    }

    public TrainingManager getTrainingManager() {
        return trainingManager;
    }
}
