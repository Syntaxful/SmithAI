package com.smithai.chat;

import com.smithai.SmithAIPlugin;
import com.smithai.ai.AIManager;
import com.smithai.memory.Conversation;
import com.smithai.memory.MemoryManager;
import com.smithai.npc.NPCManager;
import com.smithai.npc.SmithNPC;
import com.smithai.skills.SkillExecutor;
import com.smithai.skills.SkillExecutor.SkillTask;
import com.smithai.skills.TaskPlanner;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatManager implements Listener {

    private final SmithAIPlugin plugin;
    private final AIManager aiManager;
    private final MemoryManager memoryManager;
    private final NPCManager npcManager;

    /** "give me / get me / fetch me N of <item>" — counted-fetch pattern. */
    private static final Pattern GIVE_ME_PATTERN = Pattern.compile(
        "\\b(?:give|get|fetch|bring|deliver|hand)\\s+(?:me\\s+)?(\\d+)\\s+(?:of\\s+)?([a-z_][a-z0-9_]+)");

    public ChatManager(SmithAIPlugin plugin) {
        this.plugin = plugin;
        this.aiManager = plugin.getAiManager();
        this.memoryManager = plugin.getMemoryManager();
        this.npcManager = plugin.getNpcManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        String aiName = plugin.getPluginConfig().getAiName();

        if (!isAddressedToSmith(player, message, aiName)) {
            return;
        }

        // Snapshot player inventory into a name->count map for the AI to know what the
        // player already has. This is what makes "give me N diamonds" actually mean
        // "give me N that I don't yet have".
        final Map<String, Integer> inventory = snapshotInventory(player);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
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
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§eReport SmithAI issue here:");
                    player.sendMessage(reportUrl.length() > 500 ? reportUrl.substring(0, 500) + "..." : reportUrl);
                    npc.sendMessage(player, "I see something is broken. Please use that link to report it so the developers can fix me.");
                });
                return;
            }

            // 1) "give/get me N of <item>" — handled locally so it queues with a real count.
            Map<String, Object> giveMe = parseGiveMe(cleanedMessage);
            String task = detectTask(cleanedMessage);

            aiManager.getResponse(player, cleanedMessage, conversation, task, inventory)
                .thenAccept(reply -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        conversation.addMessage("assistant", reply);
                        npc.sendMessage(player, reply);
                        // Queue any action tag the AI returned, e.g. [action:follow_player]
                        Map<String, Object> parsed = parseActionTag(reply);
                        Map<String, Object> params = new HashMap<>();
                        params.put("inventory", inventory); // give dispatcher context too
                        String actionToRun = null;
                        int countToRun = 0;
                        String targetToRun = null;
                        if (parsed != null) {
                            actionToRun = String.valueOf(parsed.get("action"));
                            Object t = parsed.get("target");
                            Object q = parsed.get("quantity");
                            targetToRun = t != null ? String.valueOf(t) : null;
                            countToRun = q instanceof Number ? ((Number) q).intValue() : 0;
                        }
                        // Prefer the local give-me match when it's clearer than the AI tag
                        // so the queue always runs with the right count.
                        if (giveMe != null) {
                            actionToRun = "gather_item";
                            targetToRun = String.valueOf(giveMe.get("target"));
                            int want = (int) giveMe.get("want");
                            int have = inventory.getOrDefault(targetToRun, 0);
                            countToRun = Math.max(1, want - have);
                            params.put("target", targetToRun);
                            params.put("count", countToRun);
                            plugin.getSkillExecutor().queue(npc, "gather_item", params, player);
                        } else if (actionToRun != null) {
                            if (targetToRun != null) params.put("target", targetToRun);
                            if (countToRun > 0) params.put("count", countToRun);
                            plugin.getSkillExecutor().queue(npc, actionToRun, params, player);
                        }
                        // If the player's message matched a known task, also queue its plan
                        if (task != null) {
                            List<String> plan = TaskPlanner.plan(task);
                            String key = TaskPlanner.matchingKey(task);
                            Map<String, Integer> counts = key == null ? java.util.Collections.emptyMap() : TaskPlanner.countsFor(key);
                            if (!plan.isEmpty()) {
                                plugin.getSkillExecutor().queuePlan(npc, plan, counts, player);
                            }
                        }
                    });
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
        body.append("- SmithAI version: 2.1.0\n");
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
     * Parse a "give/get me N of &lt;item&gt;" phrase and return target + quantity, with the
     * player's already-known stock subtracted when possible.
     */
    private Map<String, Object> parseGiveMe(String message) {
        Matcher matcher = GIVE_ME_PATTERN.matcher(message.toLowerCase());
        if (!matcher.find()) return null;
        try {
            int count = Math.max(1, Integer.parseInt(matcher.group(1)));
            String raw = matcher.group(2).replaceAll("[^a-z0-9_]", "");
            if (raw.isEmpty()) return null;
            Map<String, Object> result = new HashMap<>();
            result.put("target", raw);
            result.put("want", count);
            return result;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Build a small inventory snapshot keyed by Bukkit material name so the AI knows what the
     * player already has at chat time.
     */
    private Map<String, Integer> snapshotInventory(Player player) {
        Map<String, Integer> snap = new HashMap<>();
        PlayerInventory inv = player.getInventory();
        if (inv == null) return snap;
        for (ItemStack stack : inv.getContents()) {
            if (stack == null || stack.getType() == null || stack.getType() == Material.AIR) continue;
            String key = stack.getType().name().toLowerCase();
            snap.merge(key, stack.getAmount(), Integer::sum);
        }
        return snap;
    }

    /**
     * Parses an [action:skillName] or [action:skillName,target:N] tag from an AI reply.
     * Returns a map with "action", optionally "target", and optionally "quantity".
     */
    private Map<String, Object> parseActionTag(String reply) {
        Pattern pattern = Pattern.compile("\\[action:([^\\],\\s]+)(?:,([^\\]:\\s]+))?(?::(\\d+))?\\]");
        Matcher matcher = pattern.matcher(reply);
        if (!matcher.find()) return null;
        Map<String, Object> result = new HashMap<>();
        result.put("action", matcher.group(1).trim().toLowerCase().replace(" ", "_"));
        if (matcher.group(2) != null) {
            result.put("target", matcher.group(2).trim());
        }
        if (matcher.group(3) != null) {
            try {
                result.put("quantity", Integer.parseInt(matcher.group(3)));
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }
}
