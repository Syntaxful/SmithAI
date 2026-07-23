package com.smithai.commands;

import com.smithai.SmithAIPlugin;
import com.smithai.config.Config;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SmithAPICommand implements CommandExecutor {

    private final SmithAIPlugin plugin;

    public SmithAPICommand(SmithAIPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("smithai.api")) {
            sender.sendMessage("§cYou do not have permission to use /SmithAPI.");
            return true;
        }

        Config config = plugin.getPluginConfig();

        if (args.length == 0) {
            String key = config.getExternalApiKey();
            if (key == null || key.isEmpty()) {
                sender.sendMessage("§eNo API key is set. Use §f/SmithAPI set SMA-... §eto set one.");
                sender.sendMessage("§eGet the key from the SmithAI-Server console.");
            } else {
                String masked = maskKey(key);
                sender.sendMessage("§aSmithAI API key is set: §f" + masked);
                sender.sendMessage("§eExternal server: §f" + config.getExternalUrl());
            }
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("set")) {
            String key = args[1];
            if (!isValidKey(key)) {
                sender.sendMessage("§cInvalid API key. It must start with §fSMA-§c and contain only letters, numbers, and hyphens.");
                return true;
            }

            config.setExternalApiKey(key);
            plugin.saveConfig();
            plugin.getAiManager().reload();

            sender.sendMessage("§aSmithAI API key updated.");
            sender.sendMessage("§eRe-checking connection to external server...");
            return true;
        }

        sender.sendMessage("§eUsage: §f/SmithAPI set <SMA-...> §eor §f/SmithAPI");
        return true;
    }

    private boolean isValidKey(String key) {
        return key != null && key.startsWith("SMA-") && key.length() > 4 && key.matches("^[A-Za-z0-9-]+$");
    }

    private String maskKey(String key) {
        if (key.length() <= 12) {
            return key.substring(0, Math.min(key.length(), 4)) + "****";
        }
        return key.substring(0, 8) + "****" + key.substring(key.length() - 4);
    }
}
