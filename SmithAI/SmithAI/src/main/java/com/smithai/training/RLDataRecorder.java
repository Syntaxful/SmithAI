package com.smithai.training;

import com.smithai.SmithAIPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;

/**
 * Ultra-compact CSV-based reward/punishment data recorder.
 * Format: ts,type,action,score\n
 * Example: 1712345678,R,get_diamonds,1\n
 * Keeps ~200 bytes/event. Append-only, no loading = no overhead.
 * Data is written to plugins/SmithAI/rl_data.csv for offline analysis/training.
 */
public class RLDataRecorder {

    private final SmithAIPlugin plugin;
    private final File file;
    private long lastWriteTime = 0;
    private static final long MIN_WRITE_INTERVAL_MS = 5000;

    public RLDataRecorder(SmithAIPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "rl_data.csv");
        initFile();
    }

    private void initFile() {
        if (!file.exists()) {
            try {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                // Write header
                Files.write(file.toPath(), "ts,type,action,score\n".getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to init rl_data.csv", e);
            }
        }
    }

    /**
     * Record a reward (+1) or punishment (-1) event.
     * @param type "R" for reward, "P" for punishment
     * @param action The action/skill name
     * @param score The cumulative score for this action
     */
    public void record(String type, String action, int score) {
        long now = System.currentTimeMillis();
        // Throttle writes to avoid IO spam
        if (now - lastWriteTime < MIN_WRITE_INTERVAL_MS) return;
        lastWriteTime = now;

        String line = now + "," + type + "," + sanitize(action) + "," + score + "\n";
        try {
            Files.write(file.toPath(), line.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.APPEND);
        } catch (IOException e) {
            plugin.getLogger().log(Level.FINE, "Failed to write RL data", e);
        }
    }

    private String sanitize(String s) {
        if (s == null || s.isEmpty()) return "unknown";
        // Strip commas, newlines, make compact
        return s.toLowerCase().replaceAll("[, \\n\\r]+", "_").replaceAll("_+", "_");
    }

    /**
     * Get total recorded lines (excluding header).
     */
    public long count() {
        if (!file.exists()) return 0;
        try {
            return Files.lines(file.toPath()).count() - 1; // subtract header
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Get file path for manual inspection.
     */
    public String getFilePath() {
        return file.getAbsolutePath();
    }

    public void exportTo(File dest) throws IOException {
        if (file.exists()) {
            Files.copy(file.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
