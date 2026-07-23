package com.smithai.memory;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Conversation {

    private final int maxMessages;
    private final List<Message> messages = new ArrayList<>();

    public Conversation(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    public void addMessage(String role, String content) {
        messages.add(new Message(role, content, System.currentTimeMillis()));
        while (messages.size() > maxMessages) {
            messages.remove(0);
        }
    }

    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    public void save(File file) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            yaml.set("messages." + i + ".role", m.getRole());
            yaml.set("messages." + i + ".content", m.getContent());
            yaml.set("messages." + i + ".timestamp", m.getTimestamp());
        }
        yaml.save(file);
    }

    public void load(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.contains("messages")) return;
        messages.clear();
        org.bukkit.configuration.ConfigurationSection section = yaml.getConfigurationSection("messages");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String role = yaml.getString("messages." + key + ".role", "user");
            String content = yaml.getString("messages." + key + ".content", "");
            long timestamp = yaml.getLong("messages." + key + ".timestamp", 0L);
            messages.add(new Message(role, content, timestamp));
        }
    }

    public static class Message {
        private final String role;
        private final String content;
        private final long timestamp;

        public Message(String role, String content, long timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }

        public String getRole() { return role; }
        public String getContent() { return content; }
        public long getTimestamp() { return timestamp; }
    }
}
