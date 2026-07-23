package com.smithai;

import com.smithai.ai.AIManager;
import com.smithai.chat.ChatManager;
import com.smithai.commands.SmithAICommand;
import com.smithai.commands.SmithAITabCompleter;
import com.smithai.commands.SmithAPICommand;
import com.smithai.config.Config;
import com.smithai.debug.DebugManager;
import com.smithai.health.SubsystemHealth;
import com.smithai.memory.MemoryEnhancer;
import com.smithai.memory.MemoryManager;
import com.smithai.npc.NPCManager;
import com.smithai.npc.NPCMesh;
import com.smithai.skills.SkillExecutor;
import com.smithai.skills.SkillRegistry;
import com.smithai.training.FeedbackManager;
import com.smithai.training.IssueReportManager;
import com.smithai.training.TrainingManager;
import com.smithai.util.VersionInfo;
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
    private MemoryEnhancer memoryEnhancer;
    private NPCMesh npcMesh;
    private NPCManager npcManager;
    private SkillRegistry skillRegistry;
    private SkillExecutor skillExecutor;
    private TrainingManager trainingManager;
    private FeedbackManager feedbackManager;
    private IssueReportManager issueReportManager;
    private DebugManager debugManager;
    private SubsystemHealth subsystemHealth;
    private int reminderTaskId = -1;
    private final Random random = ThreadLocalRandom.current();

    @Override
    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();

        saveDefaultConfig();
        subsystemHealth = new SubsystemHealth(this);

        try {
            this.pluginConfig = initSubsystem(SubsystemHealth.Subsystem.AI, () -> new Config(getConfig()), "Configuration loaded");
            this.memoryManager = initSubsystem(SubsystemHealth.Subsystem.MEMORY, () -> new MemoryManager(this), "Memory manager ready");
            this.memoryEnhancer = new MemoryEnhancer(this);
            this.npcMesh = new NPCMesh(this);
            this.skillRegistry = initSubsystem(SubsystemHealth.Subsystem.SKILLS, () -> new SkillRegistry(this), "Skill registry ready");
            this.skillExecutor = initSubsystem(SubsystemHealth.Subsystem.SKILLS, () -> new SkillExecutor(this), "Skill executor ready");
            this.trainingManager = initSubsystem(SubsystemHealth.Subsystem.TRAINING, () -> new TrainingManager(this), "Training manager ready");
            this.feedbackManager = initSubsystem(SubsystemHealth.Subsystem.FEEDBACK, () -> new FeedbackManager(this), "Feedback manager ready");
            this.issueReportManager = initSubsystem(SubsystemHealth.Subsystem.FEEDBACK, () -> new IssueReportManager(this), "Issue report manager ready");
            this.debugManager = new DebugManager(this);
            this.aiManager = initSubsystem(SubsystemHealth.Subsystem.AI, () -> new AIManager(this), "AI manager ready");
            this.npcManager = initSubsystem(SubsystemHealth.Subsystem.NPC, () -> new NPCManager(this), "NPC manager ready");
            this.chatManager = initSubsystem(SubsystemHealth.Subsystem.CHAT, () -> new ChatManager(this), "Chat manager ready");

            getCommand("smithai").setExecutor(new SmithAICommand(this));
            getCommand("smithai").setTabCompleter(new SmithAITabCompleter());
            getCommand("smithapi").setExecutor(new SmithAPICommand(this));
            getCommand("smithapi").setTabCompleter(new SmithAITabCompleter());
            getServer().getPluginManager().registerEvents(chatManager, this);

            startApiKeyReminder();
            sendApiKeyStartupReminder();

            VersionInfo startupVersion = new VersionInfo();
            getLogger().info("SmithAI enabled in " + (System.currentTimeMillis() - startTime) + "ms");
            getLogger().info("Server detected: " + startupVersion.getFriendlyName());
            getLogger().info("Knowledge base loaded: " + aiManager.getKnowledgeBase().size() + " entries");
            getLogger().info("Skill library loaded: " + skillRegistry.count() + " skills");
            getLogger().info("Active skill tier: " + aiManager.getActiveSkillTier() + " (" + aiManager.getAvailableSkills().size() + " skills)");
            getLogger().info("Default brain: Smith-Mini 1.0");
            if (pluginConfig.isExternalEnabled()) {
                getLogger().info("External AI configured: " + pluginConfig.getExternalUrl());
            }
            if (!subsystemHealth.isHealthy()) {
                getLogger().warning("One or more subsystems are not healthy. Check /smithai status for admins.");
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable SmithAI", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @FunctionalInterface
    private interface Initializer<T> {
        T init() throws Exception;
    }

    private <T> T initSubsystem(SubsystemHealth.Subsystem subsystem, Initializer<T> initializer, String successMessage) {
        try {
            T result = initializer.init();
            subsystemHealth.markHealthy(subsystem, successMessage);
            return result;
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Subsystem " + subsystem + " failed to initialize: " + e.getMessage(), e);
            subsystemHealth.markDegraded(subsystem, e.getMessage());
            if (subsystem == SubsystemHealth.Subsystem.AI || subsystem == SubsystemHealth.Subsystem.NPC || subsystem == SubsystemHealth.Subsystem.CHAT) {
                throw new RuntimeException("Critical subsystem failed: " + subsystem, e);
            }
            return null;
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
        if (feedbackManager != null) {
            feedbackManager.save();
        }
        if (issueReportManager != null) {
            issueReportManager.save();
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

    public MemoryEnhancer getMemoryEnhancer() {
        return memoryEnhancer;
    }

    public NPCMesh getNpcMesh() {
        return npcMesh;
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

    public FeedbackManager getFeedbackManager() {
        return feedbackManager;
    }

    public IssueReportManager getIssueReportManager() {
        return issueReportManager;
    }

    public DebugManager getDebugManager() {
        return debugManager;
    }

    public SubsystemHealth getSubsystemHealth() {
        return subsystemHealth;
    }
}
