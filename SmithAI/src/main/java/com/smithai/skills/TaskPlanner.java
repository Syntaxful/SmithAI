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

        TASKS.put("nether portal", Arrays.asList(
            "mine_obsidian", "gather_obsidian", "craft_flint_and_steel", "make_obsidian_portal_frame",
            "build_nether_portal", "light_portal", "enter_portal"
        ));
        TASKS.put("make nether portal", TASKS.get("nether portal"));
        TASKS.put("build portal", TASKS.get("nether portal"));
        TASKS.put("portal", TASKS.get("nether portal"));

        TASKS.put("beat the game", Arrays.asList(
            "chop_tree", "gather_wood", "craft_tool", "mine_stone", "gather_stone",
            "craft_stone_tools", "find_iron", "mine_iron", "smelt_iron", "craft_iron_pickaxe",
            "explore_cave", "strip_mine_diamonds", "mine_diamonds", "mine_obsidian", "gather_obsidian",
            "make_obsidian_portal_frame", "build_nether_portal", "light_portal", "enter_portal",
            "explore_nether", "find_fortress", "explore_fortress", "get_blaze_rods", "fight_blaze",
            "craft_brewing_stand", "brew_fire_resistance", "collect_ender_pearls", "craft_eyes_of_ender",
            "locate_stronghold", "travel_to_stronghold", "find_end_portal_room", "fill_end_portal",
            "enter_end_portal", "explore_end", "defeat_ender_dragon", "exit_end"
        ));
        TASKS.put("win", TASKS.get("beat the game"));
        TASKS.put("defeat the dragon", Arrays.asList("enter_end_portal", "explore_end", "defeat_ender_dragon", "exit_end"));
        TASKS.put("end portal", TASKS.get("defeat the dragon"));
        TASKS.put("enter the end", TASKS.get("defeat the dragon"));

        // Base and survival
        TASKS.put("shelter", Arrays.asList("chop_tree", "gather_wood", "craft_tool", "build_shelter", "place_lights", "secure_area"));
        TASKS.put("build shelter", TASKS.get("shelter"));
        TASKS.put("home", Arrays.asList("chop_tree", "gather_wood", "build_house", "place_chest", "place_furnace", "place_lights", "secure_area"));
        TASKS.put("make a house", TASKS.get("home"));
        TASKS.put("build base", Arrays.asList(
            "gather_wood", "gather_stone", "craft_tool", "build_house", "build_wall", "build_chest", "place_furnace",
            "place_crafting_table", "place_lights", "secure_area", "build_defensive_wall", "patrol_area"
        ));
        TASKS.put("secure base", Arrays.asList("place_lights", "secure_area", "build_defensive_wall", "patrol_area", "defend_area"));

        // Farming and food
        TASKS.put("farm", Arrays.asList("prepare_soil", "plant_seeds", "water_crops", "harvest_crops", "replant_crops"));
        TASKS.put("get food", Arrays.asList("hunt_passive_mob", "gather_crop", "cook_food", "eat_food"));
        TASKS.put("cook food", Arrays.asList("gather_meat", "gather_coal", "craft_furnace", "smelt_food", "eat_food"));

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
    }

    public static List<String> plan(String command) {
        String lower = command.toLowerCase().trim();
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
        return "I'll handle that by doing these steps: " + String.join(" → ", steps);
    }
}
