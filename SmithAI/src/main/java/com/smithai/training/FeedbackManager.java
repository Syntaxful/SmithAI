package com.smithai.training;

import com.smithai.SmithAIPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Stores specific feedback about what the AI did wrong.
 * Unlike the numeric TrainingManager, this keeps the exact message so the AI can learn from it.
 */
public class FeedbackManager {

    private final SmithAIPlugin plugin;
    private final File file;
    private final List<FeedbackEntry> entries = new ArrayList<>();

    public FeedbackManager(SmithAIPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "feedback.yml");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.contains("feedback")) return;
        for (String key : yaml.getConfigurationSection("feedback").getKeys(false)) {
            String player = yaml.getString("feedback." + key + ".player", "unknown");
            String message = yaml.getString("feedback." + key + ".message", "");
            String context = yaml.getString("feedback." + key + ".context", "");
            long time = yaml.getLong("feedback." + key + ".time", 0L);
            if (!message.isEmpty()) {
                entries.add(new FeedbackEntry(player, message, context, time));
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (int i = 0; i < entries.size(); i++) {
            FeedbackEntry e = entries.get(i);
            yaml.set("feedback." + i + ".player", e.getPlayer());
            yaml.set("feedback." + i + ".message", e.getMessage());
            yaml.set("feedback." + i + ".context", e.getContext());
            yaml.set("feedback." + i + ".time", e.getTime());
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to save feedback data", ex);
        }
    }

    public void recordFeedback(String player, String message, String context) {
        entries.add(new FeedbackEntry(player, message, context, System.currentTimeMillis()));
        if (entries.size() > 200) {
            entries.remove(0);
        }
        save();
    }

    public List<String> findRecent(String contextHint, int max) {
        List<String> result = new ArrayList<>();
        String lower = contextHint.toLowerCase();
        for (int i = entries.size() - 1; i >= 0 && result.size() < max; i--) {
            FeedbackEntry e = entries.get(i);
            if (e.getMessage().toLowerCase().contains(lower) || e.getContext().toLowerCase().contains(lower)) {
                result.add(e.getPlayer() + " said: " + e.getMessage());
            }
        }
        return result;
    }

    public List<FeedbackEntry> getRecent(int count) {
        int start = Math.max(0, entries.size() - count);
        return new ArrayList<>(entries.subList(start, entries.size()));
    }

    public int count() {
        return entries.size();
    }

    public static class FeedbackEntry {
        private final String player;
        private final String message;
        private final String context;
        private final long time;

        public FeedbackEntry(String player, String message, String context, long time) {
            this.player = player;
            this.message = message;
            this.context = context;
            this.time = time;
        }

        public String getPlayer() { return player; }
        public String getMessage() { return message; }
        public String getContext() { return context; }
        public long getTime() { return time; }
    }
}
