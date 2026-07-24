package com.smithai.memory;

import com.smithai.SmithAIPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
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
        this(plugin, new File(plugin.getDataFolder(), "memory"));
    }

    public MemoryManager(SmithAIPlugin plugin, File dataFolder) {
        this.plugin = plugin;
        this.dataFolder = dataFolder;
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
