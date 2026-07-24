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

        aiManager.getResponse(player, cleanedMessage, conversation, task)
            .thenAccept(reply -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    conversation.addMessage("assistant", reply);
                    npc.sendMessage(player, reply);
                    // Queue any action tag the AI returned, e.g. [action:follow_player]
                    java.util.Map<String, String> parsed = parseActionTag(reply);
                    if (parsed != null) {
                        String action = parsed.get("action");
                        String target = parsed.get("target");
                        java.util.Map<String, Object> params = new java.util.HashMap<>();
                        if (target != null) params.put("target", target);
                        plugin.getSkillExecutor().queue(npc, action, params, player);
                    }
                    // If the player's message matched a known task, also queue its plan
                    if (task != null) {
                        java.util.List<String> plan = TaskPlanner.plan(task);
                        if (!plan.isEmpty()) {
                            plugin.getSkillExecutor().queuePlan(npc, plan, player);
                        }
                    }
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

    /**
     * Parses an [action:skillName] or [action:skillName,target] tag from an AI reply.
     * Returns a map with "action" and optionally "target", or null if no tag is found.
     */
    private java.util.Map<String, String> parseActionTag(String reply) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[action:([^\\],]+)(?:,([^\\]]+))?\\]");
        java.util.regex.Matcher matcher = pattern.matcher(reply);
        if (!matcher.find()) return null;
        java.util.Map<String, String> result = new java.util.HashMap<>();
        result.put("action", matcher.group(1).trim().toLowerCase().replace(" ", "_"));
        if (matcher.group(2) != null) {
            result.put("target", matcher.group(2).trim());
        }
        return result;
    }
}
