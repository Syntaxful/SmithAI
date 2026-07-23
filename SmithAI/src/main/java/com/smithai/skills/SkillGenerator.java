package com.smithai.skills;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Generates a broad 9000-core-skill library on first run.
 * Split by model tier: 900 Smith-Mini, 1800 SmithGPT 1.0, 6300 SmithGPT 2.0.
 * The source JAR stays small because the skills are generated at runtime.
 * This class does not use Bukkit APIs so it can be tested standalone.
 */
public class SkillGenerator {

    private static final int TARGET_MINI = 900;
    private static final int TARGET_GPT1 = 1800;
    private static final int TARGET_GPT2 = 6300;

    private static final String[] MINI_VERBS = {
        "greet", "farewell", "thank", "apologize", "confirm", "deny", "ask", "answer", "joke", "compliment",
        "tease", "warn", "encourage", "praise", "scold", "narrate", "describe", "summarize", "translate", "repeat",
        "remember", "recall", "forget", "list_memory", "report_status", "wait", "noop", "think", "suggest", "advise",
        "follow", "stay", "move", "turn", "look", "jump", "sneak", "sprint", "swim", "climb",
        "inspect", "use", "equip", "drop", "pick_up", "interact", "open", "close", "sleep", "wake",
        "eat", "heal", "avoid", "retreat", "explore", "set_home", "go_home", "teleport_to_player", "teleport_to_spawn", "report_location",
        "wave", "bow", "salute", "dance", "emote", "pose", "gesture", "point", "call", "whisper",
        "set_mood", "focus", "relax", "alert", "calm", "celebrate", "mourn", "agree", "disagree", "offer_help",
        "decline", "accept", "invite", "welcome", "bid_farewell", "introduce", "explain_self", "state_name", "state_role", "state_model",
        "count", "compare", "estimate", "predict", "plan_simple", "schedule", "prioritize", "organize", "prepare", "cleanup",
        "check_inventory", "check_health", "check_hunger", "check_armor", "check_tool", "check_time", "check_weather", "check_biome", "check_light", "check_mobs",
        "select_tool", "select_food", "select_weapon", "select_block", "select_item", "save_favorite", "load_favorite", "clear_favorite", "set_nickname", "get_nickname",
        "begin_task", "end_task", "pause_task", "resume_task", "cancel_task", "retry_task", "skip_step", "repeat_step", "mark_done", "mark_failed",
        "say_hello", "say_goodbye", "say_yes", "say_no", "say_maybe", "say_please", "say_thanks", "say_sorry", "say_help", "say_ready",
        "ask_name", "ask_status", "ask_location", "ask_goal", "ask_opinion", "ask_preference", "ask_permission", "ask_for_help", "ask_for_item", "ask_for_direction",
        "tell_story", "tell_joke", "tell_fact", "tell_tip", "tell_warning", "tell_progress", "tell_plan", "tell_result", "tell_error", "tell_success",
        "recall_player", "recall_place", "recall_item", "recall_event", "recall_task", "recall_preference", "recall_order", "recall_joke", "recall_fact", "recall_tip",
        "focus_attention", "ignore_distraction", "reset_attention", "track_player", "track_mob", "track_item", "track_location", "track_time", "track_weather", "track_task"
    };

    private static final String[] MINI_TOPICS = {
        "player", "world", "weather", "time", "biome", "mob", "block", "item", "tool", "weapon",
        "armor", "food", "ore", "tree", "cave", "home", "spawn", "task", "goal", "plan",
        "memory", "skill", "model", "self", "team", "friend", "enemy", "danger", "safety", "progress"
    };

    private static final String[] GPT1_VERBS = {
        "gather", "collect", "harvest", "mine", "chop", "dig", "quarry", "excavate", "farm", "fish",
        "hunt", "shear", "milk", "pick", "forage", "scavenge", "salvage", "loot", "find", "obtain",
        "craft", "make", "build", "assemble", "forge", "smelt", "brew", "cook", "bake", "repair",
        "place", "remove", "decorate", "terraform", "light", "secure", "expand", "shrink", "move", "relocate",
        "fight", "attack", "defend", "hunt", "ambush", "retreat", "block", "dodge", "counter", "heal",
        "plant", "sow", "water", "fertilize", "breed", "feed", "tame", "grow", "prune", "replant",
        "explore", "locate", "scout", "map", "navigate", "search", "survey", "discover", "travel", "venture",
        "store", "sort", "organize", "deposit", "withdraw", "stack", "transfer", "trade", "stock", "manage",
        "use_tool", "use_weapon", "use_item", "use_block", "consume", "activate", "deactivate", "toggle", "configure", "prepare"
    };

    private static final String[] GPT1_OBJECTS = {
        "wood", "stone", "coal", "iron", "copper", "gold", "diamond", "emerald", "redstone", "lapis",
        "obsidian", "dirt", "sand", "gravel", "clay", "wool", "leather", "meat", "fish", "crop",
        "flower", "mushroom", "berry", "honey", "sapling", "seed", "log", "plank", "stick", "cobblestone",
        "tool", "weapon", "armor", "food", "block", "item", "potion", "torch", "ladder", "bed",
        "crafting_table", "furnace", "chest", "door", "fence", "wall", "bridge", "shelter", "house", "room",
        "hostile_mob", "passive_mob", "neutral_mob", "boss", "prey", "predator", "threat", "target", "enemy", "intruder",
        "wheat", "carrot", "potato", "beetroot", "melon", "pumpkin", "cocoa", "cane", "cactus", "bamboo",
        "cow", "pig", "chicken", "sheep", "rabbit", "horse", "wolf", "cat", "villager", "trader",
        "cave", "village", "forest", "mountain", "river", "ocean", "desert", "jungle", "plains", "hill"
    };

    private static final String[] GPT2_VERBS = {
        "conquer", "master", "complete", "achieve", "unlock", "traverse", "dominate", "overcome", "survive", "thrive",
        "optimize", "automate", "orchestrate", "engineer", "architect", "design", "strategize", "coordinate", "delegate", "execute",
        "raid", "siege", "storm", "invade", "defend", "fortify", "guard", "escort", "protect", "patrol",
        "infiltrate", "escape", "evade", "lure", "trap", "ambush", "flank", "surround", "suppress", "secure",
        "summon", "banish", "channel", "invoke", "enchant", "imbue", "transmute", "conjure", "ward", "bless",
        "subjugate", "annihilate", "eradicate", "pacify", "stabilize", "mobilize", "neutralize", "synthesize", "weaponize", "industrialize",
        "reconstruct", "repurpose", "replicate", "reverse", "reinforce", "regenerate", "rejuvenate", "resurrect", "recruit", "retain",
        "pioneer", "chart", "colonize", "establish", "install", "commission", "operate", "regulate", "monetize", "capitalize",
        "corrupt", "purify", "sacrifice", "ritualize", "mythologize", "immortalize", "memorialize", "canonize", "demonize", "sanctify"
    };

    private static final String[] GPT2_OBJECTS = {
        "nether", "end", "stronghold", "fortress", "bastion", "end_city", "dragon", "wither", "warden", "ancient_city",
        "dimensional_gateway", "outer_islands", "end_portal", "nether_portal", "blaze_spawner", "mob_grinder", "iron_farm", "trading_hall", "villager_breeder", "crop_complex",
        "autonomous_mine", "self_repairing_base", "redstone_computer", "storage_network", "crafting_pipeline", "enchanting_station", "brewing_pipeline", "smelting_array", "defense_system", "warning_network",
        "expedition", "campaign", "quest_line", "world_tour", "mapping_project", "archaeological_dig", "underwater_ruin", "desert_temple", "jungle_temple", "ocean_monument",
        "diamond_layer", "ancient_debris", "netherite_forge", "elytra_course", "shulker_army", "beacon_pyramid", "conduit_temple", "respawn_anchor", "hoglin_stables", "strider_ranch",
        "economy", "market", "guild", "kingdom", "federation", "alliance", "empire", "colony", "outpost", "capital",
        "army", "navy", "air_force", "special_forces", "militia", "mercenaries", "champions", "heroes", "legends", "avatars",
        "elemental", "spirit", "golem", "construct", "automation", "drone", "agent", "swarm", "hive", "collective",
        "advancement_tree", "trophy_hall", "museum", "library", "archive", "monument", "memorial", "wonder", "masterpiece", "legendary_item",
        "rift", "void", "abyss", "nexus", "sanctum", "citadel", "labyrinth", "maze", "arena", "battlefield",
        "reactor", "foundry", "manufactory", "workshop", "laboratory", "observatory", "greenhouse", "aquarium", "aviary", "menagerie",
        "portal_network", "road_network", "canal_system", "railway", "skyway", "subway", "highway", "checkpoint", "watchtower", "beacon_chain",
        "trade_route", "supply_line", "caravan", "fleet", "armada", "squadron", "legion", "phalanx", "cavalry", "siege_train",
        "reputation", "influence", "diplomacy", "espionage", "propaganda", "culture", "religion", "science", "magic", "technology"
    };

    public static void generateIfMissing(File file) {
        if (file.exists()) return;
        generate(file);
    }

    public static void generate(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        Set<String> used = new HashSet<>();
        int miniCount = 0;
        int gpt1Count = 0;
        int gpt2Count = 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("# SmithAI Core Skills\n");
            writer.write("# Tiers: mini (900), gpt1 (1800), gpt2 (6300)\n");
            writer.write("# Higher-tier brains can use all lower-tier skills.\n\n");

            // Tier 1: Mini
            for (String verb : MINI_VERBS) {
                if (miniCount >= TARGET_MINI) break;
                if (used.add(verb)) {
                    writeSkill(writer, verb, humanize(verb), "primitive", "mini");
                    miniCount++;
                }
            }
            for (String verb : MINI_VERBS) {
                for (String topic : MINI_TOPICS) {
                    if (miniCount >= TARGET_MINI) break;
                    String id = verb + "_" + topic;
                    if (used.add(id)) {
                        writeSkill(writer, id, humanize(id), "primitive", "mini");
                        miniCount++;
                    }
                }
            }

            // Tier 2: GPT-1
            for (String verb : GPT1_VERBS) {
                for (String obj : GPT1_OBJECTS) {
                    if (gpt1Count >= TARGET_GPT1) break;
                    String id = verb + "_" + obj;
                    if (used.add(id)) {
                        writeSkill(writer, id, humanize(id), "composite", "gpt1");
                        gpt1Count++;
                    }
                }
            }

            // Tier 3: GPT-2
            for (String verb : GPT2_VERBS) {
                for (String obj : GPT2_OBJECTS) {
                    if (gpt2Count >= TARGET_GPT2) break;
                    String id = verb + "_" + obj;
                    if (used.add(id)) {
                        writeSkill(writer, id, humanize(id), "composite", "gpt2");
                        gpt2Count++;
                    }
                }
            }

            // Fallbacks to ensure exact counts
            int i = 1;
            while (miniCount < TARGET_MINI) {
                String id = "mini_action_" + i++;
                if (used.add(id)) {
                    writeSkill(writer, id, humanize(id), "primitive", "mini");
                    miniCount++;
                }
            }
            i = 1;
            while (gpt1Count < TARGET_GPT1) {
                String id = "gpt1_task_" + i++;
                if (used.add(id)) {
                    writeSkill(writer, id, humanize(id), "composite", "gpt1");
                    gpt1Count++;
                }
            }
            i = 1;
            while (gpt2Count < TARGET_GPT2) {
                String id = "gpt2_objective_" + i++;
                if (used.add(id)) {
                    writeSkill(writer, id, humanize(id), "composite", "gpt2");
                    gpt2Count++;
                }
            }

            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate skills file: " + e.getMessage(), e);
        }
    }

    private static void writeSkill(BufferedWriter writer, String id, String description, String type, String tier) throws IOException {
        writer.write(id + ":\n");
        writer.write("  description: \"" + description + "\"\n");
        writer.write("  type: " + type + "\n");
        writer.write("  tier: " + tier + "\n");
        writer.write("  parameters: {}\n");
    }

    private static String humanize(String id) {
        String[] words = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            sb.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                sb.append(word.substring(1).toLowerCase());
            }
            sb.append(' ');
        }
        return sb.toString().trim();
    }

    public static void main(String[] args) {
        File out = new File(args.length > 0 ? args[0] : "skills-generated.yml");
        generate(out);
        System.out.println("Generated skills file: " + out.getAbsolutePath());
    }
}
