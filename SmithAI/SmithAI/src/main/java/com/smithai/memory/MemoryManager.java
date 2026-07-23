package com.smithai.memory;

import com.smithai.SmithAIPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
}
