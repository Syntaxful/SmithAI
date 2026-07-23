package com.smithai.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigTest {

    @Test
    public void testDefaults() {
        YamlConfiguration yaml = new YamlConfiguration();
        Config config = new Config(yaml);

        assertEquals("Smith_AI", config.getAiName());
        assertEquals("robot", config.getAiSkin());
        assertEquals(17, config.getMaxMemoryMessages());
        assertEquals(50, config.getMaxSkillQueueSize());
        assertEquals(5, config.getSkillStepDelay());
        assertEquals(3.0, config.getFollowDistance());
        assertFalse(config.isExternalEnabled());
        assertEquals("http://localhost:8000", config.getExternalUrl());
        assertEquals(128.0, config.getPathfindingMaxDistance());
        assertEquals(5000, config.getPathfindingMaxNodes());
        assertEquals(5, config.getPathfindingTickRate());
        assertEquals(0.3, config.getCombatRetreatHealth());
        assertEquals(6.0, config.getCombatMinFood());
        assertTrue(config.isCraftingPreferCraftingTable());
        assertEquals(900, config.getMiniSkillTier());
        assertEquals(1800, config.getGpt1SkillTier());
        assertEquals(6300, config.getGpt2SkillTier());
    }

    @Test
    public void testCustomValues() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("ai.name", "Smithy");
        yaml.set("ai.memory.maxMessages", 25);
        yaml.set("skills.maxQueueSize", 100);
        yaml.set("ai.pathfinding.maxDistance", 256.0);
        yaml.set("ai.models.mini.skillTier", 1000);
        Config config = new Config(yaml);

        assertEquals("Smithy", config.getAiName());
        assertEquals(25, config.getMaxMemoryMessages());
        assertEquals(100, config.getMaxSkillQueueSize());
        assertEquals(256.0, config.getPathfindingMaxDistance());
        assertEquals(1000, config.getMiniSkillTier());
    }

    @Test
    public void testExternalApiKeyStored() {
        YamlConfiguration yaml = new YamlConfiguration();
        Config config = new Config(yaml);
        config.setExternalApiKey("SMA-testkey");
        assertEquals("SMA-testkey", config.getExternalApiKey());
        assertEquals("SMA-testkey", yaml.getString("ai.external.apiKey"));
    }
}
