package com.smithai.training;

import com.smithai.SmithAIPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class TrainingManager {

    private final SmithAIPlugin plugin;
    private final File file;
    private final Map<String, Integer> feedbackScores = new HashMap<>();
    private final RLDataRecorder rlRecorder;

    public TrainingManager(SmithAIPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "training.yml");
        this.rlRecorder = new RLDataRecorder(plugin);
        load();
    }

    public RLDataRecorder getRlRecorder() {
        return rlRecorder;
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            feedbackScores.put(key, yaml.getInt(key, 0));
        }
    }

    public void save() {
        if (!plugin.getPluginConfig().isPersistTraining()) return;
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Integer> entry : feedbackScores.entrySet()) {
            yaml.set(entry.getKey(), entry.getValue());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save training data", e);
        }
    }

    public void recordGood(String action) {
        String key = action.toLowerCase();
        int score = feedbackScores.merge(key, 1, Integer::sum);
        // Record to compact RL CSV
        rlRecorder.record("R", action, score);
        save();
    }

    public void recordBad(String action) {
        String key = action.toLowerCase();
        int score = feedbackScores.merge(key, -1, Integer::sum);
        // Record to compact RL CSV
        rlRecorder.record("P", action, score);
        save();
    }

    public int getScore(String action) {
        return feedbackScores.getOrDefault(action.toLowerCase(), 0);
    }

    public Map<String, Integer> getAllScores() {
        return new HashMap<>(feedbackScores);
    }
}
