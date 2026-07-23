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

        if (lower.contains("hello") || lower.contains("hi")) {
            return "Hello, " + player.getName() + "! I'm Smith_AI running on Smith-Mini 1.0 with " + skills.size() + " core skills.";
        }
        if (lower.contains("follow")) {
            return "I'll follow you.";
        }
        if (lower.contains("stay")) {
            return "I'll stay here.";
        }
        if (lower.contains("diamond")) {
            return "I'll help you find diamonds. We need to mine at Y=-59 with an iron pickaxe. Available skills: " + skills.size() + ".";
        }
        if (lower.contains("nether") || lower.contains("portal")) {
            return "To make a nether portal, we need 10 obsidian and a flint and steel.";
        }
        if (lower.contains("good") || lower.contains("thanks")) {
            return "You're welcome!";
        }
        if (lower.contains("bad") || lower.contains("stop")) {
            return "Sorry, I'll do better.";
        }
        if (lower.contains("skill") || lower.contains("can you do")) {
            return "I currently have " + skills.size() + " core skills available.";
        }
        if (task != null) {
            return "I'm working on: " + task + " (using Smith-Mini 1.0).";
        }
        if (!knowledge.isEmpty()) {
            return "I know this about that: " + knowledge.get(0);
        }

        return "I'm running on Smith-Mini 1.0 with " + skills.size() + " core skills. Ask me to follow you, get diamonds, or build a nether portal.";
    }
}
