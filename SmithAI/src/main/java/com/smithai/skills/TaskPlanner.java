package com.smithai.skills;

import java.util.*;

public class TaskPlanner {

    public static final Map<String, List<String>> TASKS = new HashMap<>();

    /** Counts keyed by step name: how many of a given skill the executor should loop for. */
    public static final Map<String, Map<String, Integer>> COUNTS = new HashMap<>();

    /** Materials (with quantities) the player will need for each high-level task. */
    public static final Map<String, Map<String, Integer>> MATERIALS = new HashMap<>();

    static {
        // Core progression: beat the game
        TASKS.put("diamonds", Arrays.asList(
            "chop_tree", "gather_wood", "craft_tool", "mine_stone", "gather_stone",
            "craft_stone_tools", "find_iron", "mine_iron", "smelt_iron", "craft_iron_pickaxe",
            "explore_cave", "strip_mine_diamonds", "mine_diamonds"
        ));
        COUNTS.put("diamonds", new HashMap<String, Integer>() {{ put("chop_tree", 8); put("mine_stone", 16); put("gather_wood", 32); put("mine_diamonds", 10); }});
        MATERIALS.put("diamonds", new HashMap<String, Integer>() {{ put("oak_log", 8); put("cobblestone", 16); put("iron_ingot", 3); put("diamond", 10); }});
        TASKS.put("get diamonds", TASKS.get("diamonds"));
        TASKS.put("find diamonds", TASKS.get("diamonds"));
        TASKS.put("mine diamonds", TASKS.get("diamonds"));

        TASKS.put("nether portal", Arrays.asList(
            "mine_diamonds", "mine_obsidian", "gather_obsidian", "craft_flint_and_steel",
            "make_obsidian_portal_frame", "build_nether_portal", "light_portal", "enter_portal"
        ));
        COUNTS.put("nether portal", new HashMap<String, Integer>() {{ put("mine_obsidian", 10); put("gather_obsidian", 10); }});
        MATERIALS.put("nether portal", new HashMap<String, Integer>() {{ put("obsidian", 10); put("flint_and_steel", 1); put("iron_pickaxe", 1); }});
        TASKS.put("make nether portal", TASKS.get("nether portal"));
        TASKS.put("build portal", TASKS.get("nether portal"));
        TASKS.put("portal", TASKS.get("nether portal"));
        TASKS.put("nether", TASKS.get("nether portal"));

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
        COUNTS.put("beat the game", new HashMap<String, Integer>() {{
            put("chop_tree", 16); put("gather_wood", 64); put("mine_stone", 64); put("mine_iron", 24);
            put("mine_diamonds", 10); put("mine_obsidian", 14); put("gather_obsidian", 14);
            put("get_blaze_rods", 7); put("collect_ender_pearls", 12);
        }});
        MATERIALS.put("beat the game", new HashMap<String, Integer>() {{
            put("oak_log", 64); put("cobblestone", 128); put("iron_ingot", 24); put("diamond", 10);
            put("ender_pearl", 12); put("blaze_rod", 7); put("netherite_ingot", 4);
            put("obsidian", 14); put("wheat", 32); put("cooked_beef", 16);
        }});
        TASKS.put("win", TASKS.get("beat the game"));
        TASKS.put("defeat the dragon", Arrays.asList("enter_end_portal", "explore_end", "defeat_ender_dragon", "exit_end"));
        TASKS.put("end portal", TASKS.get("defeat the dragon"));
        TASKS.put("enter the end", TASKS.get("defeat the dragon"));

        // Base and survival
        TASKS.put("shelter", Arrays.asList("chop_tree", "gather_wood", "craft_tool", "build_shelter", "place_lights", "secure_area"));
        COUNTS.put("shelter", new HashMap<String, Integer>() {{ put("chop_tree", 16); put("gather_wood", 32); put("place_lights", 8); }});
        MATERIALS.put("shelter", new HashMap<String, Integer>() {{ put("oak_log", 32); put("planks", 64); put("torch", 8); }});
        TASKS.put("build shelter", TASKS.get("shelter"));
        TASKS.put("home", Arrays.asList("chop_tree", "gather_wood", "build_house", "place_chest", "place_furnace", "place_lights", "secure_area"));
        COUNTS.put("home", new HashMap<String, Integer>() {{ put("chop_tree", 32); put("gather_wood", 64); put("place_lights", 16); }});
        MATERIALS.put("home", new HashMap<String, Integer>() {{ put("oak_log", 64); put("cobblestone", 64); put("chest", 1); put("furnace", 1); put("torch", 16); }});
        TASKS.put("make a house", TASKS.get("home"));
        TASKS.put("build base", Arrays.asList(
            "gather_wood", "gather_stone", "craft_tool", "build_house", "build_wall", "build_chest", "place_furnace",
            "place_crafting_table", "place_lights", "secure_area", "build_defensive_wall", "patrol_area"
        ));
        COUNTS.put("build base", new HashMap<String, Integer>() {{ put("gather_wood", 64); put("gather_stone", 256); put("place_lights", 32); }});
        MATERIALS.put("build base", new HashMap<String, Integer>() {{ put("oak_log", 64); put("cobblestone", 256); put("torch", 32); put("chest", 1); put("furnace", 1); }});
        TASKS.put("make a base", TASKS.get("build base"));
        TASKS.put("secure base", Arrays.asList("place_lights", "secure_area", "build_defensive_wall", "patrol_area", "defend_area"));
        COUNTS.put("secure base", new HashMap<String, Integer>() {{ put("place_lights", 32); }});

        // Farming and food
        TASKS.put("farm", Arrays.asList("prepare_soil", "plant_seeds", "water_crops", "harvest_crops", "replant_crops"));
        COUNTS.put("farm", new HashMap<String, Integer>() {{ put("harvest_crops", 32); }});
        MATERIALS.put("farm", new HashMap<String, Integer>() {{ put("wheat_seeds", 1); put("hoe", 1); put("water_bucket", 1); put("oak_fence", 16); }});
        TASKS.put("make a farm", TASKS.get("farm"));
        TASKS.put("get food", Arrays.asList("hunt_passive_mob", "gather_crop", "cook_food", "eat_food"));
        COUNTS.put("get food", new HashMap<String, Integer>() {{ put("hunt_passive_mob", 4); put("cook_food", 4); }});
        MATERIALS.put("get food", new HashMap<String, Integer>() {{ put("cooked_beef", 8); }});
        TASKS.put("cook food", Arrays.asList("gather_meat", "gather_coal", "craft_furnace", "smelt_food", "eat_food"));
        TASKS.put("food", TASKS.get("get food"));

        // Combat and defense
        TASKS.put("defend", Arrays.asList("equip_weapon", "equip_armor", "patrol_area", "defend_area", "fight_hostile_mob"));
        TASKS.put("defend area", TASKS.get("defend"));
        TASKS.put("fight", Arrays.asList("equip_weapon", "equip_armor", "fight_hostile_mob", "heal"));
        COUNTS.put("fight", new HashMap<String, Integer>() {{ put("fight_hostile_mob", 4); }});
        TASKS.put("kill mobs", TASKS.get("fight"));
        TASKS.put("survive the night", Arrays.asList("place_lights", "secure_area", "equip_weapon", "fight_hostile_mob", "sleep"));
        COUNTS.put("survive the night", new HashMap<String, Integer>() {{ put("place_lights", 16); put("fight_hostile_mob", 4); }});

        // Exploration and gathering
        TASKS.put("explore", Arrays.asList("prepare_supplies", "explore_world", "map_area", "scout_location", "gather_resources"));
        TASKS.put("explore cave", Arrays.asList("prepare_supplies", "place_torches", "explore_cave", "mine_resources", "avoid_mob"));
        COUNTS.put("explore cave", new HashMap<String, Integer>() {{ put("place_torches", 16); }});
        TASKS.put("find iron", Arrays.asList("explore_cave", "mine_iron", "smelt_iron"));
        COUNTS.put("find iron", new HashMap<String, Integer>() {{ put("mine_iron", 12); }});
        MATERIALS.put("find iron", new HashMap<String, Integer>() {{ put("iron_ingot", 12); }});
        TASKS.put("get iron", TASKS.get("find iron"));
        TASKS.put("coal", Arrays.asList("explore_cave", "mine_coal", "gather_coal"));
        COUNTS.put("coal", new HashMap<String, Integer>() {{ put("mine_coal", 16); }});
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
        COUNTS.put("wood", new HashMap<String, Integer>() {{ put("chop_tree", 16); put("gather_wood", 32); }});
        MATERIALS.put("wood", new HashMap<String, Integer>() {{ put("oak_log", 16); put("planks", 32); }});
        TASKS.put("get wood", TASKS.get("wood"));
        TASKS.put("stone", Arrays.asList("chop_tree", "craft_wooden_pickaxe", "mine_stone", "gather_stone"));
        COUNTS.put("stone", new HashMap<String, Integer>() {{ put("mine_stone", 32); }});
        MATERIALS.put("stone", new HashMap<String, Integer>() {{ put("oak_log", 4); put("cobblestone", 32); }});
        TASKS.put("get stone", TASKS.get("stone"));
        TASKS.put("tools", Arrays.asList("chop_tree", "gather_wood", "craft_crafting_table", "craft_wooden_pickaxe", "mine_stone", "craft_stone_tools"));
        COUNTS.put("tools", new HashMap<String, Integer>() {{ put("mine_stone", 8); }});
        TASKS.put("make tools", TASKS.get("tools"));
        TASKS.put("pickaxe", TASKS.get("tools"));
        TASKS.put("torch", Arrays.asList("gather_coal", "craft_stick", "craft_torch"));
        COUNTS.put("torch", new HashMap<String, Integer>() {{ put("craft_torch", 32); }});
        MATERIALS.put("torch", new HashMap<String, Integer>() {{ put("coal", 4); put("stick", 4); put("torch", 32); }});
        TASKS.put("make torches", TASKS.get("torch"));

        // Biome-specific
        TASKS.put("find village", Arrays.asList("explore_world", "scout_location", "find_village"));
        TASKS.put("village", TASKS.get("find village"));
        TASKS.put("find fortress", Arrays.asList("enter_portal", "explore_nether", "find_fortress"));
        TASKS.put("fortress", TASKS.get("find fortress"));
        TASKS.put("find stronghold", Arrays.asList("craft_eyes_of_ender", "locate_stronghold", "travel_to_stronghold"));
        COUNTS.put("find stronghold", new HashMap<String, Integer>() {{ put("craft_eyes_of_ender", 12); }});
        MATERIALS.put("find stronghold", new HashMap<String, Integer>() {{ put("ender_pearl", 12); put("blaze_powder", 12); }});
        TASKS.put("stronghold", TASKS.get("find stronghold"));
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
        // Dynamic "give me/get me N of X" plan
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("\\b(?:give|get|fetch|bring)\\s+(?:me\\s+)?(\\d+)\\s+(?:of\\s+)?([a-z_][a-z0-9_]*)")
            .matcher(lower);
        if (m.find()) {
            int qty = Integer.parseInt(m.group(1));
            String item = m.group(2);
            List<String> plan = new ArrayList<>();
            plan.add("gather_item:" + item + ":" + qty);
            return plan;
        }
        return Collections.emptyList();
    }

    /** Same as {@link #plan(String)} but also returns the contents key used for that plan. */
    public static String matchingKey(String command) {
        String lower = command.toLowerCase().trim();
        for (Map.Entry<String, List<String>> entry : TASKS.entrySet()) {
            if (lower.equals(entry.getKey()) || lower.contains(entry.getKey())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static Map<String, Integer> countsFor(String key) {
        return COUNTS.getOrDefault(key, Collections.emptyMap());
    }

    public static Map<String, Integer> materialsFor(String key) {
        return MATERIALS.getOrDefault(key, Collections.emptyMap());
    }

    public static String describePlan(String command, List<String> steps) {
        if (steps.isEmpty()) {
            return "I don't know how to do that yet. Try 'get diamonds', 'make nether portal', 'beat the game', 'build base', or 'fight'.";
        }
        return "I'll handle that by doing these steps: " + String.join(" -> ", steps);
    }

    /** Compose a human-friendly list of materials needed for the recognized task key. */
    public static String describeMaterials(String key) {
        Map<String, Integer> materials = materialsFor(key);
        if (materials.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : materials.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.getValue()).append(' ').append(e.getKey().replace('_', ' '));
        }
        return sb.toString();
    }
}
