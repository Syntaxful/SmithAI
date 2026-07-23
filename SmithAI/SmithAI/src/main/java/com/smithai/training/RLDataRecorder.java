package com.smithai.training;

import com.smithai.SmithAIPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
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

    /**
     * Read back recent events from the CSV.
     */
    public List<RLEvent> getRecentEvents(int max) {
        List<RLEvent> events = new ArrayList<>();
        if (!file.exists()) return events;
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            // Skip header, take from end
            int start = Math.max(1, lines.size() - max);
            for (int i = start; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    events.add(new RLEvent(
                        parts[0],                      // ts
                        parts[1].equals("R") ? "Reward" : "Punishment", // type
                        parts[2],                      // action
                        Integer.parseInt(parts[3])     // score
                    ));
                }
            }
        } catch (IOException ignored) {}
        return events;
    }

    /**
     * Get summary: total rewards, total punishments, top actions.
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        Map<String, int[]> actionCounts = new HashMap<>(); // action -> [rewards, punishments]
        long totalR = 0, totalP = 0;

        if (!file.exists()) {
            summary.put("total_events", 0L);
            return summary;
        }
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            for (int i = 1; i < lines.size(); i++) { // skip header
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    boolean isR = parts[1].equals("R");
                    if (isR) totalR++; else totalP++;
                    actionCounts.computeIfAbsent(parts[2], k -> new int[2]);
                    if (isR) actionCounts.get(parts[2])[0]++; else actionCounts.get(parts[2])[1]++;
                }
            }
        } catch (IOException ignored) {}

        summary.put("total_events", totalR + totalP);
        summary.put("total_rewards", totalR);
        summary.put("total_punishments", totalP);

        // Top rewarded actions
        List<String> topRewarded = new ArrayList<>();
        actionCounts.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]))
            .limit(5).forEach(e -> topRewarded.add(e.getKey() + " (" + e.getValue()[0] + "x)"));
        summary.put("top_rewarded", topRewarded);

        // Top punished actions
        List<String> topPunished = new ArrayList<>();
        actionCounts.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue()[1], a.getValue()[1]))
            .limit(5).forEach(e -> topPunished.add(e.getKey() + " (" + e.getValue()[1] + "x)"));
        summary.put("top_punished", topPunished);

        return summary;
    }

    /**
     * Import events from a CSV string into the RL data file.
     */
    public int importFromCSV(String csvContent) {
        int imported = 0;
        String[] lines = csvContent.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("ts,")) continue;
            String[] parts = line.split(",");
            if (parts.length >= 4) {
                String type = parts[1].trim().equals("R") ? "R" : "P";
                String action = sanitize(parts[2].trim());
                int score;
                try { score = Integer.parseInt(parts[3].trim()); } catch (Exception e) { score = 0; }
                String outLine = System.currentTimeMillis() + "," + type + "," + action + "," + score + "\n";
                try {
                    Files.write(file.toPath(), outLine.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
                    imported++;
                } catch (IOException ignored) {}
            }
        }
        return imported;
    }

    public static class RLEvent {
        public final String timestamp;
        public final String type;
        public final String action;
        public final int score;
        public RLEvent(String ts, String type, String action, int score) {
            this.timestamp = ts;
            this.type = type;
            this.action = action;
            this.score = score;
        }
    }
}
