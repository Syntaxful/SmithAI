package com.smithai.skills;

import com.smithai.SmithAIPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class SkillRegistry {

    private final SmithAIPlugin plugin;
    private final Map<String, SkillDefinition> skills = new HashMap<>();
    private boolean generated = false;

    private static final Map<String, Integer> TIER_ORDER = new HashMap<>();
    static { TIER_ORDER.put("mini", 1); TIER_ORDER.put("gpt1", 2); TIER_ORDER.put("gpt2", 3); }

    private static final String[] MINI_VERBS = {
        "greet","farewell","thank","apologize","confirm","deny","ask","answer","joke","compliment",
        "tease","warn","encourage","praise","scold","narrate","describe","summarize","translate","repeat",
        "remember","recall","forget","list_memory","report_status","wait","noop","think","suggest","advise",
        "follow","stay","move","turn","look","jump","sneak","sprint","swim","climb",
        "inspect","use","equip","drop","pick_up","interact","open","close","eat","heal",
        "avoid","retreat","explore","set_home","go_home","teleport_to_player","teleport_to_spawn","report_location",
        "wave","bow","salute","dance","emote","pose","gesture","point","call","whisper",
        "set_mood","focus","relax","alert","calm","celebrate","mourn","agree","disagree","offer_help",
        "decline","accept","invite","welcome","bid_farewell","introduce","explain_self","state_name","state_role","state_model",
        "check_inventory","check_health","check_time","check_weather","check_biome","check_light","check_mobs",
        "select_tool","select_food","select_weapon","select_block","select_item",
        "begin_task","end_task","pause_task","resume_task","cancel_task","retry_task","skip_step","repeat_step","mark_done","mark_failed"
    };
    private static final String[] MINI_TOPICS = {
        "player","world","weather","time","biome","mob","block","item","tool","weapon",
        "armor","food","ore","tree","cave","home","spawn","task","goal","plan",
        "memory","skill","model","self","team","friend","enemy","danger","safety","progress"
    };
    private static final String[] GPT1_VERBS = {
        "gather","collect","harvest","mine","chop","dig","quarry","excavate","farm","fish",
        "hunt","shear","milk","pick","forage","scavenge","salvage","loot","find","obtain",
        "craft","make","build","assemble","forge","smelt","brew","cook","bake","repair",
        "place","remove","decorate","terraform","light","secure","expand","shrink","relocate",
        "fight","attack","defend","ambush","retreat","block","dodge","counter","heal",
        "plant","sow","water","fertilize","breed","feed","tame","grow","prune","replant",
        "explore","locate","scout","map","navigate","search","survey","discover","travel","venture",
        "store","sort","organize","deposit","withdraw","stack","transfer","trade","stock","manage",
        "use_tool","use_weapon","use_item","use_block","consume","activate","deactivate","toggle","configure","prepare"
    };
    private static final String[] GPT1_OBJECTS = {
        "wood","stone","coal","iron","copper","gold","diamond","emerald","redstone","lapis",
        "obsidian","dirt","sand","gravel","clay","wool","leather","meat","fish","crop",
        "flower","mushroom","berry","honey","sapling","seed","log","plank","stick","cobblestone",
        "tool","weapon","armor","food","block","item","potion","torch","ladder","bed",
        "crafting_table","furnace","chest","door","fence","wall","bridge","shelter","house","room",
        "hostile_mob","passive_mob","neutral_mob","boss","prey","predator","threat","target","enemy","intruder",
        "wheat","carrot","potato","beetroot","melon","pumpkin","cocoa","cane","cactus","bamboo",
        "cow","pig","chicken","sheep","rabbit","horse","wolf","cat","villager","trader",
        "cave","village","forest","mountain","river","ocean","desert","jungle","plains","hill"
    };
    private static final String[] GPT2_VERBS = {
        "conquer","master","complete","achieve","unlock","traverse","dominate","overcome","survive","thrive",
        "optimize","automate","orchestrate","engineer","architect","design","strategize","coordinate","delegate","execute",
        "raid","siege","storm","invade","defend","fortify","guard","escort","protect","patrol",
        "infiltrate","escape","evade","lure","trap","ambush","flank","surround","suppress","secure",
        "summon","banish","channel","invoke","enchant","imbue","transmute","conjure","ward","bless",
        "subjugate","annihilate","eradicate","pacify","stabilize","mobilize","neutralize","synthesize","weaponize","industrialize",
        "reconstruct","repurpose","replicate","reverse","reinforce","regenerate","rejuvenate","resurrect","recruit","retain",
        "pioneer","chart","colonize","establish","install","commission","operate","regulate","monetize","capitalize"
    };
    private static final String[] GPT2_OBJECTS = {
        "nether","end","stronghold","fortress","bastion","end_city","dragon","wither","warden","ancient_city",
        "dimensional_gateway","outer_islands","end_portal","nether_portal","blaze_spawner","mob_grinder","iron_farm","trading_hall","villager_breeder","crop_complex",
        "autonomous_mine","self_repairing_base","redstone_computer","storage_network","crafting_pipeline","enchanting_station","brewing_pipeline","smelting_array","defense_system","warning_network",
        "expedition","campaign","quest_line","world_tour","mapping_project","archaeological_dig","underwater_ruin","desert_temple","jungle_temple","ocean_monument",
        "diamond_layer","ancient_debris","netherite_forge","elytra_course","shulker_army","beacon_pyramid","conduit_temple","respawn_anchor","hoglin_stables","strider_ranch",
        "economy","market","guild","kingdom","federation","alliance","empire","colony","outpost","capital",
        "army","navy","air_force","special_forces","militia","mercenaries","champions","heroes","legends","avatars",
        "elemental","spirit","golem","construct","automation","drone","agent","swarm","hive","collective",
        "advancement_tree","trophy_hall","museum","library","archive","monument","memorial","wonder","masterpiece","legendary_item"
    };

    public SkillRegistry(SmithAIPlugin plugin) {
        this.plugin = plugin;
    }

    private synchronized void ensureGenerated() {
        if (generated) return;
        generateDefaults();
        loadFromDisk();
        generated = true;
    }

    private void generateDefaults() {
        Set<String> used = new HashSet<>();
        int mini = 0, gpt1 = 0, gpt2 = 0;
        for (String v : MINI_VERBS) if (used.add(v)) { register(v, "primitive", "mini"); mini++; }
        for (String v : MINI_VERBS) for (String t : MINI_TOPICS) {
            if (mini >= 900) break;
            String id = v + "_" + t; if (used.add(id)) { register(id, "primitive", "mini"); mini++; }
        }
        for (String v : GPT1_VERBS) for (String o : GPT1_OBJECTS) {
            if (gpt1 >= 1800) break;
            String id = v + "_" + o; if (used.add(id)) { register(id, "composite", "gpt1"); gpt1++; }
        }
        for (String v : GPT2_VERBS) for (String o : GPT2_OBJECTS) {
            if (gpt2 >= 6300) break;
            String id = v + "_" + o; if (used.add(id)) { register(id, "composite", "gpt2"); gpt2++; }
        }
        int i = 1; while (mini < 900) { String id = "mini_action_" + i++; if (used.add(id)) { register(id, "primitive", "mini"); mini++; } }
        i = 1; while (gpt1 < 1800) { String id = "gpt1_task_" + i++; if (used.add(id)) { register(id, "composite", "gpt1"); gpt1++; } }
        i = 1; while (gpt2 < 6300) { String id = "gpt2_objective_" + i++; if (used.add(id)) { register(id, "composite", "gpt2"); gpt2++; } }
    }

    private void loadFromDisk() {
        File file = new File(plugin.getDataFolder(), "skills.yml");
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            String description = yaml.getString(key + ".description", "");
            String type = yaml.getString(key + ".type", "composite");
            String tier = yaml.getString(key + ".tier", "gpt2");
            register(key, description, type, tier);
        }
    }

    private void register(String name, String type, String tier) {
        skills.put(name.toLowerCase(), new SkillDefinition(name, humanize(name), type, tier));
    }

    private void register(String name, String description, String type, String tier) {
        skills.put(name.toLowerCase(), new SkillDefinition(name, description, type, tier));
    }

    private static String humanize(String id) {
        String[] words = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.length() > 1 ? w.substring(1).toLowerCase() : "").append(' ');
        }
        return sb.toString().trim();
    }

    public SkillDefinition getSkill(String name) { ensureGenerated(); return skills.get(name.toLowerCase()); }
    public List<String> getSkillNames() { ensureGenerated(); return new ArrayList<>(skills.keySet()); }

    public List<String> getSkillNamesForTier(String tier) {
        ensureGenerated();
        int target = TIER_ORDER.getOrDefault(tier.toLowerCase(), 3);
        List<String> result = new ArrayList<>();
        for (SkillDefinition skill : skills.values()) {
            if (TIER_ORDER.getOrDefault(skill.getTier().toLowerCase(), 3) <= target) result.add(skill.getName());
        }
        return result;
    }

    public List<String> getMiniSkills() { return getSkillNamesForTier("mini"); }
    public List<String> getGpt1Skills() { return getSkillNamesForTier("gpt1"); }
    public List<String> getGpt2Skills() { return getSkillNamesForTier("gpt2"); }

    public String getTierForModel(String modelName) {
        String lower = modelName.toLowerCase();
        if (lower.contains("mini")) return "mini";
        if (lower.contains("2.0")) return "gpt2";
        if (lower.contains("1.0") || lower.contains("gpt")) return "gpt1";
        return "gpt2";
    }

    public List<String> getSkillsForActiveModel(String modelName) { return getSkillNamesForTier(getTierForModel(modelName)); }
    public int count() { ensureGenerated(); return skills.size(); }

    public int countByTier(String tier) {
        ensureGenerated();
        int target = TIER_ORDER.getOrDefault(tier.toLowerCase(), 3);
        int count = 0;
        for (SkillDefinition skill : skills.values()) if (TIER_ORDER.getOrDefault(skill.getTier().toLowerCase(), 3) <= target) count++;
        return count;
    }

    public static class SkillDefinition {
        private final String name, description, type, tier;
        public SkillDefinition(String name, String description, String type, String tier) {
            this.name = name; this.description = description; this.type = type; this.tier = tier;
        }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getType() { return type; }
        public String getTier() { return tier; }
    }
}
