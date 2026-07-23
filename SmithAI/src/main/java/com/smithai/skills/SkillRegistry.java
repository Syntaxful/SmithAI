package com.smithai.skills;

import com.smithai.SmithAIPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SkillRegistry {

    private final SmithAIPlugin plugin;
    private final Map<String, SkillDefinition> skills = new HashMap<>();

    private static final Map<String, Integer> TIER_ORDER = new HashMap<>();
    static {
        TIER_ORDER.put("mini", 1);
        TIER_ORDER.put("gpt1", 2);
        TIER_ORDER.put("gpt2", 3);
    }

    public SkillRegistry(SmithAIPlugin plugin) {
        this.plugin = plugin;
        loadDefaults();
        loadFromDisk();
    }

    private void loadDefaults() {
        // Defaults are now loaded from skills.yml resource, so this is a fallback.
        register("noop", "Do nothing", "primitive", "mini");
    }

    private void loadFromDisk() {
        File file = new File(plugin.getDataFolder(), "skills.yml");
        if (!file.exists()) {
            SkillGenerator.generateIfMissing(file);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            String description = yaml.getString(key + ".description", "");
            String type = yaml.getString(key + ".type", "composite");
            String tier = yaml.getString(key + ".tier", "gpt2");
            register(key, description, type, tier);
        }
    }

    private void register(String name, String description, String type, String tier) {
        skills.put(name.toLowerCase(), new SkillDefinition(name, description, type, tier));
    }

    public SkillDefinition getSkill(String name) {
        return skills.get(name.toLowerCase());
    }

    public List<String> getSkillNames() {
        return new ArrayList<>(skills.keySet());
    }

    public List<String> getSkillNamesForTier(String tier) {
        int target = TIER_ORDER.getOrDefault(tier.toLowerCase(), 3);
        List<String> result = new ArrayList<>();
        for (SkillDefinition skill : skills.values()) {
            if (TIER_ORDER.getOrDefault(skill.getTier().toLowerCase(), 3) <= target) {
                result.add(skill.getName());
            }
        }
        return result;
    }

    public List<String> getMiniSkills() {
        return getSkillNamesForTier("mini");
    }

    public List<String> getGpt1Skills() {
        return getSkillNamesForTier("gpt1");
    }

    public List<String> getGpt2Skills() {
        return getSkillNamesForTier("gpt2");
    }

    public String getTierForModel(String modelName) {
        String lower = modelName.toLowerCase();
        if (lower.contains("mini")) return "mini";
        if (lower.contains("2.0") || lower.contains("15")) return "gpt2";
        if (lower.contains("1.0") || lower.contains("7.5") || lower.contains("gpt")) return "gpt1";
        return "gpt2";
    }

    public List<String> getSkillsForActiveModel(String modelName) {
        return getSkillNamesForTier(getTierForModel(modelName));
    }

    public int count() {
        return skills.size();
    }

    public int countByTier(String tier) {
        int target = TIER_ORDER.getOrDefault(tier.toLowerCase(), 3);
        int count = 0;
        for (SkillDefinition skill : skills.values()) {
            if (TIER_ORDER.getOrDefault(skill.getTier().toLowerCase(), 3) <= target) {
                count++;
            }
        }
        return count;
    }

    public static class SkillDefinition {
        private final String name;
        private final String description;
        private final String type;
        private final String tier;

        public SkillDefinition(String name, String description, String type, String tier) {
            this.name = name;
            this.description = description;
            this.type = type;
            this.tier = tier;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getType() { return type; }
        public String getTier() { return tier; }
    }
}
