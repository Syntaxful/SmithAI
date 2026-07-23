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
 * Saves player issue reports to disk so admins can review them even if the GitHub link is too long.
 */
public class IssueReportManager {

    private final SmithAIPlugin plugin;
    private final File file;
    private final List<IssueReport> reports = new ArrayList<>();

    public IssueReportManager(SmithAIPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "issue_reports.yml");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.contains("reports")) return;
        for (String key : yaml.getConfigurationSection("reports").getKeys(false)) {
            String player = yaml.getString("reports." + key + ".player", "unknown");
            String title = yaml.getString("reports." + key + ".title", "");
            String body = yaml.getString("reports." + key + ".body", "");
            long time = yaml.getLong("reports." + key + ".time", 0L);
            if (!body.isEmpty()) {
                reports.add(new IssueReport(player, title, body, time));
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (int i = 0; i < reports.size(); i++) {
            IssueReport r = reports.get(i);
            yaml.set("reports." + i + ".player", r.getPlayer());
            yaml.set("reports." + i + ".title", r.getTitle());
            yaml.set("reports." + i + ".body", r.getBody());
            yaml.set("reports." + i + ".time", r.getTime());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save issue reports", e);
        }
    }

    public void recordReport(String player, String title, String body) {
        reports.add(new IssueReport(player, title, body, System.currentTimeMillis()));
        if (reports.size() > 100) {
            reports.remove(0);
        }
        save();
    }

    public List<IssueReport> getRecent(int count) {
        int start = Math.max(0, reports.size() - count);
        return new ArrayList<>(reports.subList(start, reports.size()));
    }

    public int count() {
        return reports.size();
    }

    public static class IssueReport {
        private final String player;
        private final String title;
        private final String body;
        private final long time;

        public IssueReport(String player, String title, String body, long time) {
            this.player = player;
            this.title = title;
            this.body = body;
            this.time = time;
        }

        public String getPlayer() { return player; }
        public String getTitle() { return title; }
        public String getBody() { return body; }
        public long getTime() { return time; }
    }
}
