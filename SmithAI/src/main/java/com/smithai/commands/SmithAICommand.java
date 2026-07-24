package com.smithai.commands;

import com.smithai.SmithAIPlugin;
import com.smithai.config.Config;
import com.smithai.health.SubsystemHealth;
import com.smithai.npc.NPCInventory;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
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
            sender.sendMessage("§eSmithAI §7v2.0.0 §e- Usage: /smithai <spawn|despawn|follow|stay|goto|do|ask|tasks|clear|info|equip|unequip|debug|health|status|model|version|reload|train|feedback|report|reports|memory|inventory|give|list|help|teleport|skin|config|export>");
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
                if (!plan.isEmpty()) {
                    plugin.getSkillExecutor().queuePlan(dnpc, plan, doer);
                    plugin.getTrainingManager().recordGood("task:" + task);
                } else {
                    dnpc.sendMessage(doer, "I don't know how to do that yet.");
                }
                return true;

            case "ask":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can use this.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§eUsage: §f/smithai ask <question>");
                    return true;
                }
                Player asker = (Player) sender;
                List<SmithNPC> nearbyAsk = npcManager.getNearbyNPCs(asker.getLocation(), 16);
                if (nearbyAsk.isEmpty()) {
                    sender.sendMessage("§cNo Smith_AI nearby.");
                    return true;
                }
                SmithNPC askNpc = nearbyAsk.get(0);
                String question = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                plugin.getAiManager().getResponse(asker, question, plugin.getMemoryManager().getConversation(askNpc.getId()), null)
                    .thenAccept(reply -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                        plugin.getMemoryManager().getConversation(askNpc.getId()).addMessage("assistant", reply);
                        askNpc.sendMessage(asker, reply);
                    }));
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
            case "clear":
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

            case "tasks":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can use this.");
                    return true;
                }
                Player taskPlayer = (Player) sender;
                List<String> queued = plugin.getSkillExecutor().getQueuedSkills();
                if (queued.isEmpty()) {
                    sender.sendMessage("§eNo active tasks.");
                } else {
                    sender.sendMessage("§eActive tasks for " + plugin.getPluginConfig().getAiName() + ":");
                    queued.forEach(s -> sender.sendMessage("§7- §f" + s));
                }
                return true;

            case "info":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can use this.");
                    return true;
                }
                Player infoPlayer = (Player) sender;
                List<SmithNPC> nearbyInfo = npcManager.getNearbyNPCs(infoPlayer.getLocation(), 16);
                if (nearbyInfo.isEmpty()) {
                    sender.sendMessage("§cNo Smith_AI nearby.");
                    return true;
                }
                SmithNPC infoNpc = nearbyInfo.get(0);
                sender.sendMessage("§e" + infoNpc.getName() + " info:");
                Location infoLoc = infoNpc.getLocation();
                sender.sendMessage("§7Location: §f" + (infoLoc != null ? infoLoc.getWorld().getName() + " " + infoLoc.getBlockX() + ", " + infoLoc.getBlockY() + ", " + infoLoc.getBlockZ() : "unknown"));
                sender.sendMessage("§7Health: §f" + String.format("%.1f", infoNpc.getHealth()) + " / " + String.format("%.1f", infoNpc.getMaxHealth()));
                sender.sendMessage("§7Food: §f" + infoNpc.getFoodLevel());
                sender.sendMessage("§7Tasks: §f" + plugin.getSkillExecutor().getQueueSize());
                sender.sendMessage("§7Brain: §f" + plugin.getAiManager().getActiveModelName());
                return true;

            case "equip":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can use this.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("§eUsage: §f/smithai equip <slot> <item> [amount]");
                    sender.sendMessage("§7Slots: §fhelmet, chestplate, leggings, boots, mainhand, offhand");
                    return true;
                }
                Player equipPlayer = (Player) sender;
                List<SmithNPC> nearbyEquip = npcManager.getNearbyNPCs(equipPlayer.getLocation(), 16);
                if (nearbyEquip.isEmpty()) {
                    sender.sendMessage("§cNo Smith_AI nearby.");
                    return true;
                }
                SmithNPC equipNpc = nearbyEquip.get(0);
                if (!equipNpc.hasInventory()) {
                    sender.sendMessage("§cThis Smith_AI has no inventory.");
                    return true;
                }
                String slot = args[1].toLowerCase();
                Material equipMat = Material.matchMaterial(args[2]);
                if (equipMat == null || equipMat == Material.AIR) {
                    sender.sendMessage("§cUnknown item: §f" + args[2]);
                    return true;
                }
                int equipAmount = 1;
                if (args.length >= 4) {
                    try { equipAmount = Integer.parseInt(args[3]); } catch (NumberFormatException e) {
                        sender.sendMessage("§cAmount must be a number.");
                        return true;
                    }
                }
                ItemStack equipItem = new ItemStack(equipMat, Math.max(1, equipAmount));
                NPCInventory equipInv = equipNpc.getInventory();
                switch (slot) {
                    case "helmet": equipInv.setArmor(equipItem, equipInv.getChestplate(), equipInv.getLeggings(), equipInv.getBoots()); break;
                    case "chestplate": equipInv.setArmor(equipInv.getHelmet(), equipItem, equipInv.getLeggings(), equipInv.getBoots()); break;
                    case "leggings": equipInv.setArmor(equipInv.getHelmet(), equipInv.getChestplate(), equipItem, equipInv.getBoots()); break;
                    case "boots": equipInv.setArmor(equipInv.getHelmet(), equipInv.getChestplate(), equipInv.getLeggings(), equipItem); break;
                    case "mainhand": equipInv.setMainHand(equipItem); break;
                    case "offhand": equipInv.setOffHand(equipItem); break;
                    default:
                        sender.sendMessage("§cUnknown slot. Use: helmet, chestplate, leggings, boots, mainhand, offhand");
                        return true;
                }
                equipNpc.sendMessage(equipPlayer, "Equipped " + slot + " with " + equipMat.name().toLowerCase().replace("_", " ") + ".");
                return true;

            case "unequip":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can use this.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§eUsage: §f/smithai unequip <slot>");
                    sender.sendMessage("§7Slots: §fhelmet, chestplate, leggings, boots, mainhand, offhand, all");
                    return true;
                }
                Player unequipPlayer = (Player) sender;
                List<SmithNPC> nearbyUnequip = npcManager.getNearbyNPCs(unequipPlayer.getLocation(), 16);
                if (nearbyUnequip.isEmpty()) {
                    sender.sendMessage("§cNo Smith_AI nearby.");
                    return true;
                }
                SmithNPC unequipNpc = nearbyUnequip.get(0);
                if (!unequipNpc.hasInventory()) {
                    sender.sendMessage("§cThis Smith_AI has no inventory.");
                    return true;
                }
                NPCInventory unequipInv = unequipNpc.getInventory();
                String unequipSlot = args[1].toLowerCase();
                switch (unequipSlot) {
                    case "helmet": unequipInv.setArmor(null, unequipInv.getChestplate(), unequipInv.getLeggings(), unequipInv.getBoots()); break;
                    case "chestplate": unequipInv.setArmor(unequipInv.getHelmet(), null, unequipInv.getLeggings(), unequipInv.getBoots()); break;
                    case "leggings": unequipInv.setArmor(unequipInv.getHelmet(), unequipInv.getChestplate(), null, unequipInv.getBoots()); break;
                    case "boots": unequipInv.setArmor(unequipInv.getHelmet(), unequipInv.getChestplate(), unequipInv.getLeggings(), null); break;
                    case "mainhand": unequipInv.setMainHand(null); break;
                    case "offhand": unequipInv.setOffHand(null); break;
                    case "all":
                        unequipInv.setArmor(null, null, null, null);
                        unequipInv.setMainHand(null);
                        unequipInv.setOffHand(null);
                        break;
                    default:
                        sender.sendMessage("§cUnknown slot. Use: helmet, chestplate, leggings, boots, mainhand, offhand, all");
                        return true;
                }
                unequipNpc.sendMessage(unequipPlayer, "Unequipped " + unequipSlot + ".");
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
                Inventory inv = invNpc.getBukkitInventory();
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
                Inventory npcInv = giveNpc.getBukkitInventory();
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

            case "config":
                if (!sender.hasPermission("smithai.admin")) {
                    sender.sendMessage("§cYou need §fsmithai.admin§c to view config.");
                    return true;
                }
                Config cfg = plugin.getPluginConfig();
                sender.sendMessage("§eSmithAI configuration:");
                sender.sendMessage("§7AI name: §f" + cfg.getAiName());
                sender.sendMessage("§7External brain: §f" + (cfg.isExternalEnabled() ? "enabled" : "disabled") + " @ " + cfg.getExternalUrl());
                sender.sendMessage("§7External model: §f" + cfg.getExternalModel());
                sender.sendMessage("§7Local brain: §f" + (cfg.isLocalEnabled() ? "enabled" : "disabled") + " @ " + cfg.getLocalModelPath());
                sender.sendMessage("§7Local fallback: §f" + cfg.isLocalFallbackToRules());
                sender.sendMessage("§7Skill tiers: §fmini=" + cfg.getMiniSkillTier() + " gpt1=" + cfg.getGpt1SkillTier() + " gpt2=" + cfg.getGpt2SkillTier());
                sender.sendMessage("§7Follow distance: §f" + cfg.getFollowDistance() + " §7pathfinder timeout: §f" + cfg.getPathfinderTimeout());
                sender.sendMessage("§7Pathfinding: §fmaxDistance=" + cfg.getPathfindingMaxDistance() + " maxNodes=" + cfg.getPathfindingMaxNodes() + " tickRate=" + cfg.getPathfindingTickRate());
                sender.sendMessage("§7Combat: §fretreatHealth=" + cfg.getCombatRetreatHealth() + " minFood=" + cfg.getCombatMinFood());
                sender.sendMessage("§7Debug: §f" + cfg.isDebugEnabled() + " §7bStats: §f" + cfg.isBstatsEnabled());
                return true;

            case "export":
                if (!sender.hasPermission("smithai.admin")) {
                    sender.sendMessage("§cYou need §fsmithai.admin§c to export data.");
                    return true;
                }
                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                File exportDir = new File(plugin.getDataFolder(), "exports/export-" + timestamp);
                if (!exportDir.exists() && !exportDir.mkdirs()) {
                    sender.sendMessage("§cFailed to create export directory.");
                    return true;
                }
                plugin.getMemoryManager().saveAll();
                plugin.getTrainingManager().save();
                try {
                    File trainingFile = new File(plugin.getDataFolder(), "training.yml");
                    if (trainingFile.exists()) Files.copy(trainingFile.toPath(), new File(exportDir, "training.yml").toPath());
                    File memoryDir = new File(plugin.getDataFolder(), "memory");
                    if (memoryDir.exists()) {
                        File destMemoryDir = new File(exportDir, "memory");
                        destMemoryDir.mkdirs();
                        for (File f : memoryDir.listFiles()) {
                            if (f.isFile()) Files.copy(f.toPath(), new File(destMemoryDir, f.getName()).toPath());
                        }
                    }
                    sender.sendMessage("§eExported memory and training data to: §f" + exportDir.getPath());
                } catch (IOException e) {
                    sender.sendMessage("§cExport failed: §f" + e.getMessage());
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
                sender.sendMessage("§7/smithai config §f- show current configuration (admin)");
                sender.sendMessage("§7/smithai export §f- export memory and training data (admin)");
                sender.sendMessage("§7/smithai train good|bad §f- reward or punish the AI");
                sender.sendMessage("§7/smithai feedback <message> §f- report a specific mistake");
                sender.sendMessage("§7/smithai report <description> §f- open a prefilled GitHub issue");
                sender.sendMessage("§7/smithai memory §f- show recent conversation");
                sender.sendMessage("§7/smithai status §f- show active brain/model");
                sender.sendMessage("§7/smithai version §f- show detected server and SmithAI version");
                sender.sendMessage("§7/smithai reload §f- reload configuration (admin)");
                sender.sendMessage("§7/smithai health §f- show subsystem health (admin)");
                return true;

            default:
                sender.sendMessage("§eUnknown subcommand. Use /smithai help.");
                return true;
        }
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
