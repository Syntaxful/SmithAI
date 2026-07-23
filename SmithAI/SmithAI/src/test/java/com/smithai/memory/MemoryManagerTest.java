package com.smithai.memory;

import com.smithai.SmithAIPlugin;
import com.smithai.config.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MemoryManagerTest {

    private SmithAIPlugin mockPlugin(File dataFolder, int maxMessages) {
        SmithAIPlugin plugin = mock(SmithAIPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        Config config = mock(Config.class);
        when(config.getMaxMemoryMessages()).thenReturn(maxMessages);
        when(plugin.getPluginConfig()).thenReturn(config);
        return plugin;
    }

    @Test
    public void testConversationCreatedForNpc(@TempDir File tempDir) {
        MemoryManager mm = new MemoryManager(mockPlugin(tempDir, 5));
        UUID id = UUID.randomUUID();
        Conversation conv = mm.getConversation(id);
        assertNotNull(conv, "Conversation should be created");
        assertTrue(conv.getMessages().isEmpty(), "New conversation should be empty");
    }

    @Test
    public void testConversationPersistsAndReloads(@TempDir File tempDir) {
        UUID id = UUID.randomUUID();
        MemoryManager mm1 = new MemoryManager(mockPlugin(tempDir, 5));
        Conversation conv1 = mm1.getConversation(id);
        conv1.addMessage("user", "hello");
        conv1.addMessage("assistant", "hi there");
        mm1.saveAll();

        MemoryManager mm2 = new MemoryManager(mockPlugin(tempDir, 5));
        Conversation conv2 = mm2.getConversation(id);
        assertEquals(2, conv2.getMessages().size(), "Reloaded conversation should have 2 messages");
    }

    @Test
    public void testMemorySearchByTopic(@TempDir File tempDir) {
        MemoryManager mm = new MemoryManager(mockPlugin(tempDir, 20));
        UUID id = UUID.randomUUID();
        Conversation conv = mm.getConversation(id);
        conv.addMessage("user", "let's go mining for diamonds");
        conv.addMessage("assistant", "I'll get my pickaxe");
        conv.addMessage("user", "build a house near the village");
        mm.saveAll();

        String query = "mining";
        java.util.List<String> results = mm.searchByTopic(query);
        assertFalse(results.isEmpty(), "Should find messages about mining");
        boolean found = results.stream().anyMatch(r -> r.contains("diamonds"));
        assertTrue(found, "Should find the diamond mining message");
    }

    @Test
    public void testGetTopics(@TempDir File tempDir) {
        MemoryManager mm = new MemoryManager(mockPlugin(tempDir, 20));
        UUID id = UUID.randomUUID();
        Conversation conv = mm.getConversation(id);
        conv.addMessage("user", "let's go mine some diamonds");
        conv.addMessage("user", "craft me a pickaxe please");
        mm.saveAll();

        java.util.List<String> topics = mm.getTopics();
        assertTrue(topics.contains("mining"), "Should detect mining topic");
        assertTrue(topics.contains("crafting"), "Should detect crafting topic");
    }

    @Test
    public void testConversationMaxMessages(@TempDir File tempDir) {
        MemoryManager mm = new MemoryManager(mockPlugin(tempDir, 3));
        UUID id = UUID.randomUUID();
        Conversation conv = mm.getConversation(id);
        conv.addMessage("user", "a");
        conv.addMessage("user", "b");
        conv.addMessage("user", "c");
        conv.addMessage("user", "d");
        assertEquals(3, conv.getMessages().size(), "Conversation should respect max messages limit");
        assertEquals("b", conv.getMessages().get(0).getContent(), "Oldest message should be dropped");
    }
}
