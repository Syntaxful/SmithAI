package com.smithai.commands;

import com.smithai.SmithAIPlugin;
import com.smithai.health.SubsystemHealth;
import com.smithai.npc.NPCManager;
import com.smithai.npc.SmithNPC;
import com.smithai.skills.TaskPlanner;
import com.smithai.util.VersionInfo;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.List;

public class SmithAICommand implements CommandExecutor {

    private final SmithAIPlugin plugin;
    private final NPCManager npcManager;

    public SmithAICommand(SmithAIPlugin plugin) {
        this.plugin = plugin;
        this.npcManager = plugin.getNpcManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eSmithAI §7v2.0.0 §e- Usage: /smithai <command>");
            sender.sendMessage("§7Or ask me: §f/smithai what command spawns you §7/ §f/smithai how do I see your inventory");
            return true;
        }

        String sub = args[0].toLowerCase();

        // Natural language command lookup — if first word isn't a known command, treat as query
        if (!isKnownCommand(sub)) {
            String query = String.join(" ", args);
            String matched = lookupCommand(query);
            if (matched != null) {
                sender.sendMessage("§e" + matched);
            } else {
                sender.sendMessage("§7I didn't understand that. Try /smithai help to see all commands.");
            }
            return true;
        }

        switch (sub) {
            case "spawn":
                if (!sender.hasPermission("smithai.spawn")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can spawn Smith_AI.");
                    return true;
                }
                Player spawner = (Player) sender;
                SmithNPC npc = npcManager.spawn(spawner.getLocation());
                if (npc != null) {
                    sender.sendMessage("§aSpawned " + plugin.getPluginConfig().getAiName() + ".");
                } else {
                    sender.sendMessage("§cFailed to spawn NPC.");
                }
                return true;

            case "despawn":
                if (!sender.hasPermission("smithai.spawn")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                int count = 0;
                for (SmithNPC n : npcManager.getAllNPCs()) {
                    npcManager.despawn(n.getId());
                    count++;
                }
                sender.sendMessage("§aDespawned " + count + " NPC(s).");
                return true;

            case "follow":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can use this.");
                    return true;
                }
                Player follower = (Player) sender;
                List<SmithNPC> nearbyFollow = npcManager.getNearbyNPCs(follower.getLocation(), 16);
                if (nearbyFollow.isEmpty()) {
                    sender.sendMessage("§cNo Smith_AI nearby.");
                    return true;
                }
                SmithNPC fnpc = nearbyFollow.get(0);
                fnpc.follow(follower);
                fnpc.sendMessage(follower, "I'll follow you.");
                return true;

            case "stay":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can use this.");
                    return true;
                }
                Player stayer = (Player) sender;
                List<SmithNPC> nearbyStay = npcManager.getNearbyNPCs(stayer.getLocation(), 16);
                if (nearbyStay.isEmpty()) {
                    sender.sendMessage("§cNo Smith_AI nearby.");
                    return true;
                }
                SmithNPC snpc = nearbyStay.get(0);
                snpc.stay();
                snpc.sendMessage(stayer, "I'll stay here.");
                return true;

            case "goto":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can use this.");
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage("§eUsage: §f/smithai goto <x> <y> <z>");
                    return true;
                }
                Player gotoer = (Player) sender;
                List<SmithNPC> nearbyGoto = npcManager.getNearbyNPCs(gotoer.getLocation(), 16);
                if (nearbyGoto.isEmpty()) {
                    sender.sendMessage("§cNo Smith_AI nearby.");
                    return true;
                }
                try {
                    double x = Double.parseDouble(args[1]);
                    double y = Double.parseDouble(args[2]);
                    double z = Double.parseDouble(args[3]);
                    Location target = new Location(gotoer.getWorld(), x, y, z);
                    SmithNPC gnpc = nearbyGoto.get(0);
                    gnpc.setMoveTarget(target);
                    gnpc.sendMessage(gotoer, "I'll go to those coordinates.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cCoordinates must be numbers.");
                }
                return true;

            case "do":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can use this.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§eUsage: §f/smithai do <task>§e, e.g. get diamonds, make nether portal, beat the game");
                    return true;
                }
                Player doer = (Player) sender;
                List<SmithNPC> nearbyDo = npcManager.getNearbyNPCs(doer.getLocation(), 16);
                if (nearbyDo.isEmpty()) {
                    sender.sendMessage("§cNo Smith_AI nearby.");
                    return true;
                }
                SmithNPC dnpc = nearbyDo.get(0);
                String task = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                List<String> plan = TaskPlanner.plan(task);
                dnpc.sendMessage(doer, TaskPlanner.describePlan(task, plan));
                if (!plan.isEmpty()) {
                    plugin.getSkillExecutor().queuePlan(dnpc, plan, doer);
                    plugin.getTrainingManager().recordGood("task:" + task);
                }
                return true;

            case "debug":
                if (!sender.hasPermission("smithai.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can use this.");
                    return true;
                }
                Player debugger = (Player) sender;
                if (args.length >= 2 && args[1].equalsIgnoreCase("global")) {
                    plugin.getDebugManager().toggleGlobal(debugger);
                } else {
                    plugin.getDebugManager().toggle(debugger);
                }
                return true;

            case "health":
                if (!sender.hasPermission("smithai.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                SubsystemHealth health = plugin.getSubsystemHealth();
                sender.sendMessage("§eSmithAI subsystem health:");
                health.getSummary().forEach((sys, msg) -> {
                    sender.sendMessage("§7- " + sys + ": §f" + msg);
                });
                sender.sendMessage("§eOverall: §f" + (health.isHealthy() ? "Healthy" : "Degraded"));
                return true;

            case "status":
                VersionInfo statusVersion = new VersionInfo();
                sender.sendMessage("§eActive brain: §f" + plugin.getAiManager().getActiveModelName());
                sender.sendMessage("§eExternal connected: §f" + plugin.getAiManager().isExternalConnected());
                sender.sendMessage("§eServer: §f" + statusVersion.getFriendlyName());
                sender.sendMessage("§eDeepslate: §f" + (statusVersion.hasDeepslate() ? "yes" : "no") + " §7| §eNetherite: §f" + (statusVersion.hasNetherite() ? "yes" : "no"));
                return true;

            case "model":
                sender.sendMessage("§eAvailable models:");
                sender.sendMessage("§7- Smith-Mini 1.0 (built-in)");
                sender.sendMessage("§7- SmithGPT 1.0 (4GB, external)");
                sender.sendMessage("§7- SmithGPT 2.0 (7.5GB, external)");
                sender.sendMessage("§eActive: §f" + plugin.getAiManager().getActiveModelName());
                return true;

            case "version":
                VersionInfo versionInfo = new VersionInfo();
                sender.sendMessage("§eSmithAI version: §f" + plugin.getDescription().getVersion());
                sender.sendMessage("§eServer version: §f" + versionInfo.getFriendlyName());
                sender.sendMessage("§eBukkit version: §f" + versionInfo.getRawBukkitVersion());
                sender.sendMessage("§eDeepslate: §f" + (versionInfo.hasDeepslate() ? "yes" : "no") + " §7| §eNetherite: §f" + (versionInfo.hasNetherite() ? "yes" : "no"));
                sender.sendMessage("§eBest diamond Y: §f" + versionInfo.bestDiamondY() + " §7| §eiron Y: §f" + versionInfo.bestIronY() + " §7| §egold Y: §f" + versionInfo.bestGoldY());
                return true;

            case "reload":
                if (!sender.hasPermission("smithai.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                plugin.reload();
                sender.sendMessage("§aSmithAI reloaded.");
                return true;

            case "stop":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can use this.");
                    return true;
                }
                Player stopper = (Player) sender;
                List<SmithNPC> nearbyStop = npcManager.getNearbyNPCs(stopper.getLocation(), 16);
                if (nearbyStop.isEmpty()) {
                    sender.sendMessage("§cNo Smith_AI nearby.");
                    return true;
                }
                plugin.getSkillExecutor().cancelAll();
                nearbyStop.get(0).sendMessage(stopper, "All tasks stopped.");
                return true;

            case "memory":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can view memory.");
                    return true;
                }
                Player p = (Player) sender;
                List<SmithNPC> nearby = npcManager.getNearbyNPCs(p.getLocation(), 16);
                if (nearby.isEmpty()) {
                    sender.sendMessage("§cNo Smith_AI nearby.");
                    return true;
                }
                sender.sendMessage("§eMemory for nearby " + plugin.getPluginConfig().getAiName() + ":");
                plugin.getMemoryManager().getConversation(nearby.get(0).getId()).getMessages()
                    .forEach(m -> sender.sendMessage("§7[" + m.getRole() + "] §f" + m.getContent()));
                return true;

            case "train":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can use this.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§eUsage: §f/smithai train good|bad|reset|import §eor §f/smithai train reset <player>");
                    sender.sendMessage("§7/smithai train import §f- paste RL data CSV to train the AI");
                    return true;
                }
                Player trainer = (Player) sender;
                String trainAction = args[1].toLowerCase();

                if (trainAction.equals("import")) {
                    if (!sender.hasPermission("smithai.admin")) {
                        sender.sendMessage("§cNo permission.");
                        return true;
                    }
                    sender.sendMessage("§ePaste RL data CSV and run /smithai train import <action>,<type>,<score>");
                    sender.sendMessage("§7Format: R,action_name,1  (for reward) or P,action_name,-1  (for punish)");
                    sender.sendMessage("§7Example: §f/smithai train import R,mine_diamond,1");
                    // Parse inline data
                    if (args.length >= 4) {
                        StringBuilder csv = new StringBuilder();
                        for (int i = 2; i < args.length; i++) csv.append(args[i]).append(" ");
                        String line = csv.toString().trim();
                        String[] parts = line.split(",");
                        if (parts.length >= 3) {
                            String rlType = parts[0].trim().toUpperCase();
                            String rlAction = parts[1].trim();
                            int rlScore;
                            try { rlScore = Integer.parseInt(parts[2].trim()); } catch (Exception e) { rlScore = 1; }
                            if (rlType.equals("R") || rlType.equals("P")) {
                                plugin.getTrainingManager().getRlRecorder().record(rlType, rlAction, rlScore);
                                if (rlType.equals("R")) plugin.getTrainingManager().recordGood(rlAction);
                                else plugin.getTrainingManager().recordBad(rlAction);
                                sender.sendMessage("§aImported: " + (rlType.equals("R") ? "Reward" : "Punishment") + " for " + rlAction + " (score: " + rlScore + ")");
                            }
                        }
                    }
                    return true;
                }

                List<SmithNPC> nearbyTrain = npcManager.getNearbyNPCs(trainer.getLocation(), 16);
                if (nearbyTrain.isEmpty() && !trainAction.equals("reset")) {
                    sender.sendMessage("§cNo Smith_AI nearby.");
                    return true;
                }
                SmithNPC tnpc = nearbyTrain.isEmpty() ? null : nearbyTrain.get(0);
                if (trainAction.equals("good")) {
                    String goodAction = args.length >= 3 ? String.join("_", java.util.Arrays.copyOfRange(args, 2, args.length)) : "general";
                    tnpc.sendMessage(trainer, "Thanks! I'll remember that as good (" + goodAction + ").");
                    plugin.getTrainingManager().recordGood(goodAction);
                } else if (trainAction.equals("bad")) {
                    String badAction = args.length >= 3 ? String.join("_", java.util.Arrays.copyOfRange(args, 2, args.length)) : "general";
                    tnpc.sendMessage(trainer, "Got it. I'll avoid that in the future (" + badAction + ").");
                    plugin.getTrainingManager().recordBad(badAction);
                } else if (trainAction.equals("reset")) {
                    if (!sender.hasPermission("smithai.admin")) {
                        sender.sendMessage("§cNo permission.");
                        return true;
                    }
                    String targetPlayer = args.length >= 3 ? args[2] : sender.getName();
                    plugin.getTrainingManager().resetFor(targetPlayer);
                    sender.sendMessage("§aTraining data reset for " + targetPlayer + ".");
                } else {
                    sender.sendMessage("§eUse §fgood <action>§e, §fbad <action>§e, or §fimport§e.");
                }
                return true;

            case "feedback":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can send feedback.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§eUsage: §f/smithai feedback <what I did wrong>");
                    sender.sendMessage("§7Example: §f/smithai feedback you broke the wrong block");
                    return true;
                }
                Player feedbackPlayer = (Player) sender;
                List<SmithNPC> nearbyFeedback = npcManager.getNearbyNPCs(feedbackPlayer.getLocation(), 16);
                if (nearbyFeedback.isEmpty()) {
                    sender.sendMessage("§cNo Smith_AI nearby.");
                    return true;
                }
                String feedbackMessage = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                String activeTask = plugin.getSkillExecutor().isBusy() ? "active-task" : "chat";
                plugin.getFeedbackManager().recordFeedback(feedbackPlayer.getName(), feedbackMessage, activeTask);
                nearbyFeedback.get(0).sendMessage(feedbackPlayer, "Thanks for the feedback. I'll try to avoid that next time.");
                return true;

            case "feedback-list":
                if (!sender.hasPermission("smithai.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                sender.sendMessage("§eRecent feedback entries:");
                plugin.getFeedbackManager().getRecent(10).forEach(e -> {
                    sender.sendMessage("§7[" + e.getPlayer() + "] §f" + e.getMessage());
                });
                return true;

            case "report":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can report issues.");
                    return true;
                }
                Player reporter = (Player) sender;
                List<SmithNPC> nearbyReport = npcManager.getNearbyNPCs(reporter.getLocation(), 16);
                SmithNPC reportNpc = nearbyReport.isEmpty() ? null : nearbyReport.get(0);
                StringBuilder body = new StringBuilder();
                body.append("### What went wrong\n");
                if (args.length >= 2) {
                    body.append(String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))).append("\n");
                } else {
                    body.append("(describe the problem here)\n");
                }
                body.append("\n### What I expected\n");
                body.append("(describe what you expected to happen)\n\n");
                body.append("### Steps to reproduce\n");
                body.append("1. \n2. \n3. \n\n");
                VersionInfo reportVersion = new VersionInfo();
                body.append("### Details\n");
                body.append("- Server type: ").append(reportVersion.getFriendlyName()).append("\n");
                body.append("- SmithAI version: ").append(plugin.getDescription().getVersion()).append("\n");
                body.append("- Active brain: ").append(plugin.getAiManager().getActiveModelName()).append("\n");
                body.append("- External connected: ").append(plugin.getAiManager().isExternalConnected()).append("\n");
                body.append("- NPC task busy: ").append(plugin.getSkillExecutor().isBusy()).append("\n");
                body.append("- Deepslate available: ").append(reportVersion.hasDeepslate()).append("\n");
                body.append("- Netherite available: ").append(reportVersion.hasNetherite()).append("\n");

                String issueTitle = "[Bug Report] " + reporter.getName() + " - " + (args.length >= 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, Math.min(args.length, 5))) : "SmithAI issue");
                String encodedTitle = encode(issueTitle);
                String encodedBody = encode(body.toString());
                String url = "https://github.com/Syntaxful/SmithAI/issues/new?title=" + encodedTitle + "&body=" + encodedBody;

                // Save a copy to disk in case the URL is too long for chat
                plugin.getIssueReportManager().recordReport(reporter.getName(), issueTitle, body.toString());

                sender.sendMessage("§eOpen this link to report the issue on GitHub:");
                if (url.length() <= 500) {
                    sender.sendMessage(url);
                } else {
                    sender.sendMessage("https://github.com/Syntaxful/SmithAI/issues/new");
                    sender.sendMessage("§7The link is too long for chat. A report has been saved to plugins/SmithAI/issue_reports.yml");
                    sender.sendMessage("§7Title: §f" + issueTitle);
                }
                sender.sendMessage("§7You can also copy the report from plugins/SmithAI/issue_reports.yml.");
                if (reportNpc != null) {
                    reportNpc.sendMessage(reporter, "Please open a GitHub issue with the details above so the developers can fix it.");
                }
                return true;

            case "reports":
                if (!sender.hasPermission("smithai.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                sender.sendMessage("§eRecent issue reports:");
                plugin.getIssueReportManager().getRecent(10).forEach(r -> {
                    sender.sendMessage("§7[" + r.getPlayer() + "] §f" + r.getTitle());
                });
                return true;

            case "inventory":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can use this.");
                    return true;
                }
                Player invPlayer = (Player) sender;
                SmithNPC invNpc = nearest(invPlayer);
                if (invNpc == null) {
                    sender.sendMessage("§cNo Smith_AI nearby.");
                    return true;
                }
                if (!invNpc.hasInventory()) {
                    sender.sendMessage("§cThis Smith_AI has no inventory.");
                    return true;
                }
                Inventory inv = invNpc.getInventory();
                sender.sendMessage("§e" + invNpc.getName() + "'s inventory:");
                if (inv != null) {
                    for (ItemStack stack : inv.getContents()) {
                        if (stack != null && stack.getType() != Material.AIR) {
                            sender.sendMessage("§7- " + stack.getAmount() + "x " + stack.getType().name().toLowerCase().replace("_", " "));
                        }
                    }
                }
                return true;

            case "give":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can use this.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§eUsage: §f/smithai give <item> [amount]");
                    return true;
                }
                Player givePlayer = (Player) sender;
                SmithNPC giveNpc = nearest(givePlayer);
                if (giveNpc == null) {
                    sender.sendMessage("§cNo Smith_AI nearby.");
                    return true;
                }
                if (!giveNpc.hasInventory()) {
                    sender.sendMessage("§cThis Smith_AI has no inventory.");
                    return true;
                }
                Material mat = Material.matchMaterial(args[1]);
                if (mat == null || mat == Material.AIR) {
                    sender.sendMessage("§cUnknown item: §f" + args[1]);
                    return true;
                }
                int amount = 1;
                if (args.length >= 3) {
                    try {
                        amount = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cAmount must be a number.");
                        return true;
                    }
                }
                Inventory npcInv = giveNpc.getInventory();
                ItemStack item = new ItemStack(mat, Math.max(1, amount));
                if (givePlayer.getInventory().containsAtLeast(item, item.getAmount())) {
                    givePlayer.getInventory().removeItem(item);
                    npcInv.addItem(item);
                    giveNpc.sendMessage(givePlayer, "Thanks, I now have " + item.getAmount() + " " + item.getType().name().toLowerCase().replace("_", " ") + ".");
                } else {
                    sender.sendMessage("§cYou don't have enough of that item.");
                }
                return true;

            case "list":
                List<SmithNPC> all = npcManager.getAllNPCs();
                sender.sendMessage("§eActive Smith_AI NPCs: §f" + all.size());
                for (SmithNPC n : all) {
                    Location l = n.getLocation();
                    String loc = l != null ? l.getWorld().getName() + " " + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ() : "unknown";
                    sender.sendMessage("§7- " + n.getName() + " §f(" + loc + ")");
                }
                return true;

            case "teleport":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can use this.");
                    return true;
                }
                Player tpPlayer = (Player) sender;
                SmithNPC tpNpc = nearest(tpPlayer);
                if (tpNpc == null) {
                    sender.sendMessage("§cNo Smith_AI nearby.");
                    return true;
                }
                tpNpc.teleport(tpPlayer.getLocation());
                tpNpc.sendMessage(tpPlayer, "I'm here now.");
                return true;

            case "skin":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can use this.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§eUsage: §f/smithai skin <url>");
                    sender.sendMessage("§7Changing NPC skins requires a player-model renderer. This is a placeholder.");
                    return true;
                }
                Player skinPlayer = (Player) sender;
                SmithNPC skinNpc = nearest(skinPlayer);
                if (skinNpc == null) {
                    sender.sendMessage("§cNo Smith_AI nearby.");
                    return true;
                }
                skinNpc.sendMessage(skinPlayer, "Skin change to " + args[1] + " is not yet implemented (requires player model).");
                return true;

            case "data":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can use this.");
                    return true;
                }
                Player dataPlayer = (Player) sender;
                long rlCount = plugin.getTrainingManager().getRlRecorder().count();
                String rlPath = plugin.getTrainingManager().getRlRecorder().getFilePath();
                java.util.Map<String, Object> summary = plugin.getTrainingManager().getRlRecorder().getSummary();

                sender.sendMessage("§6§l═══ SmithAI Training Data ═══");
                sender.sendMessage("§eRL Data: §f" + rlCount + " events recorded");
                sender.sendMessage("§7File: §f" + rlPath);
                sender.sendMessage("§aScores stored: §f" + plugin.getTrainingManager().getAllScores().size() + " actions");

                // Show training scores
                java.util.Map<String, Integer> allScores = plugin.getTrainingManager().getAllScores();
                if (!allScores.isEmpty()) {
                    sender.sendMessage("");
                    sender.sendMessage("§eAll training scores (reward: +1, punish: -1):");
                    allScores.entrySet().stream()
                        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                        .limit(10)
                        .forEach(e -> {
                            String color = e.getValue() > 0 ? "§a" : (e.getValue() < 0 ? "§c" : "§7");
                            sender.sendMessage("  " + color + e.getKey() + ": §f" + e.getValue());
                        });
                }

                // Show RL summary
                java.util.List<String> topRewarded = (java.util.List<String>) summary.get("top_rewarded");
                java.util.List<String> topPunished = (java.util.List<String>) summary.get("top_punished");
                if (!topRewarded.isEmpty()) {
                    sender.sendMessage("");
                    sender.sendMessage("§aTop rewarded actions:");
                    topRewarded.forEach(a -> sender.sendMessage("  §a✓ §f" + a));
                }
                if (!topPunished.isEmpty()) {
                    sender.sendMessage("§cTop punished actions:");
                    topPunished.forEach(a -> sender.sendMessage("  §c✗ §f" + a));
                }

                // Show recent RL events
                java.util.List<com.smithai.training.RLDataRecorder.RLEvent> recent = plugin.getTrainingManager().getRlRecorder().getRecentEvents(8);
                if (!recent.isEmpty()) {
                    sender.sendMessage("");
                    sender.sendMessage("§eRecent events:");
                    for (com.smithai.training.RLDataRecorder.RLEvent ev : recent) {
                        String color = ev.type.equals("Reward") ? "§a" : "§c";
                        sender.sendMessage("  " + color + ev.type + " §f" + ev.action + " §7(score: " + ev.score + ")");
                    }
                }
                sender.sendMessage("§6§l" + "═".repeat(30));
                return true;

            case "config":
                if (!sender.hasPermission("smithai.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                com.smithai.config.Config cfg = plugin.getPluginConfig();
                sender.sendMessage("§eSmithAI configuration:");
                sender.sendMessage("§7Name: §f" + cfg.getAiName());
                sender.sendMessage("§7External: §f" + (cfg.isExternalEnabled() ? "enabled (" + cfg.getExternalUrl() + ")" : "disabled"));
                sender.sendMessage("§7Local: §f" + (cfg.isLocalEnabled() ? "enabled" : "disabled") + " §7| §7Fallback: §f" + cfg.isLocalFallbackToRules());
                sender.sendMessage("§7Reconnect interval: §f" + cfg.getReconnectInterval() + "s");
                sender.sendMessage("§7Memory: §f" + cfg.getMaxMemoryMessages() + " messages");
                sender.sendMessage("§7Follow distance: §f" + cfg.getFollowDistance());
                sender.sendMessage("§7Skill queue: §f" + cfg.getMaxSkillQueueSize() + " max, " + cfg.getSkillStepDelay() + " tick delay");
                sender.sendMessage("§7Pathfinding: §f" + cfg.getPathfindingMaxDistance() + " max dist, " + cfg.getPathfindingMaxNodes() + " nodes");
                sender.sendMessage("§7Combat: §fretreat at " + (int)(cfg.getCombatRetreatHealth()*100) + "% health, min " + (int)cfg.getCombatMinFood() + " hunger");
                sender.sendMessage("§7Training persist: §f" + cfg.isPersistTraining());
                sender.sendMessage("§7Debug: §f" + (cfg.isDebugEnabled() ? "enabled" : "disabled"));
                sender.sendMessage("§7Skill tiers: §fMini " + cfg.getMiniSkillTier() + " | GPT1 " + cfg.getGpt1SkillTier() + " | GPT2 " + cfg.getGpt2SkillTier());
                return true;

            case "export":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can export data.");
                    return true;
                }
                if (!sender.hasPermission("smithai.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                Player exportPlayer = (Player) sender;
                String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
                java.io.File dataDir = new java.io.File(plugin.getDataFolder(), "export");
                dataDir.mkdirs();
                // Export memory
                java.io.File memFile = new java.io.File(dataDir, "memory_" + ts + ".yml");
                java.io.File trainingFile = new java.io.File(dataDir, "training_" + ts + ".yml");
                java.io.File rlFile = new java.io.File(dataDir, "rl_data_" + ts + ".csv");
                try {
                    plugin.getMemoryManager().exportTo(memFile);
                    plugin.getTrainingManager().exportTo(trainingFile);
                    plugin.getTrainingManager().getRlRecorder().exportTo(rlFile);
                    sender.sendMessage("§aData exported to plugins/SmithAI/export/:");
                    sender.sendMessage("§7- memory_" + ts + ".yml");
                    sender.sendMessage("§7- training_" + ts + ".yml");
                    sender.sendMessage("§7- rl_data_" + ts + ".csv");
                } catch (Exception e) {
                    sender.sendMessage("§cExport failed: " + e.getMessage());
                }
                return true;

            case "help":
                sender.sendMessage("§eSmithAI commands:");
                sender.sendMessage("§7/smithai spawn §f- spawn Smith_AI");
                sender.sendMessage("§7/smithai despawn §f- remove all Smith_AI");
                sender.sendMessage("§7/smithai follow §f- nearby Smith_AI follows you");
                sender.sendMessage("§7/smithai stay §f- nearby Smith_AI stops following");
                sender.sendMessage("§7/smithai goto <x> <y> <z> §f- send Smith_AI to coordinates");
                sender.sendMessage("§7/smithai do <task> §f- plan and execute a task");
                sender.sendMessage("§7/smithai stop §f- cancel all tasks");
                sender.sendMessage("§7/smithai inventory §f- view nearby Smith_AI inventory");
                sender.sendMessage("§7/smithai give <item> [amount] §f- give an item to Smith_AI");
                sender.sendMessage("§7/smithai list §f- list all active Smith_AI NPCs");
                sender.sendMessage("§7/smithai teleport §f- teleport nearby Smith_AI to you");
                sender.sendMessage("§7/smithai train good|bad §f- reward or punish the AI");
                sender.sendMessage("§7/smithai feedback <message> §f- report a specific mistake");
                sender.sendMessage("§7/smithai report <description> §f- open a prefilled GitHub issue");
                sender.sendMessage("§7/smithai memory §f- show recent conversation");
                sender.sendMessage("§7/smithai data §f- show training data stats and RL file location");
                sender.sendMessage("§7/smithai config §f- show current configuration (admin)");
                sender.sendMessage("§7/smithai export §f- export memory/training data (admin)");
                sender.sendMessage("§7/smithai status §f- show active brain/model");
                sender.sendMessage("§7/smithai version §f- show detected server and SmithAI version");
                sender.sendMessage("§7/smithai reload §f- reload configuration (admin)");
                sender.sendMessage("§7/smithai health §f- show subsystem health (admin)");
                return true;

            default:
                // This shouldn't be reached since isKnownCommand handles it, but just in case
                String fallbackQuery = String.join(" ", args);
                String fallback = lookupCommand(fallbackQuery);
                if (fallback != null) {
                    sender.sendMessage("§e" + fallback);
                } else {
                    sender.sendMessage("§eUnknown subcommand. Use /smithai help.");
                }
                return true;
        }
    }

    /**
     * Check if the given word is a known smithai subcommand.
     */
    private boolean isKnownCommand(String word) {
        switch (word) {
            case "spawn": case "despawn": case "follow": case "stay": case "goto": case "do":
            case "stop": case "debug": case "health": case "status": case "model": case "version":
            case "reload": case "train": case "feedback": case "feedback-list": case "report":
            case "reports": case "memory": case "inventory": case "give": case "list":
            case "help": case "teleport": case "skin": case "data": case "config": case "export":
                return true;
            default:
                return false;
        }
    }

    /**
     * Natural language command lookup. Maps phrases like "how do I see your inventory"
     * to command descriptions like "/smithai inventory - view nearby Smith_AI inventory".
     */
    private String lookupCommand(String query) {
        String q = query.toLowerCase().trim();
        if (q.isEmpty()) return null;

        // Remove common filler words at start
        String clean = q.replaceAll("^(what|how|which|where|tell me|can you|does|is there|i want|i need|show me|find|give me|whats|what's|command to|command for|how to|how do i|how can i|way to|way of) +", "");

        // ── Command intent mapping ──
        if (matches(clean, "spawn", "create", "summon", "make new", "bring out", "get a new")) {
            return "/smithai spawn - Spawn a new Smith_AI NPC at your location";
        }
        if (matches(clean, "despawn", "remove", "delete", "destroy", "kill npc", "get rid")) {
            return "/smithai despawn - Remove all Smith_AI NPCs";
        }
        if (matches(clean, "follow", "come with", "follow me", "walk with")) {
            return "/smithai follow - Make nearby Smith_AI follow you";
        }
        if (matches(clean, "stay", "stop follow", "wait", "stay put", "don't move")) {
            return "/smithai stay - Make nearby Smith_AI stop following and stay put";
        }
        if (matches(clean, "goto", "go to", "move to", "send to", "teleport to")) {
            return "/smithai goto <x> <y> <z> - Send Smith_AI to specific coordinates";
        }
        if (matches(clean, "do", "task", "execute", "perform", "make them", "tell them to")) {
            return "/smithai do <task> - Plan and execute a task (e.g. 'mine diamonds', 'build base')";
        }
        if (matches(clean, "stop", "cancel", "halt", "abort", "quit", "cease")) {
            return "/smithai stop - Cancel all queued tasks";
        }
        if (matches(clean, "inventory", "what do you have", "your items", "holding", "carrying", "pov", "your pov", "from your perspective", "what are you holding", "whats in your")) {
            return "/smithai inventory - View nearby Smith_AI's inventory and items";
        }
        if (matches(clean, "give", "hand", "transfer", "give item", "give them")) {
            return "/smithai give <item> [amount] - Give an item to Smith_AI";
        }
        if (matches(clean, "list", "all npcs", "active", "how many", "where are")) {
            return "/smithai list - List all active Smith_AI NPCs";
        }
        if (matches(clean, "teleport", "tp", "bring here", "come here", "bring them")) {
            return "/smithai teleport - Teleport nearby Smith_AI to your location";
        }
        if (matches(clean, "train", "train good", "train bad", "reward", "punish", "teach", "good", "bad", "praise", "scold", "training data")) {
            return "/smithai train good|bad [action] - Reward or punish the AI for a specific action";
        }
        if (matches(clean, "import", "bulk teach", "load training", "from file", "csv")) {
            return "/smithai train import <R|P>,<action>,<score> - Import training data from CSV to teach the AI";
        }
        if (matches(clean, "feedback", "tell what wrong", "report mistake", "complain", "what i think")) {
            return "/smithai feedback <message> - Report a specific mistake the AI made";
        }
        if (matches(clean, "report", "bug", "issue", "github", "problem")) {
            return "/smithai report <description> - Open a prefilled GitHub issue for bugs";
        }
        if (matches(clean, "data", "training stats", "scores", "learning", "what learned", "top actions", "rl data")) {
            return "/smithai data - Show detailed training data: scores, top rewarded/punished actions, recent RL events";
        }
        if (matches(clean, "config", "configuration", "settings", "setup")) {
            return "/smithai config - Show current configuration (admin only)";
        }
        if (matches(clean, "export", "backup", "save data", "download training")) {
            return "/smithai export - Export memory, training data, and RL data to files";
        }
        if (matches(clean, "memory", "remember", "conversation", "chat history", "recent chat", "what we talked")) {
            return "/smithai memory - Show the most recent conversation with Smith_AI";
        }
        if (matches(clean, "status", "active brain", "current model", "what model", "what brain")) {
            return "/smithai status - Show the active brain/model and connection status";
        }
        if (matches(clean, "version", "what version", "whats new", "plugin version")) {
            return "/smithai version - Show detected server version and SmithAI plugin version";
        }
        if (matches(clean, "reload", "refresh", "restart config", "reload config")) {
            return "/smithai reload - Reload the configuration file (admin only)";
        }
        if (matches(clean, "health", "subsystem", "all good", "diagnostics", "system health")) {
            return "/smithai health - Show subsystem health status (admin only)";
        }
        if (matches(clean, "debug", "debug mode", "verbose", "logs")) {
            return "/smithai debug [global] - Toggle debug mode for yourself or globally (admin)";
        }
        if (matches(clean, "model", "switch model", "change brain", "change model", "mini", "gpt", "external")) {
            return "/smithai model <brain> - Switch between Smith-Mini 1.0, SmithGPT 1.0, or SmithGPT 2.0";
        }
        if (matches(clean, "skin", "appearance", "look", "change look", "change skin")) {
            return "/smithai skin <url> - Change the NPC's skin (placeholder)";
        }
        if (matches(clean, "help", "commands", "what can you do", "capabilities", "all commands", "list commands", "usage")) {
            return "/smithai help - Show all available commands";
        }

        return null;
    }

    /**
     * Returns true if any of the keywords appear in the cleaned query.
     */
    private boolean matches(String clean, String... keywords) {
        for (String kw : keywords) {
            if (clean.contains(kw)) return true;
        }
        return false;
    }

    private SmithNPC nearest(Player player) {
        List<SmithNPC> nearby = npcManager.getNearbyNPCs(player.getLocation(), 16);
        return nearby.isEmpty() ? null : nearby.get(0);
    }

    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return value.replace(" ", "%20");
        }
    }
}
