package com.smithai.skills;

import java.util.*;

public class TaskPlanner {

    public static final Map<String, List<String>> TASKS = new HashMap<>();

    static {
        // Core progression: beat the game
        TASKS.put("diamonds", Arrays.asList(
            "chop_tree", "gather_wood", "craft_tool", "mine_stone", "gather_stone",
            "craft_stone_tools", "find_iron", "mine_iron", "smelt_iron", "craft_iron_pickaxe",
            "explore_cave", "strip_mine_diamonds", "mine_diamonds"
        ));
        TASKS.put("get diamonds", TASKS.get("diamonds"));
        TASKS.put("find diamonds", TASKS.get("diamonds"));
        TASKS.put("mine diamonds", TASKS.get("diamonds"));

        // Expanded endgame: nether portal sequence
        TASKS.put("nether portal", Arrays.asList(
            "mine_obsidian", "gather_obsidian", "craft_flint_and_steel", "make_obsidian_portal_frame",
            "build_nether_portal", "light_portal", "enter_portal"
        ));
        TASKS.put("make nether portal", TASKS.get("nether portal"));
        TASKS.put("build portal", TASKS.get("nether portal"));
        TASKS.put("portal", TASKS.get("nether portal"));
        TASKS.put("nether", TASKS.get("nether portal"));

        // Endgame: find fortress -> blaze rods
        TASKS.put("find fortress", Arrays.asList(
            "enter_portal", "explore_nether_wastes", "explore_crimson_forest", "explore_soul_sand_valley",
            "locate_nether_fortress", "approach_fortress", "enter_fortress", "defend_against_blaze",
            "fight_blazes", "collect_blaze_rods"
        ));
        TASKS.put("fortress", TASKS.get("find fortress"));
        TASKS.put("blaze rods", Arrays.asList(
            "enter_portal", "explore_nether", "find_fortress", "fight_blazes", "collect_blaze_rods"
        ));
        TASKS.put("get blaze rods", TASKS.get("blaze rods"));
        TASKS.put("blaze", TASKS.get("blaze rods"));

        // Endgame: blaze powder + ender pearls -> eyes of ender -> stronghold
        TASKS.put("eyes of ender", Arrays.asList(
            "collect_blaze_rods", "craft_blaze_powder", "collect_ender_pearls", "craft_eyes_of_ender"
        ));
        TASKS.put("get eyes of ender", TASKS.get("eyes of ender"));

        TASKS.put("ender pearls", Arrays.asList(
            "locate_endermen", "hunt_endermen", "collect_ender_pearls"
        ));
        TASKS.put("get ender pearls", TASKS.get("ender pearls"));

        // Endgame: locate stronghold -> fill portal -> enter end
        TASKS.put("find stronghold", Arrays.asList(
            "craft_eyes_of_ender", "locate_stronghold", "travel_to_stronghold", "navigate_stronghold",
            "find_end_portal_room", "fill_end_portal", "enter_end_portal"
        ));
        TASKS.put("stronghold", TASKS.get("find stronghold"));
        TASKS.put("end portal", Arrays.asList(
            "craft_eyes_of_ender", "locate_stronghold", "travel_to_stronghold", "find_end_portal_room",
            "fill_end_portal", "enter_end_portal"
        ));
        TASKS.put("enter the end", TASKS.get("end portal"));

        // Dragon fight
        TASKS.put("ender dragon", Arrays.asList(
            "enter_end_portal", "equip_bow", "equip_armor", "destroy_end_crystals",
            "attack_dragon_perched", "dodge_dragon_charge", "destroy_more_crystals",
            "finish_dragon", "collect_dragon_egg", "exit_end"
        ));
        TASKS.put("defeat the dragon", TASKS.get("ender dragon"));
        TASKS.put("kill dragon", TASKS.get("ender dragon"));
        TASKS.put("beat dragon", TASKS.get("ender dragon"));
        TASKS.put("fight dragon", TASKS.get("ender dragon"));

        // Full game beat sequence (expanded)
        TASKS.put("beat the game", Arrays.asList(
            "chop_tree", "gather_wood", "craft_tool", "mine_stone", "gather_stone",
            "craft_stone_tools", "find_iron", "mine_iron", "smelt_iron", "craft_iron_pickaxe",
            "explore_cave", "strip_mine_diamonds", "mine_diamonds",
            "mine_obsidian", "gather_obsidian", "craft_flint_and_steel",
            "make_obsidian_portal_frame", "build_nether_portal", "light_portal", "enter_portal",
            "explore_nether", "find_fortress", "enter_fortress", "fight_blazes", "collect_blaze_rods",
            "craft_blaze_powder", "collect_ender_pearls", "craft_eyes_of_ender",
            "locate_stronghold", "travel_to_stronghold", "find_end_portal_room", "fill_end_portal",
            "enter_end_portal", "equip_bow", "equip_armor", "destroy_end_crystals",
            "attack_dragon_perched", "dodge_dragon_charge", "finish_dragon", "exit_end"
        ));
        TASKS.put("win", TASKS.get("beat the game"));
        TASKS.put("speedrun", Arrays.asList(
            "chop_tree", "craft_wooden_pickaxe", "mine_stone", "craft_stone_pickaxe",
            "find_iron", "mine_iron", "smelt_iron", "craft_iron_pickaxe",
            "explore_cave", "mine_diamonds", "mine_obsidian", "craft_flint_and_steel",
            "build_nether_portal", "light_portal", "enter_portal",
            "locate_fortress_quick", "get_blaze_rods", "collect_ender_pearls",
            "craft_eyes_of_ender", "locate_stronghold", "fill_end_portal",
            "enter_end_portal", "defeat_ender_dragon"
        ));

        // Wither
        TASKS.put("wither", Arrays.asList(
            "enter_nether", "find_wither_skeletons", "kill_wither_skeletons", "collect_wither_skulls",
            "gather_soul_sand", "build_wither_construct", "summon_wither", "fight_wither", "collect_nether_star"
        ));
        TASKS.put("summon wither", TASKS.get("wither"));
        TASKS.put("kill wither", TASKS.get("wither"));

        // Base and survival
        TASKS.put("shelter", Arrays.asList("chop_tree", "gather_wood", "craft_tool", "build_shelter", "place_lights", "secure_area"));
        TASKS.put("build shelter", TASKS.get("shelter"));
        TASKS.put("home", Arrays.asList("chop_tree", "gather_wood", "build_house", "place_chest", "place_furnace", "place_lights", "secure_area"));
        TASKS.put("make a house", TASKS.get("home"));
        TASKS.put("build base", Arrays.asList(
            "gather_wood", "gather_stone", "craft_tool", "build_house", "build_wall", "build_chest", "place_furnace",
            "place_crafting_table", "place_lights", "secure_area", "build_defensive_wall", "patrol_area"
        ));
        TASKS.put("make a base", TASKS.get("build base"));
        TASKS.put("secure base", Arrays.asList("place_lights", "secure_area", "build_defensive_wall", "patrol_area", "defend_area"));

        // Farming and food
        TASKS.put("farm", Arrays.asList("prepare_soil", "plant_seeds", "water_crops", "harvest_crops", "replant_crops"));
        TASKS.put("make a farm", TASKS.get("farm"));
        TASKS.put("get food", Arrays.asList("hunt_passive_mob", "gather_crop", "cook_food", "eat_food"));
        TASKS.put("cook food", Arrays.asList("gather_meat", "gather_coal", "craft_furnace", "smelt_food", "eat_food"));
        TASKS.put("food", TASKS.get("get food"));

        // Combat and defense
        TASKS.put("defend", Arrays.asList("equip_weapon", "equip_armor", "patrol_area", "defend_area", "fight_hostile_mob"));
        TASKS.put("defend area", TASKS.get("defend"));
        TASKS.put("fight", Arrays.asList("equip_weapon", "equip_armor", "fight_hostile_mob", "heal"));
        TASKS.put("kill mobs", TASKS.get("fight"));
        TASKS.put("survive the night", Arrays.asList("place_lights", "secure_area", "equip_weapon", "fight_hostile_mob", "sleep"));

        // Exploration and gathering
        TASKS.put("explore", Arrays.asList("prepare_supplies", "explore_world", "map_area", "scout_location", "gather_resources"));
        TASKS.put("explore cave", Arrays.asList("prepare_supplies", "place_torches", "explore_cave", "mine_resources", "avoid_mob"));
        TASKS.put("find iron", Arrays.asList("explore_cave", "mine_iron", "smelt_iron"));
        TASKS.put("get iron", TASKS.get("find iron"));
        TASKS.put("coal", Arrays.asList("explore_cave", "mine_coal", "gather_coal"));
        TASKS.put("get coal", TASKS.get("coal"));

        // Advanced automation
        TASKS.put("automate", Arrays.asList("gather_resources", "build_autonomous_mine", "build_mob_grinder", "build_crop_farm", "optimize"));
        TASKS.put("build farm", Arrays.asList("gather_wood", "gather_stone", "prepare_soil", "build_fence", "plant_seeds", "water_crops"));
        TASKS.put("trading hall", Arrays.asList("gather_emeralds", "find_village", "build_trading_hall", "recruit_villager", "trade"));

        // Social / roleplay
        TASKS.put("chat", Arrays.asList("greet_player", "ask_status", "tell_joke", "listen"));
        TASKS.put("entertain", Arrays.asList("tell_joke", "dance", "emote", "celebrate"));
        TASKS.put("follow", Arrays.asList("follow_player"));
        TASKS.put("come", Arrays.asList("teleport_to_player"));
        TASKS.put("stay", Arrays.asList("stay"));
        TASKS.put("go home", Arrays.asList("teleport_to_spawn"));
        TASKS.put("spawn", Arrays.asList("teleport_to_spawn"));

        // Common early-game tasks
        TASKS.put("wood", Arrays.asList("chop_tree", "gather_wood", "craft_planks"));
        TASKS.put("get wood", TASKS.get("wood"));
        TASKS.put("stone", Arrays.asList("chop_tree", "craft_wooden_pickaxe", "mine_stone", "gather_stone"));
        TASKS.put("get stone", TASKS.get("stone"));
        TASKS.put("tools", Arrays.asList("chop_tree", "gather_wood", "craft_crafting_table", "craft_wooden_pickaxe", "mine_stone", "craft_stone_tools"));
        TASKS.put("make tools", TASKS.get("tools"));
        TASKS.put("pickaxe", TASKS.get("tools"));
        TASKS.put("torch", Arrays.asList("gather_coal", "craft_stick", "craft_torch"));
        TASKS.put("make torches", TASKS.get("torch"));

        // Biome-specific
        TASKS.put("find village", Arrays.asList("explore_world", "scout_location", "find_village"));
        TASKS.put("village", TASKS.get("find village"));
        TASKS.put("find stronghold", Arrays.asList("craft_eyes_of_ender", "locate_stronghold", "travel_to_stronghold"));
        TASKS.put("stronghold", TASKS.get("find stronghold"));

        // Enchanting
        TASKS.put("enchant", Arrays.asList("mine_diamonds", "gather_obsidian", "craft_enchanting_table",
            "gather_cane", "craft_paper", "craft_book", "craft_bookshelves",
            "place_bookshelves", "gather_lapis", "enchant_item"));
        TASKS.put("enchanting", TASKS.get("enchant"));
        TASKS.put("enchant table", TASKS.get("enchant"));
    }

    public static List<String> plan(String command) {
        String lower = command.toLowerCase().trim();
        // Try exact matches first
        for (Map.Entry<String, List<String>> entry : TASKS.entrySet()) {
            if (lower.equals(entry.getKey())) {
                return new ArrayList<>(entry.getValue());
            }
        }
        // Then substring matches
        for (Map.Entry<String, List<String>> entry : TASKS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return new ArrayList<>(entry.getValue());
            }
        }
        return Collections.emptyList();
    }

    public static String describePlan(String command, List<String> steps) {
        if (steps.isEmpty()) {
            return "I don't know how to do that yet. Try 'get diamonds', 'make nether portal', 'beat the game', 'build base', or 'fight'.";
        }
        return "I'll handle that by doing these steps: " + String.join(" -> ", steps);
    }
}
