package com.smithai.commands;

import com.smithai.SmithAIPlugin;
import com.smithai.npc.NPCManager;
import com.smithai.npc.SmithNPC;
import com.smithai.skills.TaskPlanner;
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
            sender.sendMessage("§eSmithAI §7v2.0.0 §e- Usage: /smithai <spawn|despawn|follow|stay|do|status|model|reload|train|memory>");
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

            default:
                sender.sendMessage("§eUnknown subcommand. Use /smithai for help.");
                return true;
        }
    }
}
