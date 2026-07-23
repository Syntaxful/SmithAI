package com.smithai.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SmithAITabCompleter implements TabCompleter {

    private static final List<String> SUBS = Arrays.asList(
        "spawn", "despawn", "follow", "stay", "goto", "do", "debug", "health", "stop", "status", "model", "version", "reload", "train", "feedback", "feedback-list", "report", "reports", "memory", "inventory", "give", "list", "help", "teleport", "skin", "data", "config", "export"
    );
    private static final List<String> TRAIN_OPTIONS = Arrays.asList("good", "bad", "reset");
    private static final List<String> DO_SUGGESTIONS = Arrays.asList(
        "get diamonds", "find iron", "make nether portal", "build base", "beat the game",
        "fight", "get food", "farm", "build shelter", "explore cave", "follow", "stay", "come"
    );
    private static final List<String> FEEDBACK_SUGGESTIONS = Arrays.asList(
        "broke the wrong block", "went the wrong way", "ignored my command", "wasted resources", "attacked the wrong target"
    );
    private static final List<String> REPORT_SUGGESTIONS = Arrays.asList(
        "broke wrong block", "won't follow", "ignored command", "said wrong thing", "stuck in place", "attacked wrong mob"
    );
    private static final List<String> DEBUG_OPTIONS = Arrays.asList("global", "on", "off");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBS;
        }
        if (args.length >= 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("train")) {
                return TRAIN_OPTIONS;
            }
            if (sub.equals("do")) {
                return DO_SUGGESTIONS;
            }
            if (sub.equals("feedback")) {
                return FEEDBACK_SUGGESTIONS;
            }
            if (sub.equals("report")) {
                return REPORT_SUGGESTIONS;
            }
            if (sub.equals("debug")) {
                return DEBUG_OPTIONS;
            }
            if (sub.equals("goto")) {
                return Collections.emptyList(); // coordinates are free-form
            }
        }
        return Collections.emptyList();
    }
}
