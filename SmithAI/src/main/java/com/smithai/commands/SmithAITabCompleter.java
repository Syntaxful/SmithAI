package com.smithai.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SmithAITabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("smithai")) {
            if (args.length == 1) {
                suggestions.addAll(Arrays.asList("spawn", "despawn", "follow", "stay", "do", "status", "model", "reload", "train", "memory"));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("do")) {
                suggestions.addAll(Arrays.asList("get diamonds", "find diamonds", "make nether portal", "build nether portal", "beat the game", "build shelter", "farm", "defend"));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("train")) {
                suggestions.addAll(Arrays.asList("good", "bad"));
            }
        } else if (cmd.equals("smithapi")) {
            if (args.length == 1) {
                suggestions.add("set");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
                suggestions.add("SMA-");
            }
        }

        String partial = args[args.length - 1].toLowerCase();
        List<String> filtered = new ArrayList<>();
        for (String s : suggestions) {
            if (s.toLowerCase().startsWith(partial)) {
                filtered.add(s);
            }
        }
        return filtered;
    }
}
