package com.smithai.memory;

import com.smithai.SmithAIPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Level;

public class MemoryManager {

    private final SmithAIPlugin plugin;
    private final Map<UUID, Conversation> conversations = new HashMap<>();
    private final File dataFolder;

    public MemoryManager(SmithAIPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "memory");
        if (!this.dataFolder.exists()) {
            this.dataFolder.mkdirs();
        }
    }

    public Conversation getConversation(UUID npcId) {
        return conversations.computeIfAbsent(npcId, id -> {
            File file = new File(dataFolder, id.toString() + ".yml");
            Conversation conv = new Conversation(plugin.getPluginConfig().getMaxMemoryMessages());
            if (file.exists()) {
                conv.load(file);
            }
            return conv;
        });
    }

    public void exportTo(File dest) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        int i = 0;
        for (Map.Entry<UUID, Conversation> entry : conversations.entrySet()) {
            for (Conversation.Message msg : entry.getValue().getMessages()) {
                yaml.set("conversation." + i + ".npc", entry.getKey().toString());
                yaml.set("conversation." + i + ".role", msg.getRole());
                yaml.set("conversation." + i + ".content", msg.getContent());
                i++;
            }
        }
        yaml.save(dest);
    }

    public void saveAll() {
        for (Map.Entry<UUID, Conversation> entry : conversations.entrySet()) {
            File file = new File(dataFolder, entry.getKey().toString() + ".yml");
            try {
                entry.getValue().save(file);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save conversation for " + entry.getKey(), e);
            }
        }
    }

    /**
     * Memory search by topic. Searches all conversations for messages containing the given keywords.
     */
    public List<String> searchByTopic(String query) {
        String lower = query.toLowerCase();
        List<String> results = new ArrayList<>();
        for (Map.Entry<UUID, Conversation> entry : conversations.entrySet()) {
            for (Conversation.Message msg : entry.getValue().getMessages()) {
                if (msg.getContent().toLowerCase().contains(lower)) {
                    results.add("[" + entry.getKey().toString().substring(0, 8) + "] " 
                        + msg.getRole() + ": " + msg.getContent());
                    if (results.size() >= 10) return results;
                }
            }
        }
        return results;
    }

    /**
     * Get all unique topics from conversation history based on keyword matching.
     */
    public List<String> getTopics() {
        java.util.Set<String> topics = new java.util.LinkedHashSet<>();
        String[][] patterns = {
            {"mining", "mine", "dig", "ore", "diamond"},
            {"building", "build", "place", "construct"},
            {"combat", "fight", "attack", "kill", "defend"},
            {"farming", "farm", "plant", "crop", "harvest"},
            {"crafting", "craft", "make", "smith"},
            {"exploration", "explore", "go", "travel", "move"},
            {"trading", "trade", "give", "get"},
            {"endgame", "nether", "portal", "dragon", "end"},
        };
        for (Map.Entry<UUID, Conversation> entry : conversations.entrySet()) {
            for (Conversation.Message msg : entry.getValue().getMessages()) {
                String c = msg.getContent().toLowerCase();
                for (String[] pattern : patterns) {
                    for (String keyword : pattern) {
                        if (c.contains(keyword)) {
                            topics.add(pattern[0]); // Add the category name
                            break;
                        }
                    }
                }
            }
        }
        return new ArrayList<>(topics);
    }
}
