package com.smithai.chat;

import com.smithai.SmithAIPlugin;
import com.smithai.ai.AIManager;
import com.smithai.memory.Conversation;
import com.smithai.memory.MemoryManager;
import com.smithai.npc.NPCManager;
import com.smithai.npc.SmithNPC;
import com.smithai.skills.TaskPlanner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;

public class ChatManager implements Listener {

    private final SmithAIPlugin plugin;
    private final AIManager aiManager;
    private final MemoryManager memoryManager;
    private final NPCManager npcManager;

    public ChatManager(SmithAIPlugin plugin) {
        this.plugin = plugin;
        this.aiManager = plugin.getAiManager();
        this.memoryManager = plugin.getMemoryManager();
        this.npcManager = plugin.getNpcManager();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        String aiName = plugin.getPluginConfig().getAiName();

        if (!isAddressedToSmith(player, message, aiName)) {
            return;
        }

        List<SmithNPC> nearbyNPCs = npcManager.getNearbyNPCs(player.getLocation(), 16);
        if (nearbyNPCs.isEmpty()) {
            return;
        }

        SmithNPC npc = nearbyNPCs.get(0);
        String cleanedMessage = stripAddress(message, aiName);
        Conversation conversation = memoryManager.getConversation(npc.getId());
        conversation.addMessage("user", cleanedMessage);

        String task = detectTask(cleanedMessage);
        if (task != null) {
            npc.sendMessage(player, "Task understood: " + task + ". I'll use my " + aiManager.getActiveSkillTier() + " skill set (" + aiManager.getAvailableSkills().size() + " skills).");
        }

        aiManager.getResponse(player, cleanedMessage, conversation, task)
            .thenAccept(reply -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    conversation.addMessage("assistant", reply);
                    npc.sendMessage(player, reply);
                });
            });
    }

    private boolean isAddressedToSmith(Player player, String message, String aiName) {
        String name = aiName.toLowerCase();
        String lower = message.toLowerCase();
        return lower.contains(name) || lower.contains("smith") || lower.contains("@ai") || lower.contains("hey ai");
    }

    private String stripAddress(String message, String aiName) {
        String cleaned = message;
        cleaned = cleaned.replaceAll("(?i)" + aiName, "");
        cleaned = cleaned.replaceAll("(?i)smith", "");
        cleaned = cleaned.replaceAll("(?i)@ai", "");
        cleaned = cleaned.replaceAll("(?i)hey ai", "");
        cleaned = cleaned.replaceAll("[,.!?\\s]+", " ").trim();
        return cleaned.isEmpty() ? message : cleaned;
    }

    private String detectTask(String message) {
        String lower = message.toLowerCase();
        for (String task : TaskPlanner.TASKS.keySet()) {
            if (lower.contains(task)) {
                return task;
            }
        }
        return null;
    }
}
