package com.smithai.knowledge;

import com.smithai.SmithAIPlugin;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class KnowledgeBaseTest {

    private SmithAIPlugin mockPlugin() {
        SmithAIPlugin plugin = mock(SmithAIPlugin.class);
        String blocks = "[{\"id\":\"minecraft:stone\",\"category\":\"block\",\"name\":\"Stone\",\"description\":\"Solid block\",\"tags\":[\"block\",\"mining\"]}]";
        String mobs = "[{\"id\":\"minecraft:zombie\",\"category\":\"mob\",\"name\":\"Zombie\",\"description\":\"Hostile mob\",\"tags\":[\"mob\",\"hostile\"]}]";
        String items = "[{\"id\":\"minecraft:iron_pickaxe\",\"category\":\"item\",\"name\":\"Iron Pickaxe\",\"description\":\"Mines diamond ore\",\"tags\":[\"item\",\"tool\"]}]";
        String recipes = "[{\"id\":\"recipe:stick\",\"category\":\"recipe\",\"name\":\"Stick\",\"description\":\"Two planks\",\"tags\":[\"recipe\"]}]";
        String strategy = "[{\"id\":\"strategy:mining\",\"category\":\"strategy\",\"name\":\"Mining\",\"description\":\"Dig down carefully\",\"tags\":[\"strategy\"]}]";
        String biomes = "[{\"id\":\"minecraft:plains\",\"category\":\"biome\",\"name\":\"Plains\",\"description\":\"Flat grasslands\",\"tags\":[\"biome\"]}]";
        when(plugin.getResource("knowledge/blocks.json")).thenReturn(stream(blocks));
        when(plugin.getResource("knowledge/mobs.json")).thenReturn(stream(mobs));
        when(plugin.getResource("knowledge/items.json")).thenReturn(stream(items));
        when(plugin.getResource("knowledge/recipes.json")).thenReturn(stream(recipes));
        when(plugin.getResource("knowledge/strategy.json")).thenReturn(stream(strategy));
        when(plugin.getResource("knowledge/biomes.json")).thenReturn(stream(biomes));
        return plugin;
    }

    private InputStream stream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testLoadsKnowledgeEntries() {
        KnowledgeBase kb = new KnowledgeBase(mockPlugin());
        assertEquals(6, kb.size(), "Should load all six mocked knowledge entries");
    }

    @Test
    public void testFindRelevantByName() {
        KnowledgeBase kb = new KnowledgeBase(mockPlugin());
        assertFalse(kb.findRelevant("zombie").isEmpty(), "Should find zombie entry");
        assertTrue(kb.findRelevant("zombie").get(0).toLowerCase().contains("zombie"), "Result should contain zombie");
    }

    @Test
    public void testFindRelevantByTag() {
        KnowledgeBase kb = new KnowledgeBase(mockPlugin());
        assertFalse(kb.findRelevant("tool").isEmpty(), "Should find tool entry by tag");
    }

    @Test
    public void testFindRelevantLimitsToFive() {
        KnowledgeBase kb = new KnowledgeBase(mockPlugin());
        assertTrue(kb.findRelevant("a").size() <= 5, "Should limit results to 5");
    }

    @Test
    public void testKnowledgeVersionAndStats() {
        KnowledgeBase kb = new KnowledgeBase(mockPlugin());
        assertNotNull(kb.getVersion(), "Version should not be null");
        assertTrue(kb.getLoadedAt() > 0, "Loaded timestamp should be set");
        java.util.Map<String, Object> stats = kb.getStats();
        assertNotNull(stats.get("version"), "Stats should include version");
        assertEquals(6, stats.get("total_entries"), "Stats should show correct total entries");
    }

    @Test
    public void testGetCategoryCounts() {
        KnowledgeBase kb = new KnowledgeBase(mockPlugin());
        java.util.Map<String, Integer> counts = kb.getCategoryCounts();
        assertEquals(1, counts.get("block").intValue(), "Should have 1 block entry");
        assertEquals(1, counts.get("mob").intValue(), "Should have 1 mob entry");
    }
}
