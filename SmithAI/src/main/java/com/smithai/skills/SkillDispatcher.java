package com.smithai.skills;

import com.smithai.SmithAIPlugin;
import com.smithai.npc.SmithNPC;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Dispatches broad skill categories to concrete Bukkit actions.
 * This is a starter implementation; primitive executors are added as the project grows.
 */
public class SkillDispatcher {

    private final SmithAIPlugin plugin;

    public SkillDispatcher(SmithAIPlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(SmithNPC npc, String skillName, Map<String, Object> params, Player contextPlayer) {
        String lower = skillName.toLowerCase();

        // Chat / social
        if (lower.startsWith("chat_") || lower.startsWith("say_") || lower.startsWith("ask_") || lower.startsWith("tell_") ||
            lower.startsWith("greet") || lower.startsWith("farewell") || lower.startsWith("thank") || lower.startsWith("apologize") ||
            lower.startsWith("compliment") || lower.startsWith("tease") || lower.startsWith("warn") || lower.startsWith("encourage") ||
            lower.startsWith("praise") || lower.startsWith("scold") || lower.startsWith("narrate") || lower.startsWith("describe") ||
            lower.startsWith("summarize") || lower.startsWith("translate") || lower.startsWith("repeat") || lower.startsWith("joke") ||
            lower.startsWith("explain") || lower.startsWith("suggest") || lower.startsWith("advise") || lower.startsWith("agree") ||
            lower.startsWith("disagree") || lower.startsWith("confirm") || lower.startsWith("deny") || lower.startsWith("offer_help") ||
            lower.startsWith("accept") || lower.startsWith("decline") || lower.startsWith("invite") || lower.startsWith("welcome") ||
            lower.startsWith("bid_farewell") || lower.startsWith("introduce") || lower.startsWith("state_") || lower.startsWith("set_mood") ||
            lower.startsWith("emote") || lower.startsWith("wave") || lower.startsWith("bow") || lower.startsWith("salute") ||
            lower.startsWith("dance") || lower.startsWith("pose") || lower.startsWith("gesture") || lower.startsWith("point") ||
            lower.startsWith("call") || lower.startsWith("whisper") || lower.startsWith("shout") || lower.startsWith("mourn") ||
            lower.startsWith("celebrate") || lower.startsWith("calm") || lower.startsWith("alert") || lower.startsWith("relax") ||
            lower.startsWith("focus") || lower.startsWith("remember") || lower.startsWith("recall") || lower.startsWith("forget") ||
            lower.startsWith("list_memory") || lower.startsWith("report_status") || lower.startsWith("report_location") || lower.startsWith("think") ||
            lower.startsWith("noop") || lower.startsWith("wait")) {
            executeChat(npc, lower, contextPlayer);
            return;
        }

        // Movement
        if (lower.startsWith("follow") || lower.equals("move_to_player") || lower.equals("teleport_to_player")) {
            if (contextPlayer != null) npc.follow(contextPlayer);
            return;
        }
        if (lower.startsWith("stay") || lower.equals("stay")) {
            npc.stay();
            return;
        }
        if (lower.startsWith("teleport_to_spawn") || lower.equals("go_home") || lower.equals("teleport_home")) {
            Location spawn = npc.getLocation() != null ? npc.getLocation().getWorld().getSpawnLocation() : null;
            if (spawn != null) npc.teleport(spawn);
            return;
        }
        if (lower.startsWith("look") || lower.startsWith("turn") || lower.startsWith("inspect") || lower.startsWith("check_")) {
            if (contextPlayer != null) npc.lookAt(contextPlayer.getLocation());
            return;
        }
        if (lower.startsWith("move") || lower.startsWith("jump") || lower.startsWith("sneak") || lower.startsWith("sprint") ||
            lower.startsWith("climb") || lower.startsWith("swim") || lower.startsWith("explore_random") || lower.startsWith("wander") ||
            lower.startsWith("walk") || lower.startsWith("run") || lower.startsWith("turn")) {
            executeMovement(npc, lower, contextPlayer);
            return;
        }

        // Basic interaction
        if (lower.startsWith("use") || lower.startsWith("interact") || lower.startsWith("open") || lower.startsWith("close") ||
            lower.startsWith("equip") || lower.startsWith("select") || lower.startsWith("pick_up") || lower.startsWith("drop") ||
            lower.startsWith("sleep") || lower.startsWith("wake") || lower.startsWith("eat") || lower.startsWith("heal") ||
            lower.startsWith("avoid") || lower.startsWith("retreat") || lower.startsWith("set_home") || lower.startsWith("save_favorite") ||
            lower.startsWith("load_favorite") || lower.startsWith("clear_favorite") || lower.startsWith("set_nickname") || lower.startsWith("get_nickname") ||
            lower.startsWith("begin_task") || lower.startsWith("end_task") || lower.startsWith("pause_task") || lower.startsWith("resume_task") ||
            lower.startsWith("cancel_task") || lower.startsWith("retry_task") || lower.startsWith("skip_step") || lower.startsWith("repeat_step") ||
            lower.startsWith("mark_done") || lower.startsWith("mark_failed") || lower.startsWith("place_torch") || lower.startsWith("strip_mine_safe")) {
            executeInteraction(npc, lower, contextPlayer);
            return;
        }

        // Combat / survival / attack
        if (lower.startsWith("attack") || lower.startsWith("defend") || lower.startsWith("fight_") || lower.startsWith("hunt_") ||
            lower.startsWith("auto_equip") || lower.startsWith("auto_eat") || lower.startsWith("auto_heal") || lower.startsWith("retreat_from_combat") ||
            lower.startsWith("ambush") || lower.startsWith("block") || lower.startsWith("dodge") || lower.startsWith("counter") ||
            lower.startsWith("raid_") || lower.startsWith("siege_") || lower.startsWith("storm_") || lower.startsWith("invade_") ||
            lower.startsWith("fortify_") || lower.startsWith("guard_") || lower.startsWith("patrol_") || lower.startsWith("protect_") ||
            lower.startsWith("suppress_") || lower.startsWith("secure_") || lower.startsWith("track_") || lower.startsWith("stalk_") ||
            lower.startsWith("tame_") || lower.startsWith("befriend") || lower.startsWith("recruit") || lower.startsWith("train_elite")) {
            executeCombat(npc, lower, contextPlayer);
            return;
        }

        // Gather / resource / farming / crafting / building
        if (lower.startsWith("gather_") || lower.startsWith("collect_") || lower.startsWith("harvest_") || lower.startsWith("mine_") ||
            lower.startsWith("chop_") || lower.startsWith("dig_") || lower.startsWith("quarry_") || lower.startsWith("excavate_") ||
            lower.startsWith("farm_") || lower.startsWith("fish") || lower.startsWith("hunt_") || lower.startsWith("shear_") ||
            lower.startsWith("milk_") || lower.startsWith("pick_") || lower.startsWith("forage_") || lower.startsWith("scavenge_") ||
            lower.startsWith("salvage_") || lower.startsWith("loot_") || lower.startsWith("find_") || lower.startsWith("obtain_") ||
            lower.startsWith("craft_") || lower.startsWith("make_") || lower.startsWith("build_") || lower.startsWith("assemble_") ||
            lower.startsWith("forge_") || lower.startsWith("smelt_") || lower.startsWith("brew_") || lower.startsWith("cook_") ||
            lower.startsWith("bake_") || lower.startsWith("repair_") || lower.startsWith("place_") || lower.startsWith("remove_") ||
            lower.startsWith("decorate_") || lower.startsWith("terraform_") || lower.startsWith("light_") || lower.startsWith("secure_") ||
            lower.startsWith("expand_") || lower.startsWith("shrink_") || lower.startsWith("relocate_") || lower.startsWith("plant_") ||
            lower.startsWith("sow_") || lower.startsWith("water_") || lower.startsWith("fertilize_") || lower.startsWith("breed_") ||
            lower.startsWith("feed_") || lower.startsWith("grow_") || lower.startsWith("prune_") || lower.startsWith("replant_") ||
            lower.startsWith("explore_") || lower.startsWith("locate_") || lower.startsWith("scout_") || lower.startsWith("map_") ||
            lower.startsWith("navigate_") || lower.startsWith("search_") || lower.startsWith("survey_") || lower.startsWith("discover_") ||
            lower.startsWith("travel_") || lower.startsWith("venture_") || lower.startsWith("store_") || lower.startsWith("sort_") ||
            lower.startsWith("organize_") || lower.startsWith("deposit_") || lower.startsWith("withdraw_") || lower.startsWith("stack_") ||
            lower.startsWith("transfer_") || lower.startsWith("stock_") || lower.startsWith("manage_") || lower.startsWith("fuel_") ||
            lower.startsWith("configure_") || lower.startsWith("prepare_") || lower.startsWith("use_")) {
            executeComposite(npc, lower, contextPlayer, "I'll work on that task: " + humanize(skillName));
            return;
        }

        // Advanced / progression / strategy
        executeComposite(npc, lower, contextPlayer, "I'll plan and execute that advanced objective: " + humanize(skillName));
    }

    private void executeChat(SmithNPC npc, String skill, Player player) {
        String reply = generateChatReply(skill, player);
        if (player != null) {
            npc.sendMessage(player, reply);
        } else {
            npc.sendMessageToAll(reply);
        }
    }

    private String generateChatReply(String skill, Player player) {
        String name = player != null ? player.getName() : "there";
        if (skill.contains("greet") || skill.contains("hello")) return "Hello, " + name + "!";
        if (skill.contains("farewell") || skill.contains("goodbye")) return "See you later, " + name + "!";
        if (skill.contains("thank")) return "You're welcome!";
        if (skill.contains("apologize") || skill.contains("sorry")) return "No worries.";
        if (skill.contains("joke")) return "Why did the creeper cross the road? To get to the other side... boom!";
        if (skill.contains("warn")) return "Be careful!";
        if (skill.contains("encourage")) return "You can do it!";
        if (skill.contains("praise") || skill.contains("compliment")) return "Great job!";
        if (skill.contains("status")) return "I'm ready to help.";
        if (skill.contains("location")) return "I'm right here.";
        return "Got it.";
    }

    private void executeMovement(SmithNPC npc, String skill, Player player) {
        if (player == null || npc.getLocation() == null) return;
        Location loc = player.getLocation();
        if (skill.contains("look") || skill.contains("turn") || skill.contains("inspect")) {
            npc.lookAt(loc);
        } else if (skill.contains("jump")) {
            Entity entity = npc.getEntity();
            if (entity != null) entity.setVelocity(entity.getVelocity().setY(0.5));
        } else {
            npc.lookAt(loc);
        }
    }

    private void executeInteraction(SmithNPC npc, String skill, Player player) {
        if (skill.startsWith("eat") || skill.startsWith("heal")) {
            Entity entity = npc.getEntity();
            if (entity instanceof LivingEntity) {
                LivingEntity le = (LivingEntity) entity;
                le.setHealth(Math.min(le.getHealth() + 2, le.getMaxHealth()));
            }
            return;
        }
        if (skill.startsWith("equip") || skill.startsWith("select")) {
            // Placeholder: in a full implementation this would select the right hotbar slot.
            return;
        }
        if (skill.startsWith("place_torch") && player != null) {
            Location loc = npc.getLocation();
            if (loc != null) {
                Location place = loc.clone().add(0, -1, 0);
                place.getBlock().setType(Material.TORCH);
            }
            return;
        }
        npc.sendMessage(player, "Working on: " + humanize(skill));
    }

    private void executeCombat(SmithNPC npc, String skill, Player player) {
        npc.sendMessage(player, "Engaging target: " + humanize(skill));
        // Placeholder: attack nearest hostile entity within range.
        Entity entity = npc.getEntity();
        if (entity instanceof LivingEntity) {
            LivingEntity self = (LivingEntity) entity;
            LivingEntity target = findNearestHostile(self, 8);
            if (target != null) {
                self.attack(target);
            }
        }
    }

    private LivingEntity findNearestHostile(LivingEntity self, double radius) {
        LivingEntity nearest = null;
        double best = radius * radius;
        Location loc = self.getLocation();
        for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (e instanceof LivingEntity && e != self && !(e instanceof Player)) {
                double d = e.getLocation().distanceSquared(loc);
                if (d < best) {
                    best = d;
                    nearest = (LivingEntity) e;
                }
            }
        }
        return nearest;
    }

    private void executeComposite(SmithNPC npc, String skill, Player player, String defaultMessage) {
        npc.sendMessage(player, defaultMessage);
    }

    private String humanize(String id) {
        String[] words = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            sb.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) sb.append(word.substring(1).toLowerCase());
            sb.append(' ');
        }
        return sb.toString().trim();
    }
}
