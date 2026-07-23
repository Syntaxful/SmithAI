package com.smithai.memory;

import com.smithai.SmithAIPlugin;
import com.smithai.npc.SmithNPC;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Enhanced memory features: long-term summarization, per-player preferences,
 * emotion/mood tracking, conversation threading across sessions.
 */
public class MemoryEnhancer {

    private final SmithAIPlugin plugin;
    private final Map<UUID, PlayerPreferences> preferences = new HashMap<>();
    private final Map<UUID, MoodState> moods = new HashMap<>();
    private final Map<String, List<String>> threadKeys = new HashMap<>();
    private final File dataFolder;

    public MemoryEnhancer(SmithAIPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "memory");
        if (!this.dataFolder.exists()) this.dataFolder.mkdirs();
        loadAll();
    }

    // ── LONG-TERM SUMMARIZATION ──

    /**
     * Summarize a conversation into a few key facts for long-term storage.
     * This uses simple heuristics instead of an LLM to keep it lightweight.
     */
    public String summarizeMessages(List<Conversation.Message> messages) {
        if (messages.isEmpty()) return "No conversation history.";
        StringBuilder summary = new StringBuilder();
        Set<String> topics = new HashSet<>();
        int playerCount = 0;
        int aiCount = 0;

        for (Conversation.Message msg : messages) {
            if (msg.getRole().equals("player")) {
                playerCount++;
                String content = msg.getContent().toLowerCase();
                if (content.contains("follow") || content.contains("come")) topics.add("following");
                if (content.contains("mine") || content.contains("dig")) topics.add("mining");
                if (content.contains("build") || content.contains("place")) topics.add("building");
                if (content.contains("fight") || content.contains("attack") || content.contains("kill")) topics.add("combat");
                if (content.contains("farm") || content.contains("plant") || content.contains("crop")) topics.add("farming");
                if (content.contains("craft") || content.contains("make")) topics.add("crafting");
                if (content.contains("nether") || content.contains("portal") || content.contains("dragon")) topics.add("endgame");
                if (content.contains("thank") || content.contains("good") || content.contains("love")) topics.add("positive_engagement");
                if (content.contains("bad") || content.contains("stop") || content.contains("no")) topics.add("negative_engagement");
            } else {
                aiCount++;
            }
        }

        summary.append("Chat summary: ").append(playerCount).append(" player messages, ").append(aiCount).append(" AI responses. ");
        if (!topics.isEmpty()) {
            summary.append("Key topics: ").append(String.join(", ", topics)).append(". ");
        }
        return summary.toString();
    }

    public String getLongTermSummary(UUID npcId) {
        File file = new File(dataFolder, npcId.toString() + "_summary.yml");
        if (!file.exists()) return "No long-term memory yet.";
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        return yaml.getString("summary", "No summary available.");
    }

    public void updateLongTermSummary(UUID npcId, String summary) {
        File file = new File(dataFolder, npcId.toString() + "_summary.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("summary", summary);
        yaml.set("updated", System.currentTimeMillis());
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save long-term summary", e);
        }
    }

    // ── PER-PLAYER PREFERENCES ──

    public PlayerPreferences getPreferences(UUID npcId, UUID playerId) {
        String key = npcId + ":" + playerId;
        return preferences.computeIfAbsent(npcId, id -> new PlayerPreferences());
    }

    public void setPreference(UUID npcId, UUID playerId, String key, String value) {
        PlayerPreferences prefs = getPreferences(npcId, playerId);
        prefs.set(key, value);
        savePreferences(npcId, playerId);
    }

    public String getPreference(UUID npcId, UUID playerId, String key, String defaultValue) {
        PlayerPreferences prefs = getPreferences(npcId, playerId);
        return prefs.get(key, defaultValue);
    }

    private void savePreferences(UUID npcId, UUID playerId) {
        File file = new File(dataFolder, npcId + "_prefs_" + playerId + ".yml");
        PlayerPreferences prefs = preferences.get(npcId);
        if (prefs == null) return;
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, String> e : prefs.getAll().entrySet()) {
            yaml.set(e.getKey(), e.getValue());
        }
        try { yaml.save(file); } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save preferences", e);
        }
    }

    // ── EMOTION/MOOD TRACKING ──

    public MoodState getMood(UUID npcId) {
        return moods.computeIfAbsent(npcId, id -> new MoodState("neutral", 50));
    }

    public void adjustMood(UUID npcId, String emotion, int delta) {
        MoodState mood = getMood(npcId);
        mood.emotion = emotion;
        mood.intensity = Math.max(0, Math.min(100, mood.intensity + delta));
        if (delta > 10) mood.emotion = "happy";
        else if (delta < -10) mood.emotion = "sad";
        else if (mood.intensity > 70) mood.emotion = "excited";
        else if (mood.intensity < 20) mood.emotion = "tired";
        saveMood(npcId);
    }

    public String getMoodDescription(UUID npcId) {
        MoodState m = getMood(npcId);
        return m.emotion + " (" + m.intensity + "/100)";
    }

    private void saveMood(UUID npcId) {
        File file = new File(dataFolder, npcId.toString() + "_mood.yml");
        MoodState m = moods.get(npcId);
        if (m == null) return;
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("emotion", m.emotion);
        yaml.set("intensity", m.intensity);
        try { yaml.save(file); } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save mood", e);
        }
    }

    // ── CONVERSATION THREADING ──

    public void startThread(UUID npcId, Player player) {
        String key = threadKey(npcId, player.getUniqueId());
        threadKeys.computeIfAbsent(key, k -> {
            List<String> t = new ArrayList<>();
            t.add("Thread started at " + System.currentTimeMillis());
            return t;
        });
    }

    public void addToThread(UUID npcId, UUID playerId, String message) {
        String key = threadKey(npcId, playerId);
        threadKeys.computeIfAbsent(key, k -> new ArrayList<>()).add(message);
    }

    public List<String> getThread(UUID npcId, UUID playerId) {
        return threadKeys.getOrDefault(threadKey(npcId, playerId), Collections.emptyList());
    }

    public void endThread(UUID npcId, UUID playerId) {
        threadKeys.remove(threadKey(npcId, playerId));
    }

    private String threadKey(UUID npcId, UUID playerId) {
        return npcId + ":" + playerId;
    }

    // ── PERSISTENCE ──

    private void loadAll() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith("_mood.yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                String name = f.getName().replace("_mood.yml", "");
                UUID id = UUID.fromString(name);
                MoodState m = new MoodState(yaml.getString("emotion", "neutral"), yaml.getInt("intensity", 50));
                moods.put(id, m);
            } catch (Exception ignored) {}
        }
    }

    public void saveAll() {
        for (UUID id : moods.keySet()) saveMood(id);
    }

    // ── INNER CLASSES ──

    public static class PlayerPreferences {
        private final Map<String, String> prefs = new HashMap<>();
        public void set(String key, String value) { prefs.put(key, value); }
        public String get(String key, String def) { return prefs.getOrDefault(key, def); }
        public Map<String, String> getAll() { return new HashMap<>(prefs); }
    }

    public static class MoodState {
        public String emotion;
        public int intensity;
        public MoodState(String emotion, int intensity) {
            this.emotion = emotion;
            this.intensity = intensity;
        }
    }
}
