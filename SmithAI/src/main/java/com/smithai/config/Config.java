package com.smithai.config;

import org.bukkit.configuration.file.FileConfiguration;

public class Config {

    private FileConfiguration config;

    private String aiName;
    private String aiSkin;
    private int maxMemoryMessages;
    private boolean externalEnabled;
    private String externalUrl;
    private int externalTimeout;
    private String externalApiKey;
    private String externalModel;
    private boolean localEnabled;
    private String localModelPath;
    private boolean localFallbackToRules;
    private int localThreadCount;
    private int reconnectInterval;
    private boolean statusMessage;
    private boolean reminderEnabled;
    private int reminderMinSeconds;
    private int reminderMaxSeconds;
    private int maxSkillQueueSize;
    private int skillStepDelay;
    private boolean chatFeedback;
    private boolean persistTraining;
    private double followDistance;
    private int pathfinderTimeout;

    public Config(FileConfiguration config) {
        this.config = config;
        reload(config);
    }

    public void reload(FileConfiguration config) {
        this.config = config;
        this.aiName = config.getString("ai.name", "Smith_AI");
        this.aiSkin = config.getString("ai.skin", "robot");
        this.maxMemoryMessages = config.getInt("ai.memory.maxMessages", 17);
        this.externalEnabled = config.getBoolean("ai.external.enabled", false);
        this.externalUrl = config.getString("ai.external.url", "http://localhost:8000");
        this.externalTimeout = config.getInt("ai.external.timeout", 10);
        this.externalApiKey = config.getString("ai.external.apiKey", "");
        this.externalModel = config.getString("ai.external.model", "smithgpt-1.0-7.5");
        this.localEnabled = config.getBoolean("ai.local.enabled", true);
        this.localModelPath = config.getString("ai.local.modelPath", "plugins/SmithAI/models/smith-mini-1.0.gguf");
        this.localFallbackToRules = config.getBoolean("ai.local.fallbackToRules", true);
        this.localThreadCount = config.getInt("ai.local.threadCount", 1);
        this.reconnectInterval = config.getInt("ai.reconnectInterval", 30);
        this.statusMessage = config.getBoolean("ai.statusMessage", true);
        this.reminderEnabled = config.getBoolean("ai.reminder.enabled", true);
        this.reminderMinSeconds = config.getInt("ai.reminder.minSeconds", 10);
        this.reminderMaxSeconds = config.getInt("ai.reminder.maxSeconds", 50);
        this.maxSkillQueueSize = config.getInt("skills.maxQueueSize", 50);
        this.skillStepDelay = config.getInt("skills.stepDelay", 5);
        this.chatFeedback = config.getBoolean("training.chatFeedback", true);
        this.persistTraining = config.getBoolean("training.persist", true);
        this.followDistance = config.getDouble("npc.followDistance", 3.0);
        this.pathfinderTimeout = config.getInt("npc.pathfinderTimeout", 30);
    }

    public void setExternalApiKey(String apiKey) {
        this.externalApiKey = apiKey;
        config.set("ai.external.apiKey", apiKey);
    }

    public String getAiName() { return aiName; }
    public String getAiSkin() { return aiSkin; }
    public int getMaxMemoryMessages() { return maxMemoryMessages; }
    public boolean isExternalEnabled() { return externalEnabled; }
    public String getExternalUrl() { return externalUrl; }
    public int getExternalTimeout() { return externalTimeout; }
    public String getExternalApiKey() { return externalApiKey; }
    public String getExternalModel() { return externalModel; }
    public boolean isLocalEnabled() { return localEnabled; }
    public String getLocalModelPath() { return localModelPath; }
    public boolean isLocalFallbackToRules() { return localFallbackToRules; }
    public int getLocalThreadCount() { return localThreadCount; }
    public int getReconnectInterval() { return reconnectInterval; }
    public boolean isStatusMessage() { return statusMessage; }
    public boolean isReminderEnabled() { return reminderEnabled; }
    public int getReminderMinSeconds() { return reminderMinSeconds; }
    public int getReminderMaxSeconds() { return reminderMaxSeconds; }
    public int getMaxSkillQueueSize() { return maxSkillQueueSize; }
    public int getSkillStepDelay() { return skillStepDelay; }
    public boolean isChatFeedback() { return chatFeedback; }
    public boolean isPersistTraining() { return persistTraining; }
    public double getFollowDistance() { return followDistance; }
    public int getPathfinderTimeout() { return pathfinderTimeout; }
}
