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

        // Automatic negative feedback detection
        if (isNegativeFeedback(cleanedMessage)) {
            plugin.getFeedbackManager().recordFeedback(player.getName(), cleanedMessage, "chat");
        }

        // Automatic report detection
        if (isReportRequest(cleanedMessage)) {
            String reportUrl = buildReportUrl(player, npc, cleanedMessage);
            player.sendMessage("§eReport SmithAI issue here:");
            player.sendMessage(reportUrl.length() > 500 ? reportUrl.substring(0, 500) + "..." : reportUrl);
            npc.sendMessage(player, "I see something is broken. Please use that link to report it so the developers can fix me.");
            return;
        }

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

    private boolean isNegativeFeedback(String message) {
        String lower = message.toLowerCase();
        return lower.contains("don't do that") || lower.contains("dont do that") ||
            lower.contains("stop doing") || lower.contains("wrong") ||
            lower.contains("bad idea") || lower.contains("don't do") ||
            lower.contains("never do that") || lower.contains("that's not right") ||
            lower.contains("that is not right") || lower.contains("you messed up") ||
            lower.contains("you did wrong") || lower.contains("not what i asked") ||
            lower.contains("feedback");
    }

    private boolean isReportRequest(String message) {
        String lower = message.toLowerCase();
        return lower.contains("report") || lower.contains("bug") || lower.contains("broken") ||
            lower.contains("not working") || lower.contains("issue") || lower.contains("glitch") ||
            lower.contains("this is wrong") || lower.contains("something is wrong");
    }

    private String buildReportUrl(Player player, SmithNPC npc, String message) {
        StringBuilder body = new StringBuilder();
        body.append("### What went wrong\n");
        body.append(message).append("\n\n");
        body.append("### What I expected\n");
        body.append("(describe what you expected to happen)\n\n");
        body.append("### Steps to reproduce\n");
        body.append("1. \n2. \n3. \n\n");
        body.append("### Details\n");
        body.append("- Server type: ").append(player.getServer().getName()).append("\n");
        body.append("- SmithAI version: 2.0.0\n");
        body.append("- Active brain: ").append(plugin.getAiManager().getActiveModelName()).append("\n");
        body.append("- External connected: ").append(plugin.getAiManager().isExternalConnected()).append("\n");
        body.append("- NPC task busy: ").append(plugin.getSkillExecutor().isBusy()).append("\n");

        String issueTitle = "[Bug Report] " + player.getName() + " - " + (message.length() > 40 ? message.substring(0, 40) + "..." : message);
        try {
            return "https://github.com/Syntaxful/SmithAI/issues/new?title=" +
                java.net.URLEncoder.encode(issueTitle, "UTF-8").replace("+", "%20") +
                "&body=" + java.net.URLEncoder.encode(body.toString(), "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return "https://github.com/Syntaxful/SmithAI/issues/new";
        }
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
