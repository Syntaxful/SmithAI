package com.smithai.commands;

import com.smithai.SmithAIPlugin;
import com.smithai.health.SubsystemHealth;
import com.smithai.npc.NPCManager;
import com.smithai.npc.SmithNPC;
import com.smithai.skills.TaskPlanner;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
            sender.sendMessage("§eSmithAI §7v2.0.0 §e- Usage: /smithai <spawn|despawn|follow|stay|goto|do|debug|health|status|model|reload|train|feedback|report|reports|memory>");
            return true;
        }

        String sub = args[0].toLowerCase();

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
                sender.sendMessage("§eActive brain: §f" + plugin.getAiManager().getActiveModelName());
                sender.sendMessage("§eExternal connected: §f" + plugin.getAiManager().isExternalConnected());
                return true;

            case "model":
                sender.sendMessage("§eAvailable models:");
                sender.sendMessage("§7- Smith-Mini 1.0 (built-in)");
                sender.sendMessage("§7- SmithGPT 1.0 (7.5GB, external)");
                sender.sendMessage("§7- SmithGPT 2.0 (15GB, external)");
                sender.sendMessage("§eActive: §f" + plugin.getAiManager().getActiveModelName());
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
                    sender.sendMessage("§eUsage: §f/smithai train good §eor §f/smithai train bad");
                    return true;
                }
                Player trainer = (Player) sender;
                List<SmithNPC> nearbyTrain = npcManager.getNearbyNPCs(trainer.getLocation(), 16);
                if (nearbyTrain.isEmpty()) {
                    sender.sendMessage("§cNo Smith_AI nearby.");
                    return true;
                }
                String feedback = args[1].toLowerCase();
                SmithNPC tnpc = nearbyTrain.get(0);
                if (feedback.equals("good")) {
                    tnpc.sendMessage(trainer, "Thanks! I'll remember that as good.");
                    plugin.getTrainingManager().recordGood("general");
                } else if (feedback.equals("bad")) {
                    tnpc.sendMessage(trainer, "Got it. I'll avoid that in the future.");
                    plugin.getTrainingManager().recordBad("general");
                } else {
                    sender.sendMessage("§eUse §fgood §eor §fbad§e.");
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
                body.append("### Details\n");
                body.append("- Server type: ").append(reporter.getServer().getName()).append("\n");
                body.append("- SmithAI version: 2.0.0\n");
                body.append("- Active brain: ").append(plugin.getAiManager().getActiveModelName()).append("\n");
                body.append("- External connected: ").append(plugin.getAiManager().isExternalConnected()).append("\n");
                body.append("- NPC task busy: ").append(plugin.getSkillExecutor().isBusy()).append("\n");

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

            default:
                sender.sendMessage("§eUnknown subcommand. Use /smithai for help.");
                return true;
        }
    }

    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return value.replace(" ", "%20");
        }
    }
}
