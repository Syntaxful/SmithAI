package com.smithai.ai;

import com.smithai.SmithAIPlugin;
import com.smithai.memory.Conversation;
import org.bukkit.entity.Player;

import java.util.List;

public class LocalMiniAI {

    private final SmithAIPlugin plugin;

    public LocalMiniAI(SmithAIPlugin plugin) {
        this.plugin = plugin;
    }

    public String getResponse(Player player, String message, Conversation conversation, String task, List<String> knowledge, List<String> skills) {
        String lower = message.toLowerCase();

        // Negative feedback / correction detection
        if (isNegativeFeedback(lower)) {
            plugin.getFeedbackManager().recordFeedback(player.getName(), message, task != null ? task : "chat");
            return "Got it. I'll try not to do that again. What would you like me to do instead?";
        }

        // Greetings
        if (lower.matches(".*\\b(hi|hello|hey|greetings|howdy)\\b.*")) {
            return "Hello, " + player.getName() + "! I'm Smith_AI running on Smith-Mini 1.0 with " + skills.size() + " core skills.";
        }

        // Follow / stay / movement commands
        if (lower.contains("follow")) return "I'll follow you. [action:follow_player]";
        if (lower.contains("stay")) return "I'll stay here. [action:stay]";
        if (lower.contains("come")) return "I'm coming to you. [action:teleport_to_player]";
        if (lower.contains("stop")) return "Stopping tasks. [action:cancel_task]";

        // Mining and progression
        if (lower.contains("diamond") || lower.contains("find diamonds") || lower.contains("get diamonds")) {
            return "Diamonds are most common around Y=-59. I'll need an iron pickaxe and plenty of torches. [action:mine_block]";
        }
        if (lower.contains("iron") || lower.contains("find iron")) {
            return "Iron ore generates up to Y=72. I'll dig down and mine it. [action:mine_block]";
        }
        if (lower.contains("gold") || lower.contains("find gold")) {
            return "Gold ore is found underground around Y=-16. [action:mine_block]";
        }
        if (lower.contains("nether") || lower.contains("portal")) {
            return "To make a nether portal, we need obsidian and a flint and steel. [action:build_nether_portal]";
        }
        if (lower.contains("end") || lower.contains("dragon") || lower.contains("stronghold")) {
            return "To reach the End, we need eyes of ender. [action:defeat_ender_dragon]";
        }

        // Building and base
        if (lower.contains("base") || lower.contains("house") || lower.contains("shelter") || lower.contains("build")) {
            return "I'll help build a base. [action:build_base]";
        }
        if (lower.contains("farm") || lower.contains("crop") || lower.contains("food")) {
            return "I can make a farm by tilling dirt and planting seeds. [action:farm_crops]";
        }
        if (lower.contains("torch") || lower.contains("light")) {
            return "I can place torches to light up the area. [action:place_torch]";
        }

        // Combat and survival
        if (lower.contains("fight") || lower.contains("kill") || lower.contains("attack") || lower.contains("defend")) {
            return "I'll equip a weapon and fight nearby hostile mobs. [action:fight_hostile_mob]";
        }
        if (lower.contains("heal") || lower.contains("health") || lower.contains("hurt")) {
            return "I can eat food to recover health. [action:eat_food]";
        }
        if (lower.contains("weapon") || lower.contains("sword") || lower.contains("axe") || lower.contains("tool")) {
            return "I can select the best tool or weapon from my hotbar. [action:equip_tool]";
        }

        // Feedback
        if (lower.contains("good") || lower.contains("great") || lower.contains("thanks") || lower.contains("thank you")) {
            return "You're welcome! Let me know if you need anything else.";
        }
        if (lower.contains("bad") || lower.contains("wrong")) {
            return "Sorry, tell me exactly what I did wrong with /smithai feedback <message> so I can improve.";
        }

        // Skill / capability questions
        if (lower.contains("skill") || lower.contains("can you") || lower.contains("what can you do") || lower.contains("help")) {
            return "I have " + skills.size() + " core skills available. I can follow you, mine, build, farm, fight, craft, and explore. Ask me to do any of those.";
        }
        if (lower.contains("model") || lower.contains("brain") || lower.contains("tier") || lower.contains("smithgpt")) {
            return "I'm running Smith-Mini 1.0. You can connect to SmithGPT 1.0 (7.5GB) or 2.0 (15GB) using the SmithAI-Server.";
        }

        // Knowledge-backed response
        if (!knowledge.isEmpty()) {
            return "I know this: " + knowledge.get(0);
        }

        // Task context
        if (task != null) {
            return "I'm working on: " + task + " (using Smith-Mini 1.0).";
        }

        return "I'm running on Smith-Mini 1.0 with " + skills.size() + " core skills. Ask me to follow you, mine, build, farm, fight, or explore.";
    }

    private boolean isNegativeFeedback(String lower) {
        return lower.contains("don't do that") || lower.contains("dont do that") ||
            lower.contains("stop doing") || lower.contains("wrong") ||
            lower.contains("bad idea") || lower.contains("don't do") ||
            lower.contains("never do that") || lower.contains("that's not right") ||
            lower.contains("that is not right") || lower.contains("you messed up") ||
            lower.contains("you did wrong") || lower.contains("not what i asked") ||
            lower.contains("feedback");
    }
}
